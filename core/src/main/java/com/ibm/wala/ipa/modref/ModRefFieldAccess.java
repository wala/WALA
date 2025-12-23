/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.ipa.modref;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Computes interprocedural field accesses for a given method.
 *
 * @author Martin Seidel
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public final class ModRefFieldAccess {

  private static class TwoMaps {
    private Map<IClass, Set<IField>> mods;
    private Map<IClass, Set<IField>> refs;

    public TwoMaps(Map<IClass, Set<IField>> mods, Map<IClass, Set<IField>> refs) {
      this.mods = mods;
      this.refs = refs;
      if (mods == null) {
        this.mods = new HashMap<>();
      }
      if (refs == null) {
        this.refs = new HashMap<>();
      }
    }

    public Map<IClass, Set<IField>> getMods() {
      return mods;
    }

    public Map<IClass, Set<IField>> getRefs() {
      return refs;
    }
  }

  private final CallGraph cg;

  private final Map<CGNode, @NonNull Map<IClass, Set<IField>>> mods;
  private final Map<CGNode, @NonNull Map<IClass, Set<IField>>> refs;
  private final Map<CGNode, @NonNull Map<IClass, Set<IField>>> tmods;
  private final Map<CGNode, @NonNull Map<IClass, Set<IField>>> trefs;

  private final List<CGNode> done;

  private ModRefFieldAccess(CallGraph cg) {
    this.cg = cg;
    this.refs = new HashMap<>();
    this.mods = new HashMap<>();
    this.trefs = new HashMap<>();
    this.tmods = new HashMap<>();
    this.done = new ArrayList<>();
  }

  public static ModRefFieldAccess compute(CallGraph cg) {
    ModRefFieldAccess fa = new ModRefFieldAccess(cg);
    fa.run();

    return fa;
  }

  public Map<IClass, Set<IField>> getMod(CGNode node) {
    return mods.get(node);
  }

  public Map<IClass, Set<IField>> getRef(CGNode node) {
    return refs.get(node);
  }

  public Map<IClass, Set<IField>> getTransitiveMod(CGNode node) {
    return tmods.get(node);
  }

  public Map<IClass, Set<IField>> getTransitiveRef(CGNode node) {
    return trefs.get(node);
  }

  private void run() {

    for (CGNode cgNode : cg) {
      Map<IClass, Set<IField>> refsMap = refs.computeIfAbsent(cgNode, absent -> new HashMap<>());
      Map<IClass, Set<IField>> modsMap = mods.computeIfAbsent(cgNode, absent -> new HashMap<>());

      final IR ir = cgNode.getIR();

      if (ir == null) {
        continue;
      }

      for (SSAInstruction instr : Iterator2Iterable.make(ir.iterateNormalInstructions())) {
        if (instr instanceof SSAGetInstruction) {
          SSAGetInstruction get = (SSAGetInstruction) instr;
          FieldReference fref = get.getDeclaredField();
          IField field = cg.getClassHierarchy().resolveField(fref);
          if (field != null) {
            IClass cls = field.getDeclaringClass();
            if (cls != null) {
              refsMap.computeIfAbsent(cls, absent -> new HashSet<>()).add(field);
            }
          }
        } else if (instr instanceof SSAPutInstruction) {
          SSAPutInstruction put = (SSAPutInstruction) instr;
          FieldReference fput = put.getDeclaredField();
          IField field = cg.getClassHierarchy().resolveField(fput);
          if (field != null) {
            IClass cls = field.getDeclaringClass();
            if (cls != null) {
              modsMap.computeIfAbsent(cls, absent -> new HashSet<>()).add(field);
            }
          }
        }
      }
    }

    recAdd(cg.getFakeRootNode());
  }

  private TwoMaps recAdd(CGNode node) {
    Map<IClass, Set<IField>> trefsMap = trefs.computeIfAbsent(node, absent -> new HashMap<>());
    Map<IClass, Set<IField>> tmodsMap = tmods.computeIfAbsent(node, absent -> new HashMap<>());

    final IR ir = node.getIR();
    if (ir != null) {
      for (SSAInstruction instr : Iterator2Iterable.make(ir.iterateNormalInstructions())) {
        if (instr instanceof SSAGetInstruction) {
          SSAGetInstruction get = (SSAGetInstruction) instr;
          FieldReference fref = get.getDeclaredField();
          IField field = cg.getClassHierarchy().resolveField(fref);
          if (field != null) {
            IClass cls = field.getDeclaringClass();
            if (cls != null) {
              trefsMap.computeIfAbsent(cls, absent -> new HashSet<>()).add(field);
            }
          }
        } else if (instr instanceof SSAPutInstruction) {
          SSAPutInstruction put = (SSAPutInstruction) instr;
          FieldReference fput = put.getDeclaredField();
          IField field = cg.getClassHierarchy().resolveField(fput);
          if (field != null) {
            IClass cls = field.getDeclaringClass();
            if (cls != null) {
              tmodsMap.computeIfAbsent(cls, absent -> new HashSet<>()).add(field);
            }
          }
        }
      }
    }

    for (CGNode n : Iterator2Iterable.make(cg.getSuccNodes(node))) {
      if (!done.contains(n)) {
        done.add(n);
        TwoMaps t = recAdd(n);
        for (IClass c : t.getRefs().keySet()) {
          Map<IClass, Set<IField>> setMap = trefs.get(node);
          setMap.compute(
              c,
              (key, priorValue) -> {
                if (priorValue != null) {
                  priorValue.addAll(t.getRefs().get(key));
                  return priorValue;
                } else {
                  return t.getRefs().get(key);
                }
              });
        }
        for (IClass c : t.getMods().keySet()) {
          tmods
              .get(node)
              .compute(
                  c,
                  (key, priorValue) -> {
                    Set<IField> iFields = t.getMods().get(c);
                    if (priorValue == null) {
                      return iFields;
                    }
                    priorValue.addAll(iFields);
                    return priorValue;
                  });
        }
      }
    }

    return new TwoMaps(tmods.get(node), trefs.get(node));
  }
}
