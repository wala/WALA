/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.ipa.lexical;

import java.util.Collection;
import java.util.Map;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys.ScopeMappingInstanceKey;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphTransitiveClosure;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Given a call graph / pointer analysis, determines the lexical variables
 * accessed by each call graph node and its transitive callees. Essentially, a
 * mod-ref analysis limited to lexical variables.
 * 
 * TODO Share even more code with {@link ModRef}?
 * 
 */
public class TransitiveLexicalAccesses {

  public static TransitiveLexicalAccesses make(CallGraph cg, PointerAnalysis pa) {
    return new TransitiveLexicalAccesses(cg, pa);
  }



  private final CallGraph cg;
  
  private final PointerAnalysis pa;
  
  
  
  protected TransitiveLexicalAccesses(CallGraph cg, PointerAnalysis pa) {
    this.cg = cg;
    this.pa = pa;
  }

  public Map<CGNode, OrdinalSet<Pair<CGNode, String>>> computeLexVarsRead() {
    Map<CGNode, Collection<Pair<CGNode, String>>> scan = CallGraphTransitiveClosure.collectNodeResults(cg, new Function<CGNode, Collection<Pair<CGNode,String>>>() {
    
      public Collection<Pair<CGNode, String>> apply(CGNode n) {
        return scanNodeForLexReads(n);
      }
    });
    return CallGraphTransitiveClosure.transitiveClosure(cg, scan);
  }

  public Map<CGNode, OrdinalSet<Pair<CGNode, String>>> computeLexVarsWritten() {
    Map<CGNode, Collection<Pair<CGNode, String>>> scan = CallGraphTransitiveClosure.collectNodeResults(cg, new Function<CGNode, Collection<Pair<CGNode,String>>>() {

      public Collection<Pair<CGNode, String>> apply(CGNode n) {
        return scanNodeForLexWrites(n);
      }
    });
    return CallGraphTransitiveClosure.transitiveClosure(cg, scan);
  }

  protected Collection<Pair<CGNode, String>> scanNodeForLexReads(CGNode n) {
    Collection<Pair<CGNode, String>> result = HashSetFactory.make();
    IR ir = n.getIR();
    if (ir != null) {
      for (SSAInstruction instr: Iterator2Iterable.make(ir.iterateNormalInstructions())) {
        if (instr instanceof AstLexicalRead) {
          AstLexicalRead read = (AstLexicalRead) instr;
          for (Access a : read.getAccesses()) {
            Pair<String, String> nameAndDefiner = a.getName();
            result.addAll(getNodeNamePairsForAccess(n, nameAndDefiner));
          }
        }
      }
    }
    return result;
  }

  protected Collection<Pair<CGNode, String>> scanNodeForLexWrites(CGNode n) {
    Collection<Pair<CGNode, String>> result = HashSetFactory.make();
    IR ir = n.getIR();
    if (ir != null) {
      for (SSAInstruction instr: Iterator2Iterable.make(ir.iterateNormalInstructions())) {
        if (instr instanceof AstLexicalWrite) {
          AstLexicalWrite write = (AstLexicalWrite) instr;
          for (Access a : write.getAccesses()) {
            Pair<String, String> nameAndDefiner = a.getName();
            result.addAll(getNodeNamePairsForAccess(n, nameAndDefiner));
          }
        }
      }
    }
    return result;
  }

  private Collection<Pair<CGNode, String>> getNodeNamePairsForAccess(CGNode n, Pair<String, String> nameAndDefiner) {
    Collection<Pair<CGNode, String>> result = HashSetFactory.make();
    // use scope-mapping instance keys in pointer analysis. may need a different
    // scheme for CG construction not based on pointer analysis
    OrdinalSet<InstanceKey> functionValues = pa.getPointsToSet(pa.getHeapModel().getPointerKeyForLocal(n, 1));
    for (InstanceKey ik : functionValues) {
      if (ik instanceof ScopeMappingInstanceKey) {
        ScopeMappingInstanceKey smik = (ScopeMappingInstanceKey) ik;
        for (CGNode definerNode : Iterator2Iterable.make(smik.getFunargNodes(nameAndDefiner))) {
          result.add(Pair.make(definerNode,nameAndDefiner.fst));
        }
      }
    }
    return result;
  }
}
