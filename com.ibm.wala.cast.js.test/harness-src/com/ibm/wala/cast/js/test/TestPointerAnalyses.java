/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.GlobalVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.ObjectVertex;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.GlobalObjectKey;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptConstructorFunctions.JavaScriptConstructor;
import com.ibm.wala.cast.js.test.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;

public abstract class TestPointerAnalyses {

  private final class CheckPointers extends
      Predicate<Pair<Set<Pair<CGNode, NewSiteReference>>, Set<Pair<CGNode, NewSiteReference>>>> {
    private Set<Pair<String,Integer>> map(Set<Pair<CGNode, NewSiteReference>> sites) {
      Set<Pair<String,Integer>> result = HashSetFactory.make();
      for(Pair<CGNode,NewSiteReference> s : sites) {
        result.add(Pair.make(s.fst.getMethod().toString(), s.snd.getProgramCounter()));
      }
      return result;
    }

    @Override
    public boolean test(Pair<Set<Pair<CGNode, NewSiteReference>>, Set<Pair<CGNode, NewSiteReference>>> t) {
      if (t.snd.isEmpty()) {
        return true;
      }
      
      Set<Pair<String, Integer>> x = HashSetFactory.make(map(t.fst));
      x.retainAll(map(t.snd));
      return ! x.isEmpty();
    }
  }

  private final JavaScriptTranslatorFactory factory;
    
  protected TestPointerAnalyses(JavaScriptTranslatorFactory factory) {
    this.factory = factory;
    JSCallGraphUtil.setTranslatorFactory(factory);
  }

  private Pair<CGNode, NewSiteReference> map(CallGraph CG, Pair<CGNode, NewSiteReference> ptr) {
    CGNode n = ptr.fst;
    
    if (! (n.getMethod() instanceof JavaScriptConstructor)) {
      return ptr;
    }
    
    Iterator<CGNode> preds = CG.getPredNodes(n);
    
    if (! preds.hasNext()) {
      return ptr;
    }
    
    CGNode caller = preds.next();
    assert !preds.hasNext() : n;
    
    Iterator<CallSiteReference> sites = CG.getPossibleSites(caller, n);
    CallSiteReference site = sites.next();
    assert ! sites.hasNext();
  
    return Pair.make(caller, new NewSiteReference(site.getProgramCounter(), ptr.snd.getDeclaredType()));
  }
  
  private Set<Pair<CGNode, NewSiteReference>> map(CallGraph CG, Set<Pair<CGNode, NewSiteReference>> ptrs) {
    Set<Pair<CGNode, NewSiteReference>> result = HashSetFactory.make();
    for(Pair<CGNode, NewSiteReference> ptr : ptrs) {
      result.add(map(CG, ptr));
    }
    return result;
  }
  
  private Set<Pair<CGNode, NewSiteReference>> ptrs(Set<CGNode> functions, int local, CallGraph CG, PointerAnalysis<? extends InstanceKey> pa) {
    Set<Pair<CGNode, NewSiteReference>> result = HashSetFactory.make();

    for(CGNode n : functions) {
      PointerKey l = pa.getHeapModel().getPointerKeyForLocal(n, local);
      if (l != null) {
        OrdinalSet<? extends InstanceKey> pointers = pa.getPointsToSet(l);
        if (pointers != null) {
          for(InstanceKey k : pointers) {
            for(Iterator<Pair<CGNode, NewSiteReference>> css = k.getCreationSites(CG); css.hasNext(); ) {
              result.add(css.next());
            }
          }
        }
      }
    }

    return result;
  }
  
  private boolean isGlobal(Set<CGNode> functions, int local, PointerAnalysis<? extends InstanceKey> pa) {
    for(CGNode n : functions) {
      PointerKey l = pa.getHeapModel().getPointerKeyForLocal(n, local);
      if (l != null) {
        OrdinalSet<? extends InstanceKey> pointers = pa.getPointsToSet(l);
        if (pointers != null) {
          for(InstanceKey k : pointers) {
            if (k instanceof GlobalObjectKey || k instanceof GlobalVertex) {
              return true;
            }
          }
        }
      }
    }
    
    return false;
  }
  
  private void testPage(URL page, Predicate<MethodReference> filter, Predicate<Pair<Set<Pair<CGNode, NewSiteReference>>, Set<Pair<CGNode, NewSiteReference>>>> test) throws IOException, WalaException, CancelException {
    boolean save = JSSourceExtractor.USE_TEMP_NAME;
    try {
      JSSourceExtractor.USE_TEMP_NAME = false;

      FieldBasedCGUtil fb = new FieldBasedCGUtil(factory);
      Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> fbResult = fb.buildCG(page, BuilderType.OPTIMISTIC, true);
 
      JSCFABuilder propagationBuilder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(page);
      CallGraph propCG = propagationBuilder.makeCallGraph(propagationBuilder.getOptions());
      PointerAnalysis<InstanceKey> propPA = propagationBuilder.getPointerAnalysis();

      test(filter, test, fbResult.fst, fbResult.snd, propCG, propPA); 
      
    } finally {
      JSSourceExtractor.USE_TEMP_NAME = save;
    }
  }

