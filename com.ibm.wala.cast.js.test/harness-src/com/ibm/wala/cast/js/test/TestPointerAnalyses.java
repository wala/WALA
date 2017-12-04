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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.cast.ipa.callgraph.GlobalObjectKey;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.GlobalVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.ObjectVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.PrototypeFieldVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.PrototypeFieldVertex.PrototypeField;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.TransitivePrototypeKey;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptConstructorFunctions.JavaScriptConstructor;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.test.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstDynamicField;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

public abstract class TestPointerAnalyses {

  private final class CheckPointers implements Predicate<Pair<Set<Pair<CGNode, NewSiteReference>>, Set<Pair<CGNode, NewSiteReference>>>> {
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

  private static Pair<CGNode, NewSiteReference> map(CallGraph CG, Pair<CGNode, NewSiteReference> ptr) {
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
  
  private static Set<Pair<CGNode, NewSiteReference>> map(CallGraph CG, Set<Pair<CGNode, NewSiteReference>> ptrs) {
    Set<Pair<CGNode, NewSiteReference>> result = HashSetFactory.make();
    for(Pair<CGNode, NewSiteReference> ptr : ptrs) {
      result.add(map(CG, ptr));
    }
    return result;
  }
  
  private static Set<Pair<CGNode, NewSiteReference>> ptrs(Set<CGNode> functions, int local, CallGraph CG, PointerAnalysis<? extends InstanceKey> pa) {
    Set<Pair<CGNode, NewSiteReference>> result = HashSetFactory.make();

    for(CGNode n : functions) {
      PointerKey l = pa.getHeapModel().getPointerKeyForLocal(n, local);
      if (l != null) {
        OrdinalSet<? extends InstanceKey> pointers = pa.getPointsToSet(l);
        if (pointers != null) {
          for(InstanceKey k : pointers) {
            for(Pair<CGNode, NewSiteReference> cs : Iterator2Iterable.make(k.getCreationSites(CG))) {
              result.add(cs);
            }
          }
        }
      }
    }

    return result;
  }
  
  private static boolean isGlobal(Set<CGNode> functions, int local, PointerAnalysis<? extends InstanceKey> pa) {
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
  
  private void testPage(URL page, Predicate<MethodReference> filter, Predicate<Pair<Set<Pair<CGNode, NewSiteReference>>, Set<Pair<CGNode, NewSiteReference>>>> test) throws WalaException, CancelException {
    boolean save = JSSourceExtractor.USE_TEMP_NAME;
    try {
      JSSourceExtractor.USE_TEMP_NAME = false;

      FieldBasedCGUtil fb = new FieldBasedCGUtil(factory);
      Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> fbResult = fb.buildCG(page, BuilderType.OPTIMISTIC, true, DefaultSourceExtractor.factory);
 
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
    HeapGraph<ObjectVertex> hg = fbPA.getHeapGraph();
    
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
      
      CGNode node = fbNodes.iterator().next();
      IR ir = node.getIR();
      System.err.println(ir);
      
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

        Assert.assertTrue("analysis should agree on global object for " + i + " of " + ir, isGlobal(fbNodes, i, fbPA) == isGlobal(propNodes, i, propPA));
        
        if (!fbPtrs.isEmpty() || !propPtrs.isEmpty()) {
          System.err.println("checking local " + i + " of " + function + ": " + fbPtrs + " vs " + propPtrs);
        }
        
        Assert.assertTrue(fbPtrs + " should intersect  " + propPtrs + " for " + i + " of " + ir, test.test(Pair.make(fbPtrs, propPtrs)));
      }

      SymbolTable symtab = ir.getSymbolTable();
      for(SSAInstruction inst : ir.getInstructions()) {
        if (inst instanceof JavaScriptPropertyWrite) {
          int property = ((JavaScriptPropertyWrite) inst).getMemberRef();
          if (symtab.isConstant(property)) {
            String p = JSCallGraphUtil.simulateToStringForPropertyNames(symtab.getConstantValue(property));
            
            int obj = ((JavaScriptPropertyWrite) inst).getObjectRef();
            PointerKey objKey = fbPA.getHeapModel().getPointerKeyForLocal(node, obj);
            OrdinalSet<ObjectVertex> objPtrs = fbPA.getPointsToSet(objKey);
            for(ObjectVertex o : objPtrs) {
              PointerKey propKey = fbPA.getHeapModel().getPointerKeyForInstanceField(o, new AstDynamicField(false, o.getConcreteType(), Atom.findOrCreateUnicodeAtom(p), JavaScriptTypes.Root));
              Assert.assertTrue("object " + o + " should have field " + propKey, hg.hasEdge(o, propKey));

              int val = ((JavaScriptPropertyWrite) inst).getValue();
              PointerKey valKey = fbPA.getHeapModel().getPointerKeyForLocal(node, val);
              OrdinalSet<ObjectVertex> valPtrs = fbPA.getPointsToSet(valKey);
              for(ObjectVertex v : valPtrs) {
                Assert.assertTrue("field " + propKey + " should point to object " + valKey + "(" + v + ")", hg.hasEdge(propKey, v));
              }
            }
                     
            System.err.println("heap graph models instruction " + inst);
          }          
        } else if (inst instanceof AstGlobalWrite) {
          String propName = ((AstGlobalWrite) inst).getGlobalName();
          propName = propName.substring("global ".length());
          PointerKey propKey = fbPA.getHeapModel().getPointerKeyForInstanceField(null, new AstDynamicField(false, null, Atom.findOrCreateUnicodeAtom(propName), JavaScriptTypes.Root));
          Assert.assertTrue("global " + propName + " should exist", hg.hasEdge(GlobalVertex.instance(), propKey));

          System.err.println("heap graph models instruction " + inst);
        } else if (inst instanceof JavaScriptInvoke) {
          int vn = ((JavaScriptInvoke) inst).getReceiver();
          
          Set<Pair<CGNode, NewSiteReference>> fbPrototypes = getFbPrototypes(fbPA, hg, fbCG, node, vn);
          Set<Pair<CGNode, NewSiteReference>> propPrototypes = getPropPrototypes(propPA, propCG, node, vn);
          Assert.assertTrue("should have prototype overlap for " + fbPrototypes + " and " + propPrototypes + " at " + inst,
              (fbPrototypes.isEmpty() && propPrototypes.isEmpty()) || !Collections.disjoint(fbPrototypes, propPrototypes));
        }
      } 
    }
    
    for(InstanceKey k : fbPA.getInstanceKeys()) {
      k.getCreationSites(fbCG);
      for(String f :  new String[]{ "__proto__", "prototype" }) {
        boolean dump = false;
        PointerKey pointerKeyForInstanceField = fbPA.getHeapModel().getPointerKeyForInstanceField(k, new AstDynamicField(false, k.getConcreteType(), Atom.findOrCreateUnicodeAtom(f), JavaScriptTypes.Root));
        if (! hg.containsNode(pointerKeyForInstanceField)) {
          dump = true;
          System.err.println("no " + f + " for " + k + "(" + k.getConcreteType() + ")");
        } else if (! hg.getSuccNodes(pointerKeyForInstanceField).hasNext()){
          dump = true;
          System.err.println("empty " + f + " for " + k + "(" + k.getConcreteType() + ")");          
        }
        if (dump) {
          for(Pair<CGNode, NewSiteReference> cs : Iterator2Iterable.make(k.getCreationSites(fbCG))) {
            System.err.println(cs);
          }
        }
      }
    }
  }

  private static <T extends InstanceKey> Set<Pair<CGNode, NewSiteReference>> getPrototypeSites(PointerAnalysis<T> fbPA, 
      CallGraph CG,
      Function<T,Iterator<T>> proto,
      CGNode node, 
      int vn) {
    Set<Pair<CGNode, NewSiteReference>> fbProtos = HashSetFactory.make();
    PointerKey fbKey = fbPA.getHeapModel().getPointerKeyForLocal(node, vn);
    OrdinalSet<T> fbPointsTo = fbPA.getPointsToSet(fbKey);
    for(T o : fbPointsTo) {
      for(T p : Iterator2Iterable.make(proto.apply(o))) {
        for(Pair<CGNode, NewSiteReference> cs : Iterator2Iterable.make(p.getCreationSites(CG))) {
          fbProtos.add(cs);
        }
      }
    }
    return fbProtos;
  }

  private static Set<Pair<CGNode, NewSiteReference>> getFbPrototypes(PointerAnalysis<ObjectVertex> fbPA, 
      final HeapGraph<ObjectVertex> hg,
      CallGraph CG,
      CGNode node, 
      int vn) {
    return getPrototypeSites(fbPA, CG, o -> {
      PrototypeFieldVertex proto = new PrototypeFieldVertex(PrototypeField.__proto__, o);
      if (hg.containsNode(proto)) {
      return 
          new MapIterator<>(hg.getSuccNodes(proto),
              ObjectVertex.class::cast);
      } else {
        return EmptyIterator.instance();
      }
    }, node, vn);
  }

  private static Set<Pair<CGNode, NewSiteReference>> getPropPrototypes(final PointerAnalysis<InstanceKey> fbPA, 
      CallGraph CG, 
      CGNode node, 
      int vn) {
    return getPrototypeSites(fbPA, CG, o -> fbPA.getPointsToSet(new TransitivePrototypeKey(o)).iterator(), node, vn);
  }

  private void testPageUserCodeEquivalent(URL page) throws WalaException, CancelException {
    final String name = page.getFile().substring(page.getFile().lastIndexOf('/')+1, page.getFile().lastIndexOf('.'));
    testPage(page, nameFilter(name), new CheckPointers());
  }

  protected Predicate<MethodReference> nameFilter(final String name) {
    return t -> {
      System.err.println(t + "  " + name);
      return t.getSelector().equals(AstMethodReference.fnSelector) &&
          t.getDeclaringClass().getName().toString().startsWith("L" + name);
    };
  }
  
  @Test
  public void testWindowOnload() throws WalaException, CancelException {
    testPageUserCodeEquivalent(getClass().getClassLoader().getResource("pages/windowonload.html"));
  }

  @Test
  public void testObjects() throws IOException, WalaException, CancelException {
    testTestScript("tests", "objects.js", nameFilter("tests/objects.js"), new CheckPointers());
  }

  @Test
  public void testInherit() throws IOException, WalaException, CancelException {
    testTestScript("tests", "inherit.js", nameFilter("tests/inherit.js"), new CheckPointers());
  }
}
