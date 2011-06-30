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

  interface LexicalScopingResolver extends ContextItem {
  
    LexicalScopingResolver getParent();
    
    boolean isReadOnly(Pair<String,String> name);
    
    LocalPointerKey getReadOnlyValue(Pair<String,String> name);
    
    Iterator<Pair<CallSiteReference,CGNode>> getLexicalSites(Pair<String,String> name);
  
    Set<LexicalScopingResolver> children();
    
    CGNode getOriginalDefiner(Pair<String,String> name);
  }
  
  private LexicalScopingResolver findChild(CGNode caller, CallSiteReference callSite) {    
    LexicalScopingResolver parent = (LexicalScopingResolver) caller.getContext().get(RESOLVER);
    if (parent == null) {
      parent = globalResolver;
    }
    
    Map<String,LocalPointerKey> readOnlyNames = new HashMap<String,LocalPointerKey>();
    Set<Pair<String,String>> names = new HashSet<Pair<String,String>>(); 
    
    LexicalInformation LI = ((AstMethod)caller.getMethod()).lexicalInfo();
    int[] exposedUses =  LI.getExposedUses(callSite.getProgramCounter());
    if (exposedUses.length > 0) {
      Pair<String,String> exposedNames[] = LI.getExposedNames();
      for(int i = 0; i < exposedUses.length; i++) {
        if (exposedUses[i] != -1) {
          if (! parent.isReadOnly(exposedNames[i])) {
            if (LI.isReadOnly(exposedNames[i].snd)) {
              readOnlyNames.put(exposedNames[i].snd, new LocalPointerKey(caller, exposedUses[i]));
            } else {
              names.add(exposedNames[i]);
            }
          }
        }
      }
    
      for(LexicalScopingResolver c : parent.children()) {
        if (! names.isEmpty()) {
          if (c instanceof SiteResolver) {
            if (((SiteResolver)c).mySite.equals(callSite) && ((SiteResolver)c).myNode.equals(caller)) {
              return c;
            }
          }
        } else {
          if (c instanceof ReadOnlyResolver) {
            if (((ReadOnlyResolver)c).myReadOnlyDefs.keySet().containsAll(readOnlyNames.keySet())) {
              return c;
            }
          }
        }
      }
      
      if (! names.isEmpty()) {
        SiteResolver result = new SiteResolver(parent, caller, readOnlyNames, callSite, names);
        parent.children().add(result);
        return result;
      } else {
        ReadOnlyResolver result = new ReadOnlyResolver(parent, caller, readOnlyNames);
        parent.children().add(result);
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
        return new NonNullSingletonIterator<Pair<CallSiteReference, CGNode>>(Pair.make((CallSiteReference)null, builder.getCallGraph().getFakeRootNode()));
      } else {
        return EmptyIterator.instance();
      }
    }
    
    private Set<LexicalScopingResolver> children;
    
    public Set<LexicalScopingResolver> children() {
      if (children == null) { 
        children = new HashSet<LexicalScopingResolver>();
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
  
  class ReadOnlyResolver implements LexicalScopingResolver {
    final protected LexicalScopingResolver parent;
    protected Set<LexicalScopingResolver> children;
    final protected String myDefiner;
    final private Map<String,LocalPointerKey> myReadOnlyDefs;
    final protected CGNode myNode;
    
    private ReadOnlyResolver(LexicalScopingResolver parent, CGNode caller, Map<String,LocalPointerKey> readOnlyDefs) {
      this.myDefiner = ((AstMethod)caller.getMethod()).lexicalInfo().getScopingName();
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

    public Set<LexicalScopingResolver> children() {
      if (children == null) { 
        children = new HashSet<LexicalScopingResolver>();
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
 
  }
  
  class SiteResolver extends ReadOnlyResolver implements LexicalScopingResolver {
    private final Set<Pair<String,String>> myDefs;
    private final CallSiteReference mySite;
    
    private SiteResolver(LexicalScopingResolver parent, CGNode caller, Map<String,LocalPointerKey> readOnlyDefs, CallSiteReference site, Set<Pair<String,String>> defs) {
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
  }
  

  private class LexicalScopingResolverContext implements Context {
    private final LexicalScopingResolver governingCallSites;
    private final Context base;

    public int hashCode() {
      return base.hashCode() * (governingCallSites==null? 1077651: governingCallSites.hashCode());
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (getClass().equals(o.getClass())) {
        LexicalScopingResolverContext c = (LexicalScopingResolverContext)o;
        return (base==null? c.base==null: base.equals(c.base)) 
                                    &&
               (governingCallSites == c.governingCallSites);
      } else {
        return false;
      }
    }

    public ContextItem get(ContextKey name) {
      return name.equals(RESOLVER)? governingCallSites: base!=null? base.get(name): null;
    }

    private LexicalScopingResolverContext(LexicalScopingResolver governingCallSites, Context base) {
      this.base = base;
      this.governingCallSites = governingCallSites;
    }
    
    private LexicalScopingResolverContext(CGNode source, CallSiteReference callSite, Context base) {
      this.base = base;
      this.governingCallSites = findChild(source, callSite);      
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
      for(CGNode n : builder.getCallGraph().getNodes(target.getReference())) {
        if (n.getContext().get(RESOLVER) == srcResolver) {
          return n.getContext();
        }
      }
      srcResolver = srcResolver.getParent();
    }
    return null;
  }
  
  private boolean hasExposedUses(CGNode caller, CallSiteReference site) {
    int uses[] = ((AstMethod)caller.getMethod()).lexicalInfo().getExposedUses(site.getProgramCounter());
    if (uses != null && uses.length > 0) {
      for(int use : uses) {
        if (use > 0) {
          return true;
        }
      }
    }
    
    return false;
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
    LexicalScopingResolver resolver = (LexicalScopingResolver)caller.getContext().get(RESOLVER);
    
    Context recursiveParent = checkForRecursion(callee, resolver);
    if (recursiveParent != null) {
      return recursiveParent;
    }
    
    if (caller.getMethod() instanceof AstMethod 
                      &&
        hasExposedUses(caller, site)) 
    {
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