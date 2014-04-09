package com.ibm.wala.cast.ipa.callgraph;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.IteratorPlusOne;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSet;

public final class LexicalScopingResolverContexts implements ContextSelector {

  public static final ContextKey RESOLVER = new ContextKey() {
    public final String toString() {
      return "Resolver Key";
    }
  };

  private static final boolean USE_CGNODE_RESOLVER = true;

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
    Set<LocalPointerKey> getReadOnlyValues(Pair<String, String> name);

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

    Map<String, LocalPointerKey> readOnlyNames = HashMapFactory.make();
    Set<Pair<String, String>> readWriteNames = HashSetFactory.make();

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
              readWriteNames.add(exposedNames[i]);
            }
          }
        }
      }

      if (USE_CGNODE_RESOLVER) {
        CGNodeResolver result = (CGNodeResolver) parent.children().get(caller);
        if (result == null) {
          result = new CGNodeResolver(parent, caller);
          parent.children().put(caller, result);
        }
        for (String readOnlyName : readOnlyNames.keySet()) {
          result.addReadOnlyName(readOnlyName, readOnlyNames.get(readOnlyName));
        }
        for (Pair<String, String> readWriteName : readWriteNames) {
          result.addReadWriteName(readWriteName, callSite);
        }
        return result;
      } else {
        Object key;
        if (!readWriteNames.isEmpty()) {
          key = Pair.make(caller, callSite);
        } else {
          key = readOnlyNames.keySet();
        }

        if (parent.children().containsKey(key)) {
          return parent.children().get(key);
        }

        if (!readWriteNames.isEmpty()) {
          SiteResolver result = new SiteResolver(parent, caller, readOnlyNames, callSite, readWriteNames);
          parent.children().put(key, result);
          return result;
        } else {
          ReadOnlyResolver result = new ReadOnlyResolver(parent, caller, readOnlyNames);
          parent.children().put(key, result);
          return result;
        }
      }
    }

    return parent;
  }

  LexicalScopingResolver globalResolver = new LexicalScopingResolver() {

    public boolean isReadOnly(Pair<String, String> name) {
      return false;
    }

    public Set<LocalPointerKey> getReadOnlyValues(Pair<String, String> name) {
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
        children = HashMapFactory.make();
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

    public String toString() {
      return "GLOBAL_RESOLVER";
    }
  };

  /**
   * single {@link LexicalScopingResolver} for a CGNode, handling read-only and
   * read-write names
   */
  class CGNodeResolver implements LexicalScopingResolver {

    private final LexicalScopingResolver parent;
    private Map<Object, LexicalScopingResolver> children;
    /**
     * definer name for corresponding scope
     */
    private final String myDefiner;
    private final CGNode myNode;

    /**
     * maps a read-only name defined in this scope to the local pointer keys by
     * which it is referenced at call sites encountered thus far
     */
    private Map<String, Set<LocalPointerKey>> myReadOnlyDefs = null;

    /**
     * maps a name defined in this scope that may be defined in a nested scope
     * to the set of call sites at which it is exposed for nested writes
     */
    private Map<Pair<String, String>, Set<Pair<CallSiteReference, CGNode>>> myDefs = null;

    public CGNodeResolver(LexicalScopingResolver parent, CGNode myNode) {
      super();
      this.parent = parent;
      this.myDefiner = ((AstMethod) myNode.getMethod()).lexicalInfo().getScopingName();
      this.myNode = myNode;
    }

    public void addReadWriteName(Pair<String, String> readWriteName, CallSiteReference callSite) {
      if (myDefs == null) {
        myDefs = HashMapFactory.make();
      }
      MapUtil.findOrCreateSet(myDefs, readWriteName).add(Pair.make(callSite, myNode));
    }

    public void addReadOnlyName(String readOnlyName, LocalPointerKey localPointerKey) {
      if (myReadOnlyDefs == null) {
        myReadOnlyDefs = HashMapFactory.make();
      }
      MapUtil.findOrCreateSet(myReadOnlyDefs, readOnlyName).add(localPointerKey);
    }

    public LexicalScopingResolver getParent() {
      return parent;
    }

    public boolean isReadOnly(Pair<String, String> name) {
      if (myDefiner.equals(name.fst)) {
        return myReadOnlyDefs != null && myReadOnlyDefs.containsKey(name.snd);
      } else {
        return parent.isReadOnly(name);
      }
    }

    public Set<LocalPointerKey> getReadOnlyValues(Pair<String, String> name) {
      if (myDefiner.equals(name.fst)) {
        return myReadOnlyDefs.get(name.snd);
      } else {
        return parent.getReadOnlyValues(name);
      }
    }

    public Iterator<Pair<CallSiteReference, CGNode>> getLexicalSites(Pair<String, String> name) {
      if (myDefs == null || myDefs.containsKey(name)) {
        if (myDefiner.equals(name.snd)) {
          // no need to recurse to parent
          return myDefs.get(name).iterator();
        } else {
          return new CompoundIterator<Pair<CallSiteReference, CGNode>>(parent.getLexicalSites(name), myDefs.get(name).iterator());
        }
      } else {
        return parent.getLexicalSites(name);
      }
    }

    public Map<Object, LexicalScopingResolver> children() {
      if (children == null) {
        children = HashMapFactory.make();
      }
      return children;
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
    // StringBuilder result = new StringBuilder();
    // result.append("CGNodeResolver[myDefiner=");
    // result.append(myDefiner);
    // result.append(",\n parent=");
    // result.append(parent);
    // result.append("]");
    // return result.toString();
    // }

  }

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
    final protected Map<String, LocalPointerKey> myReadOnlyDefs;
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

    public Set<LocalPointerKey> getReadOnlyValues(Pair<String, String> name) {
      if (myDefiner.equals(name.fst)) {
        return Collections.singleton(myReadOnlyDefs.get(name.snd));
      } else {
        return parent.getReadOnlyValues(name);
      }
    }

    public Map<Object, LexicalScopingResolver> children() {
      if (children == null) {
        children = HashMapFactory.make();
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

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("ReadOnlyResolver[myDefiner=");
      result.append(myDefiner);
      // result.append(", myNode=");
      // result.append(myNode);
      result.append(",\n myReadOnlyDefs=");
      result.append(myReadOnlyDefs);
      result.append(",\n parent=");
      result.append(parent);
      result.append("]");
      return result.toString();
    }

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
      StringBuilder result = new StringBuilder();
      result.append("SiteResolver[myDefiner=");
      result.append(myDefiner);
      // result.append(", myNode=");
      // result.append(myNode);
      result.append(",\n mySite=");
      result.append(mySite);
      result.append(",\n myReadOnlyDefs=");
      result.append(myReadOnlyDefs);
      result.append(",\n myDefs=");
      result.append(myDefs);
      result.append(",\n parent=");
      result.append(parent);
      result.append("]");
      return result.toString();
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

  private static class RecursionKey {
    private final IMethod caller;
    private final CallSiteReference site;
    private final IMethod target;

    public RecursionKey(IMethod caller, CallSiteReference site, IMethod target) {
      super();
      this.caller = caller;
      this.site = site;
      this.target = target;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + caller.hashCode();
      result = prime * result + site.hashCode();
      result = prime * result + target.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      RecursionKey other = (RecursionKey) obj;
      if (!caller.equals(other.caller))
        return false;
      if (!site.equals(other.site))
        return false;
      if (!target.equals(other.target))
        return false;
      return true;
    }

  }

  /**
   * cache for recursion checks
   */
  private final Map<RecursionKey, List<LexicalScopingResolverContext>> key2Contexts = HashMapFactory.make();

  private Context checkForRecursion(RecursionKey key, LexicalScopingResolver srcResolver) {
    List<LexicalScopingResolverContext> calleeContexts = key2Contexts.get(key);
    if (calleeContexts != null) {
      // globalResolver better be at the top of any parent chain
      while (srcResolver != globalResolver) {
        for (LexicalScopingResolverContext c : calleeContexts) {
          if (c.governingCallSites == srcResolver) {
            return c;
          }
        }
        srcResolver = srcResolver.getParent();
      }
    }
    return null;
  }

  public static boolean hasExposedUses(CGNode caller, CallSiteReference site) {
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
    if (callee instanceof SummarizedMethod) {
      final String calleeName = callee.getReference().toString();
      // TODO create a sub-class in the cast.js projects so we're not checking strings here
      if (calleeName.equals("< JavaScriptLoader, LArray, ctor()LRoot; >") || calleeName.equals("< JavaScriptLoader, LObject, ctor()LRoot; >")) {
        return baseContext;
      }
    }
    LexicalScopingResolver resolver = (LexicalScopingResolver) caller.getContext().get(RESOLVER);

    final RecursionKey key = new RecursionKey(caller.getMethod(), site, callee);
    if (resolver != null) {
      Context recursiveParent = checkForRecursion(key, resolver);
      if (recursiveParent != null) {
        return recursiveParent;
      }
    }

    if (caller.getMethod() instanceof AstMethod && hasExposedUses(caller, site)) {
      LexicalScopingResolverContext result = new LexicalScopingResolverContext(caller, site, baseContext);
      MapUtil.findOrCreateList(key2Contexts, key).add(result);
      return result;
    }

    else if (resolver != null) {
      LexicalScopingResolverContext result = new LexicalScopingResolverContext(resolver, baseContext);
      MapUtil.findOrCreateList(key2Contexts, key).add(result);
      return result;
    }

    else {
      return baseContext;
    }
  }

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }
}