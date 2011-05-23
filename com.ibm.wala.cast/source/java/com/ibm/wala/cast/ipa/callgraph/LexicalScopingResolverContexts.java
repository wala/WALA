package com.ibm.wala.cast.ipa.callgraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
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
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.intset.IntSet;

public final class LexicalScopingResolverContexts implements ContextSelector {

  public static final ContextKey RESOLVER = new ContextKey() {
    public final String toString() {
      return "Resolver Key";
    }
  };

  public static class Resolver extends HashMap<Pair<String,String>, Object> implements ContextItem {
    private final Map<Pair<String,String>,CGNode> funargKeys = new HashMap<Pair<String,String>,CGNode>();
    private final Resolver parent;
    
    private Resolver(Resolver parent) {
      super(1);
      this.parent = parent;
    }

    private static Iterator<Pair<CallSiteReference,CGNode>> getLexicalSitesRec(final Object x) {
      if (x == null) {
        return null;
      } else if (x instanceof CGNode) {
        return new MapIterator<CallSiteReference,Pair<CallSiteReference,CGNode>>(
            ((CGNode)x).iterateCallSites(),
            new Function<CallSiteReference,Pair<CallSiteReference,CGNode>>() {
              public Pair<CallSiteReference, CGNode> apply(CallSiteReference object) {
                return Pair.make(object, (CGNode)x);
              }
            });

      } else if (x instanceof Pair) {
        return new NonNullSingletonIterator(x);

      } else {
        Iterator<Pair<CallSiteReference,CGNode>> result = EmptyIterator.instance();
        Iterator<?> c = ((Collection<?>) x).iterator();
        while(c.hasNext()) {
          result = new CompoundIterator<Pair<CallSiteReference,CGNode>>(result, getLexicalSitesRec(c.next()));
        }
        return result;
      } 
    }

    private void add(Pair<String,String> name, Pair<CallSiteReference,CGNode> site) {
      if (! containsKey(name)) {
        put(name, site);
      } else {
        Object x = get(name);
        if (! x.equals(site)) {
          if (x instanceof Collection) {
            ((Collection)x).add(site);
          } else {
            Collection<Object> s = new HashSet<Object>(2);
            s.add(x);
            s.add(site);
            put(name,s);
          }
        }
      }
    }

    public Iterator<Pair<CallSiteReference,CGNode>> getLexicalSites(Pair<String,String> p) {
      return getLexicalSitesRec(get(p));
    }

    public Iterator<Pair<CallSiteReference,CGNode>> getLexicalSites(Access a) {
      return getLexicalSites(Pair.make(a.variableName, a.variableDefiner));
    }
    
    public void addFunarg(Pair<String,String> var, CGNode target) {
      funargKeys.put(var, target);
    }
    
    public CGNode getFunarg(Pair<String,String> var) {
      return funargKeys.get(var);
    }
  }

  private class LexicalScopingResolverContext implements Context {
    private final Resolver governingCallSites;
    private final Context base;

    public int hashCode() {
      return governingCallSites==null? 1077651: governingCallSites.keySet().hashCode();
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (getClass().equals(o.getClass())) {
        LexicalScopingResolverContext c = (LexicalScopingResolverContext)o;
        return (base==null? c.base==null: base.equals(c.base)) 
                                    &&
               (governingCallSites.equals(c.governingCallSites));
      } else {
        return false;
      }
    }

    public ContextItem get(ContextKey name) {
      return name.equals(RESOLVER)? governingCallSites: base!=null? base.get(name): null;
    }

    private LexicalScopingResolverContext(Resolver governingCallSites, Context base) {
      this.base = base;
      this.governingCallSites = governingCallSites;
    }
    
    private LexicalScopingResolverContext(CGNode source, CallSiteReference callSite, Context base) {
      Context srcContext = source.getContext();
      Resolver srcResolver = (Resolver) srcContext.get(RESOLVER);        

      this.base = base;
      this.governingCallSites = new Resolver(srcResolver);
      
      if (source.getMethod() instanceof AstMethod) {
        LexicalInformation LI = ((AstMethod)source.getMethod()).lexicalInfo();
        int[] exposedUses =  LI.getExposedUses(callSite.getProgramCounter());
        int[] exposedExitUses =  LI.getExitExposedUses();
        if (exposedUses.length > 0) {
          Pair<String,String> exposedNames[] = LI.getExposedNames();
          for(int i = 0; i < exposedUses.length; i++) {
            if (exposedUses[i] != -1) {
              governingCallSites.add(exposedNames[i], Pair.make(callSite, source));
            }
            if (exposedExitUses[i] != -1) {
              governingCallSites.addFunarg(exposedNames[i], source);
            }
          }
        }
      }
 
      if (srcResolver != null) {
        for(Pair<String,String> x : srcResolver.keySet()) {
          Iterator<Pair<CallSiteReference,CGNode>> sites = srcResolver.getLexicalSites(x);
          while (sites.hasNext()) {
            governingCallSites.add(x, sites.next());
          }
        }
      }
    }
  }
  
  private final ContextSelector base;
    
  private final PropagationCallGraphBuilder builder;
  
  public LexicalScopingResolverContexts(PropagationCallGraphBuilder builder, ContextSelector base) {
    this.base = base;
    this.builder = builder;
  }

  private Context checkForRecursion(IMethod target, Resolver srcResolver) {
    while (srcResolver != null) {
      for(CGNode n : builder.getCallGraph().getNodes(target.getReference())) {
        if (n.getContext().get(RESOLVER) == srcResolver) {
          return n.getContext();
        }
      }
      srcResolver = srcResolver.parent;
    }
    return null;
  }
  
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
    Resolver resolver = (Resolver)caller.getContext().get(RESOLVER);
    
    Context recursiveParent = checkForRecursion(callee, resolver);
    if (recursiveParent != null) {
      return recursiveParent;
    }
    
    if (caller.getMethod() instanceof AstMethod 
                      &&
        ((AstMethod)caller.getMethod()).lexicalInfo().getExposedUses(site.getProgramCounter()).length > 0) 
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