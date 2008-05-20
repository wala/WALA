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
package com.ibm.wala.viz;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph.ExplodedBasicBlock;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * An SWT action that spawns spawns a ghostview to see the local supergraph for
 * a proecedure node which is the current selection in a tree viewer.
 * 
 * @author sfink
 */
public class ViewIFDSLocalAction<T, P> extends Action {
  /**
   * Governing tree viewer
   */
  private final SWTTreeViewer viewer;

  /**
   * Governing supergraph
   */
  private final ISupergraph<T, P> supergraph;

  /**
   * name of postscript file to generate
   */
  private final String psFile;

  /**
   * name of dot file to generate
   */
  private final String dotFile;

  /**
   * path to dot.exe
   */
  private final String dotExe;

  /**
   * path to ghostview executable
   */
  private final String gvExe;

  private final NodeDecorator labels;

  public ViewIFDSLocalAction(SWTTreeViewer viewer, TabulationResult<T, P> result, String psFile, String dotFile, String dotExe,
      String gvExe) {
    this.viewer = viewer;
    this.supergraph = result.getProblem().getSupergraph();
    this.psFile = psFile;
    this.dotFile = dotFile;
    this.dotExe = dotExe;
    this.gvExe = gvExe;
    this.labels = new Labels(result);
    setText("View Local Supergraph");
  }

  private class Labels implements NodeDecorator {
    private TabulationResult<T, P> result;

    Labels(TabulationResult<T, P> result) {
      this.result = result;
    }

    @SuppressWarnings("unchecked")
    public String getLabel(Object o) throws WalaException {
      T t = (T) o;
      if (t instanceof BasicBlockInContext) {
        BasicBlockInContext bb = (BasicBlockInContext)t;
        if (bb.getDelegate() instanceof ExplodedBasicBlock) { 
          ExplodedBasicBlock delegate = (ExplodedBasicBlock) bb.getDelegate();
          String s = delegate.getNumber() + " " + result.getResult(t) +  " " + stringify(delegate.getInstruction());
          for (Iterator<SSAPhiInstruction> phis = delegate.iteratePhis(); phis.hasNext(); ) {
            SSAPhiInstruction phi = phis.next();
            s += " " + phi;
          }
          if (delegate.isCatchBlock()) {
            s += " " + delegate.getCatchInstruction();
          }
          return s;
        }
      }
      return t + " " + result.getResult(t);
    }
  }
  
  public static String stringify(SSAInstruction s) {
    if (s == null) {
      return null;
    }
    if (s instanceof SSAAbstractInvokeInstruction) {
      SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction)s;
      String def = call.hasDef() ? Integer.valueOf(call.getDef()) + "=" : "";
      return def + "call " + call.getDeclaredTarget().getDeclaringClass().getName().getClassName() + "." + call.getDeclaredTarget().getName();
    }
    return s.toString();
  }

  /*
   * @see org.eclipse.jface.action.IAction#run()
   */
  @Override
  public void run() {

    try {
      final P proc = getProcedureForSelection();
      Filter<T> filter = new Filter<T>() {
        public boolean accepts(T o) {
          return supergraph.getProcOf(o).equals(proc);
        }
      };
      Graph<T> localGraph = GraphSlicer.prune(supergraph, filter);

      // spawn the viewer
      System.err.println("Spawn Viewer for " + proc);
      DotUtil.dotify(localGraph, labels, dotFile, psFile, dotExe);

      GVUtil.launchGV(psFile, gvExe);
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  @SuppressWarnings("unchecked")
  protected P getProcedureForSelection() {
    // we assume the tree viewer's current selection is a P
    IStructuredSelection selection = viewer.getSelection();
    if (selection.size() != 1) {
      throw new UnsupportedOperationException("did not expect selection of size " + selection.size());
    }
    P first = (P) selection.getFirstElement();

    return first;
  }

  protected SWTTreeViewer getViewer() {
    return viewer;
  }

  protected ISupergraph<T, P> getSupergraph() {
    return supergraph;
  }

  protected String getDotExe() {
    return dotExe;
  }

  protected String getDotFile() {
    return dotFile;
  }

  protected String getGvExe() {
    return gvExe;
  }

  protected String getPsFile() {
    return psFile;
  }
}