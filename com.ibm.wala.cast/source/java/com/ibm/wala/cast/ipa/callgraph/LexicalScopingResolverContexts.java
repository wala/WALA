package com.ibm.wala.cast.ipa.callgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.IteratorPlusOne;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSet;

public final class LexicalScopingResolverContexts implements ContextSelector {

  public static final ContextKey RESOLVER = new ContextKey() {
    public final String toString() {
      return "Resolver Key";
    }
  };

  /**
   * used to resolve lexical accesses during call graph construction
   */
  interface LexicalScopingResolver extends ContextItem {

    /**
     * return resolver for parent lexical scope
     */
    LexicalScopingResolver getParent();

    /**
     * return true if name may only be read in nested lexical scopes, otherwise
     * false
     */
    boolean isReadOnly(Pair<String, String> name);

    /**
     * if {@link #isReadOnly(Pair)} returns true for name, get the
     * {@link LocalPointerKey} corresponding to name from the {@link CGNode}
     * that defines it
     */
    LocalPointerKey getReadOnlyValue(Pair<String, String> name);

    /**
     * get the site-node pairs (s,n) in the scope-resolver chain such that n has
     * a definition of the name and s is the call site in n possibly exposing
     * the name to an invoked nested function
     */
    Iterator<Pair<CallSiteReference, CGNode>> getLexicalSites(Pair<String, String> name);

    /**
     * resolvers for child lexical scopes. updates are performed via mutations
     * of the returned map.
     */
    Map<Object, LexicalScopingResolver> children();

    /**
     * get the CGNode in the scope-resolver chain that defines name, or
     * <code>null</code> if so such node exists (happens when accesses occurs
     * after exit of defining function)
     */
    CGNode getOriginalDefiner(Pair<String, String> name);
  }

  /**
   * find or create an appropriate {@link LexicalScopingResolver} for the caller
   * and callSite. Note that {@link #hasExposedUses(CGNode, CallSiteReference)}
   * must be true for caller and callSite. We try to re-use a previously-created
   * context whenever possible.
   */
  private LexicalScopingResolver findChild(CGNode caller, CallSiteReference callSite) {
    LexicalScopingResolver parent = (LexicalScopingResolver) caller.getContext().get(RESOLVER);
    if (parent == null) {
      parent = globalResolver;
    }

    Map<String, LocalPointerKey> readOnlyNames = new HashMap<String, LocalPointerKey>();
    Set<Pair<String, String>> readWritesNames = new HashSet<Pair<String, String>>();

    LexicalInformation LI = ((AstMethod) caller.getMethod()).lexicalInfo();
    int[] exposedUses = LI.getExposedUses(callSite.getProgramCounter());
    if (exposedUses.length > 0) {
      Pair<String, String> exposedNames[] = LI.getExposedNames();
      for (int i = 0; i < exposedUses.length; i++) {
        if (exposedUses[i] != -1) {
          if (!parent.isReadOnly(exposedNames[i])) {
            if (LI.isReadOnly(exposedNames[i].snd)) {
              readOnlyNames.put(exposedNames[i].snd, new LocalPointerKey(caller, exposedUses[i]));
            } else {
              readWritesNames.add(exposedNames[i]);
            }
          }
        }
      }

      Object key;
      if (!readWritesNames.isEmpty()) {
        key = Pair.make(caller, callSite);
      } else {
        key = readOnlyNames.keySet();
      }

      if (parent.children().containsKey(key)) {
        return parent.children().get(key);
      }

      if (!readWritesNames.isEmpty()) {
        SiteResolver result = new SiteResolver(parent, caller, readOnlyNames, callSite, readWritesNames);
        parent.children().put(key, result);
        return result;
      } else {
        ReadOnlyResolver result = new ReadOnlyResolver(parent, caller, readOnlyNames);
        parent.children().put(key, result);
        return result;
      }
    }

    return parent;
  }

  LexicalScopingResolver globalResolver = new LexicalScopingResolver() {

    public boolean isReadOnly(Pair<String, String> name) {
      return false;
    }

    public LocalPointerKey getReadOnlyValue(Pair<String, String> name) {
      throw new UnsupportedOperationException("not expecting read only global");
    }

    public Iterator<Pair<CallSiteReference, CGNode>> getLexicalSites(Pair<String, String> name) {
      if (name.snd == null) {
        return new NonNullSingletonIterator<Pair<CallSiteReference, CGNode>>(Pair.make((CallSiteReference) null, builder
            .getCallGraph().getFakeRootNode()));
      } else {
        return EmptyIterator.instance();
      }
    }

    private Map<Object, LexicalScopingResolver> children;

    public Map<Object, LexicalScopingResolver> children() {
      if (children == null) {
        children = new HashMap<Object, LexicalScopingResolver>();
      }
      return children;
    }

    public LexicalScopingResolver getParent() {
      return null;
    }

    public CGNode getOriginalDefiner(Pair<String, String> name) {
      if (name.snd == null) {
        return builder.getCallGraph().getFakeRootNode();
      } else {
        return null;
      }
    }
  };

  /**
   * {@link LexicalScopingResolver} for case where all exposed names from the
   * corresponding scope are read-only
   */
  class ReadOnlyResolver implements LexicalScopingResolver {
    final protected LexicalScopingResolver parent;
    protected Map<Object, LexicalScopingResolver> children;
    /**
     * definer name for corresponding scope
     */
    final protected String myDefiner;
    final private Map<String, LocalPointerKey> myReadOnlyDefs;
    final protected CGNode myNode;

