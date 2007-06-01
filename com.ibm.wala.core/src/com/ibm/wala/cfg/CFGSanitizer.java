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
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * Utility class to remove edges to exit() from a CFG
 * 
 * @author Stephen Fink
 */
public class CFGSanitizer {

  /*
   */
  public static Graph<IBasicBlock> sanitize(IR ir, IClassHierarchy cha) throws IllegalArgumentException, WalaException {
 
    if (ir == null) {
      throw new IllegalArgumentException("ir cannot be null");
    }

    ControlFlowGraph cfg = ir.getControlFlowGraph();
    Graph<IBasicBlock> G = new SlowSparseNumberedGraph<IBasicBlock>();
    // add all nodes to the graph
    for (Iterator<? extends IBasicBlock> it = cfg.iterator(); it.hasNext();) {
      G.addNode(it.next());
    }

    // add all edges to the graph, except those that go to exit
    for (Iterator it = cfg.iterator(); it.hasNext();) {
      IBasicBlock b = (IBasicBlock) it.next();
      for (Iterator it2 = cfg.getSuccNodes(b); it2.hasNext();) {
        IBasicBlock b2 = (IBasicBlock) it2.next();

        if (!b2.isExitBlock()) {
          G.addEdge(b, b2);
        }
      }
    }

    // now add edges to exit, ignoring undeclared exceptions
    IBasicBlock exit = cfg.exit();

    for (Iterator it = cfg.getPredNodes(exit); it.hasNext();) {
      // for each predecessor of exit ...
      IBasicBlock b = (IBasicBlock) it.next();

      SSAInstruction s = ir.getInstructions()[b.getLastInstructionIndex()];
      if (s == null) {
        // TODO: this shouldn't happen?
        continue;
      }
      if (s instanceof SSAReturnInstruction || s instanceof SSAThrowInstruction) {
        // return or athrow: add edge to exit
        G.addEdge(b, exit);
      } else {
        // compute types of exceptions the pei may throw
        TypeReference[] exceptions = null;
        try {
          exceptions = computeExceptions(cha, s);
        } catch (InvalidClassFileException e1) {
          e1.printStackTrace();
          Assertions.UNREACHABLE();
        }
        // remove any exceptions that are caught by catch blocks
        for (Iterator it2 = cfg.getSuccNodes(b); it2.hasNext();) {
          IBasicBlock c = (IBasicBlock) it2.next();

          if (c.isCatchBlock()) {
            SSACFG.ExceptionHandlerBasicBlock cb = (ExceptionHandlerBasicBlock) c;

            for (Iterator it3 = cb.getCaughtExceptionTypes(); it3.hasNext();) {
              TypeReference ex = (TypeReference) it3.next();
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
          for (int i = 0; i < exceptions.length; i++) {
            boolean isDeclared = false;
            if (exceptions[i] != null) {
              IClass exi = cha.lookupClass(exceptions[i]);
              if (exi == null) {
                throw new WalaException("failed to find " + exceptions[i]);
              }
              for (int j = 0; j < declared.length; j++) {
                IClass dc = cha.lookupClass(declared[j]);
                if (dc == null) {
                  throw new WalaException("failed to find " + declared[j]);
                }
                if (cha.isSubclassOf(exi, dc)) {
                  isDeclared = true;
                  break;
                }
              }
              if (isDeclared) {
                // found a declared exceptional edge
                G.addEdge(b, exit);
              }
            }
          }
        }
      }
    }
    return G;
  }

  private static TypeReference[] computeExceptions(IClassHierarchy cha, SSAInstruction s) throws InvalidClassFileException {
    Collection c = null;
    if (s instanceof SSAInvokeInstruction) {
      SSAInvokeInstruction call = (SSAInvokeInstruction) s;
      c = Exceptions.inferInvokeExceptions(call.getDeclaredTarget(), cha, new WarningSet());
    } else {
      c = s.getExceptionTypes();
    }
    if (c == null) {
      return null;
    } else {
      TypeReference[] exceptions = new TypeReference[c.size()];
      Iterator it = c.iterator();
      for (int i = 0; i < exceptions.length; i++) {
        exceptions[i] = (TypeReference) it.next();
      }
      return exceptions;
    }
  }

} 
