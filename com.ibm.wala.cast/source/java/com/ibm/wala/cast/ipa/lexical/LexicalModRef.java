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
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Compute mod-ref information limited to accesses of lexical variables.
 * 
 */
public class LexicalModRef {

  public static LexicalModRef make(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
    return new LexicalModRef(cg, pa);
  }

  private final CallGraph cg;

  private final PointerAnalysis<InstanceKey> pa;

  protected LexicalModRef(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
    this.cg = cg;
    this.pa = pa;
  }

  /**
   * Compute the lexical variables possibly read by each {@link CGNode} and its
   * transitive callees. A lexical variable is represented as a pair (C,N),
   * where C is the defining {@link CGNode} and N is the {@link String} name.
   */
  public Map<CGNode, OrdinalSet<Pair<CGNode, String>>> computeLexicalRef() {
    Map<CGNode, Collection<Pair<CGNode, String>>> scan = CallGraphTransitiveClosure.collectNodeResults(cg,
        this::scanNodeForLexReads);
    return CallGraphTransitiveClosure.transitiveClosure(cg, scan);
  }

  /**
   * Compute the lexical variables possibly modified by each {@link CGNode} and
   * its transitive callees. A lexical variable is represented as a pair (C,N),
   * where C is the defining {@link CGNode} and N is the {@link String} name.
   */
  public Map<CGNode, OrdinalSet<Pair<CGNode, String>>> computeLexicalMod() {
    Map<CGNode, Collection<Pair<CGNode, String>>> scan = CallGraphTransitiveClosure.collectNodeResults(cg,
        this::scanNodeForLexWrites);
    return CallGraphTransitiveClosure.transitiveClosure(cg, scan);
  }

  protected Collection<Pair<CGNode, String>> scanNodeForLexReads(CGNode n) {
    Collection<Pair<CGNode, String>> result = HashSetFactory.make();
    IR ir = n.getIR();
    if (ir != null) {
      for (SSAInstruction instr : Iterator2Iterable.make(ir.iterateNormalInstructions())) {
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
      for (SSAInstruction instr : Iterator2Iterable.make(ir.iterateNormalInstructions())) {
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
          result.add(Pair.make(definerNode, nameAndDefiner.fst));
        }
      }
    }
    return result;
  }
}