    private ReadOnlyResolver(LexicalScopingResolver parent, CGNode caller, Map<String, LocalPointerKey> readOnlyDefs) {
      this.myDefiner = ((AstMethod) caller.getMethod()).lexicalInfo().getScopingName();
      this.parent = parent;
      this.myReadOnlyDefs = readOnlyDefs;
      this.myNode = caller;
    }

    public boolean isReadOnly(Pair<String, String> name) {
      if (myDefiner.equals(name.fst)) {
        return myReadOnlyDefs.containsKey(name.snd);
      } else {
        return parent.isReadOnly(name);
      }
    }

    public LocalPointerKey getReadOnlyValue(Pair<String, String> name) {
      if (myDefiner.equals(name.fst)) {
        return myReadOnlyDefs.get(name.snd);
      } else {
        return parent.getReadOnlyValue(name);
      }
    }

    public Map<Object, LexicalScopingResolver> children() {
      if (children == null) {
        children = new HashMap<Object, LexicalScopingResolver>();
      }
      return children;
    }

    public Iterator<Pair<CallSiteReference, CGNode>> getLexicalSites(Pair<String, String> name) {
      return parent.getLexicalSites(name);
    }

    public LexicalScopingResolver getParent() {
      return parent;
    }

    public CGNode getOriginalDefiner(Pair<String, String> name) {
      if (myDefiner.equals(name.snd)) {
        return myNode;
      } else {
        return parent.getOriginalDefiner(name);
      }
    }

    // @Override
    // public String toString() {
    // return "ReadOnlyResolver [parent=" + parent + ", myDefiner=" + myDefiner
    // + ", myReadOnlyDefs="
    // + myReadOnlyDefs + ", myNode=" + myNode + "]";
    // }

  }

  /**
   * {@link LexicalScopingResolver} handling case where some exposed names may
   * be written in enclosed scopes
   */
  class SiteResolver extends ReadOnlyResolver implements LexicalScopingResolver {
    /**
     * names defined in the corresponding scope that may be written in a nested
     * scope
     */
    private final Set<Pair<String, String>> myDefs;
    private final CallSiteReference mySite;

    private SiteResolver(LexicalScopingResolver parent, CGNode caller, Map<String, LocalPointerKey> readOnlyDefs,
        CallSiteReference site, Set<Pair<String, String>> defs) {
      super(parent, caller, readOnlyDefs);
      this.mySite = site;
      this.myDefs = defs;
    }

    public Iterator<Pair<CallSiteReference, CGNode>> getLexicalSites(Pair<String, String> name) {
      if (myDefs.contains(name)) {
        if (myDefiner.equals(name.snd)) {
          return new NonNullSingletonIterator<Pair<CallSiteReference, CGNode>>(Pair.make(mySite, myNode));
        } else {
          return IteratorPlusOne.make(parent.getLexicalSites(name), Pair.make(mySite, myNode));
        }
      } else {
        return parent.getLexicalSites(name);
      }
    }

    @Override
    public String toString() {
      return "SiteResolver [myDefs=" + myDefs + ", mySite=" + mySite + ", toString()=" + super.toString() + "]";
    }

  }

  private class LexicalScopingResolverContext implements Context {
    private final LexicalScopingResolver governingCallSites;
    private final Context base;

    public int hashCode() {
      return base.hashCode() * (governingCallSites == null ? 1077651 : governingCallSites.hashCode());
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (getClass().equals(o.getClass())) {
        LexicalScopingResolverContext c = (LexicalScopingResolverContext) o;
        return (base == null ? c.base == null : base.equals(c.base)) && (governingCallSites == c.governingCallSites);
      } else {
        return false;
      }
    }

    public ContextItem get(ContextKey name) {
      return name.equals(RESOLVER) ? governingCallSites : base != null ? base.get(name) : null;
    }

    private LexicalScopingResolverContext(LexicalScopingResolver governingCallSites, Context base) {
      this.base = base;
      this.governingCallSites = governingCallSites;
    }

    private LexicalScopingResolverContext(CGNode source, CallSiteReference callSite, Context base) {
      this.base = base;
      this.governingCallSites = findChild(source, callSite);
    }

    @Override
    public String toString() {
      return "LexicalScopingResolverContext [governingCallSites=" + governingCallSites + ", base=" + base + "]";
    }

  }

  private final ContextSelector base;

  private final PropagationCallGraphBuilder builder;

  public LexicalScopingResolverContexts(PropagationCallGraphBuilder builder, ContextSelector base) {
    this.base = base;
    this.builder = builder;
  }

  private Context checkForRecursion(IMethod target, LexicalScopingResolver srcResolver) {
    while (srcResolver != null) {
      for (CGNode n : builder.getCallGraph().getNodes(target.getReference())) {
        if (n.getContext().get(RESOLVER) == srcResolver) {
          return n.getContext();
        }
      }
      srcResolver = srcResolver.getParent();
    }
    return null;
  }

  private boolean hasExposedUses(CGNode caller, CallSiteReference site) {
    int uses[] = ((AstMethod) caller.getMethod()).lexicalInfo().getExposedUses(site.getProgramCounter());
    if (uses != null && uses.length > 0) {
      for (int use : uses) {
        if (use > 0) {
          return true;
        }
      }
    }

    return false;
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
    LexicalScopingResolver resolver = (LexicalScopingResolver) caller.getContext().get(RESOLVER);

    Context recursiveParent = checkForRecursion(callee, resolver);
    if (recursiveParent != null) {
      return recursiveParent;
    }

    if (caller.getMethod() instanceof AstMethod && hasExposedUses(caller, site)) {
      return new LexicalScopingResolverContext(caller, site, baseContext);
    }

    else if (resolver != null) {
      return new LexicalScopingResolverContext(resolver, baseContext);
    }

    else {
      return baseContext;
    }
  }

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }
}