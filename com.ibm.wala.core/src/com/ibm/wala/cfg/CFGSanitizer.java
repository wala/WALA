/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cfg;

import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

/**
 * Utility class to remove exceptional edges to exit() from a CFG
 */
public class CFGSanitizer {

  /**
   * Return a view of the {@link ControlFlowGraph} for an {@link IR}, which elides all exceptional exits from PEIs in the IR.
   */
  public static Graph<ISSABasicBlock> sanitize(IR ir, IClassHierarchy cha) throws IllegalArgumentException, WalaException {

    if (ir == null) {
      throw new IllegalArgumentException("ir cannot be null");
    }

    ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = ir.getControlFlowGraph();
    Graph<ISSABasicBlock> g = SlowSparseNumberedGraph.make();
    // add all nodes to the graph
    for (ISSABasicBlock basicBlock : cfg) {
      g.addNode(basicBlock);
    }

    // add all edges to the graph, except those that go to exit
    for (ISSABasicBlock b : cfg) {
      for (ISSABasicBlock b2 : Iterator2Iterable.make(cfg.getSuccNodes(b))) {
        if (!b2.isExitBlock()) {
          g.addEdge(b, b2);
        }
      }
    }

    // now add edges to exit, ignoring undeclared exceptions
    ISSABasicBlock exit = cfg.exit();

    for (ISSABasicBlock b : Iterator2Iterable.make(cfg.getPredNodes(exit))) {
      // for each predecessor of exit ...

      SSAInstruction s = ir.getInstructions()[b.getLastInstructionIndex()];
      if (s == null) {
        // TODO: this shouldn't happen?
        continue;
      }
      if (s instanceof SSAReturnInstruction || s instanceof SSAThrowInstruction || cfg.getSuccNodeCount(b) == 1) {
        // return or athrow, or some statement which is not an athrow or return whose only successor is the exit node (can only
        // occur in synthetic methods without a return statement? --MS); add edge to exit
        g.addEdge(b, exit);
      } else {
        // compute types of exceptions the pei may throw
        TypeReference[] exceptions = null;
        try {
          exceptions = computeExceptions(cha, ir, s);
        } catch (InvalidClassFileException e1) {
          e1.printStackTrace();
          Assertions.UNREACHABLE();
        }
        // remove any exceptions that are caught by catch blocks
        for (ISSABasicBlock c : Iterator2Iterable.make(cfg.getSuccNodes(b))) {

          if (c.isCatchBlock()) {
            SSACFG.ExceptionHandlerBasicBlock cb = (ExceptionHandlerBasicBlock) c;

            for (TypeReference ex : Iterator2Iterable.make(cb.getCaughtExceptionTypes())) {
              IClass exClass = cha.lookupClass(ex);
              if (exClass == null) {
                throw new WalaException("failed to find " + ex);
              }
              for (int i = 0; i < exceptions.length; i++) {
                if (exceptions[i] != null) {
                  IClass exi = cha.lookupClass(exceptions[i]);
                  if (exi == null) {
                    throw new WalaException("failed to find " + exceptions[i]);
                  }
                  if (cha.isSubclassOf(exi, exClass)) {
                    exceptions[i] = null;
                  }
                }
              }
            }
          }
        }
        // check the remaining uncaught exceptions
        TypeReference[] declared = null;
        try {
          declared = ir.getMethod().getDeclaredExceptions();
        } catch (InvalidClassFileException e) {
          e.printStackTrace();
          Assertions.UNREACHABLE();
        }
        if (declared != null && exceptions != null) {
          for (TypeReference exception : exceptions) {
            boolean isDeclared = false;
            if (exception != null) {
              IClass exi = cha.lookupClass(exception);
              if (exi == null) {
                throw new WalaException("failed to find " + exception);
              }
              for (TypeReference element : declared) {
                IClass dc = cha.lookupClass(element);
                if (dc == null) {
                  throw new WalaException("failed to find " + element);
                }
                if (cha.isSubclassOf(exi, dc)) {
                  isDeclared = true;
                  break;
                }
              }
              if (isDeclared) {
                // found a declared exceptional edge
                g.addEdge(b, exit);
              }
            }
          }
        }
      }
    }
    return g;
  }

  /**
   * What are the exception types which s may throw?
   */
  private static TypeReference[] computeExceptions(IClassHierarchy cha, IR ir, SSAInstruction s) throws InvalidClassFileException {
    Collection<TypeReference> c = null;
    Language l = ir.getMethod().getDeclaringClass().getClassLoader().getLanguage();
    if (s instanceof SSAInvokeInstruction) {
      SSAInvokeInstruction call = (SSAInvokeInstruction) s;
      c = l.inferInvokeExceptions(call.getDeclaredTarget(), cha);
    } else {
      c = s.getExceptionTypes();
    }
    if (c == null) {
      return null;
    } else {
      TypeReference[] exceptions = new TypeReference[c.size()];
      Iterator<TypeReference> it = c.iterator();
      for (int i = 0; i < exceptions.length; i++) {
        exceptions[i] = it.next();
      }
      return exceptions;
    }
  }

}