  private void testTestScript(String dir, String name, Predicate<MethodReference> filter, Predicate<Pair<Set<Pair<CGNode, NewSiteReference>>, Set<Pair<CGNode, NewSiteReference>>>> test) throws IOException, WalaException, CancelException {
    boolean save = JSSourceExtractor.USE_TEMP_NAME;
    try {
      JSSourceExtractor.USE_TEMP_NAME = false;

      FieldBasedCGUtil fb = new FieldBasedCGUtil(factory);
      Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> fbResult = fb.buildTestCG(dir, name, BuilderType.OPTIMISTIC, new NullProgressMonitor(), true);
 
      JSCFABuilder propagationBuilder = JSCallGraphBuilderUtil.makeScriptCGBuilder(dir, name);
      CallGraph propCG = propagationBuilder.makeCallGraph(propagationBuilder.getOptions());
      PointerAnalysis<InstanceKey> propPA = propagationBuilder.getPointerAnalysis();

      test(filter, test, fbResult.fst, fbResult.snd, propCG, propPA); 
      
    } finally {
      JSSourceExtractor.USE_TEMP_NAME = save;
    }
  }

  protected void test(Predicate<MethodReference> filter,
      Predicate<Pair<Set<Pair<CGNode, NewSiteReference>>, Set<Pair<CGNode, NewSiteReference>>>> test, CallGraph fbCG,
      PointerAnalysis<ObjectVertex> fbPA, CallGraph propCG, PointerAnalysis<InstanceKey> propPA) {
    Set<MethodReference> functionsToCompare = HashSetFactory.make();
    for(CGNode n : fbCG) {
      MethodReference ref = n.getMethod().getReference();
      if (filter.test(ref) && !propCG.getNodes(ref).isEmpty()) {
        functionsToCompare.add(ref);
      }
    }

    System.err.println(fbCG);
    
    for(MethodReference function : functionsToCompare) {
      System.err.println("testing " + function);

      Set<CGNode> fbNodes = fbCG.getNodes(function);
      Set<CGNode> propNodes = propCG.getNodes(function);

      System.err.println(fbNodes.iterator().next().getIR());
      
      int maxVn = -1;
      for(CallGraph cg : new CallGraph[]{fbCG, propCG}) {
        for(CGNode n : cg) {
          IR nir = n.getIR();
          if (nir != null && nir.getSymbolTable().getMaxValueNumber() > maxVn) {
            maxVn = nir.getSymbolTable().getMaxValueNumber();
          }
        }
      }

      for(int i = 1; i <= maxVn; i++) {
        Set<Pair<CGNode, NewSiteReference>> fbPtrs = ptrs(fbNodes, i, fbCG, fbPA);
        Set<Pair<CGNode, NewSiteReference>> propPtrs = map(propCG, ptrs(propNodes, i, propCG, propPA));

        Assert.assertTrue("analysis should agree on global object for " + i + " of " + fbNodes.iterator().next().getIR(), isGlobal(fbNodes, i, fbPA) == isGlobal(propNodes, i, propPA));
        
        if (!fbPtrs.isEmpty() || !propPtrs.isEmpty()) {
          System.err.println("checking local " + i + " of " + function + ": " + fbPtrs + " vs " + propPtrs);
        }
        
        Assert.assertTrue(fbPtrs + " should intersect  " + propPtrs + " for " + i + " of " + fbNodes.iterator().next().getIR(), test.test(Pair.make(fbPtrs, propPtrs)));
      }
    }
    
    for(InstanceKey k : fbPA.getInstanceKeys()) {
      k.getCreationSites(fbCG);
    }
  }
  
  private void testPageUserCodeEquivalent(URL page) throws IOException, WalaException, CancelException {
    final String name = page.getFile().substring(page.getFile().lastIndexOf('/')+1, page.getFile().lastIndexOf('.'));
    testPage(page, nameFilter(name), new CheckPointers());
  }

  protected Predicate<MethodReference> nameFilter(final String name) {
    return new Predicate<MethodReference>() {
      @Override
      public boolean test(MethodReference t) {
        System.err.println(t + "  " + name);
        return t.getSelector().equals(AstMethodReference.fnSelector) &&
            t.getDeclaringClass().getName().toString().startsWith("L" + name);
      }      
    };
  }
  
  @Test
  public void testWindowOnload() throws IOException, WalaException, CancelException {
    testPageUserCodeEquivalent(getClass().getClassLoader().getResource("pages/windowonload.html"));
  }

  @Test
  public void testObjects() throws IOException, WalaException, CancelException {
    testTestScript("tests", "objects.js", nameFilter("tests/objects.js"), new CheckPointers());
  }
}