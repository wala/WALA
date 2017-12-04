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
package com.ibm.wala.ide.ui;

import java.util.function.Predicate;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;
import com.ibm.wala.viz.PDFViewUtil;

/**
 * An SWT action that spawns spawns a ghostview to see the local supergraph for a procedure node which is the current selection in a
 * tree viewer.
 * 
 * @author sfink
 */
public class ViewIFDSLocalAction<T, P, F> extends Action {
  /**
   * Governing tree viewer
   */
  private final SWTTreeViewer viewer;

  /**
   * Governing supergraph
   */
  private final ISupergraph<T, P> supergraph;

  /**
   * name of PDF file to generate
   */
  private final String pdfFile;

  /**
   * name of dot file to generate
   */
  private final String dotFile;

  /**
   * path to dot.exe
   */
  private final String dotExe;

  /**
   * path to pdf view executable
   */
  private final String pdfViewExe;

  private final NodeDecorator<T> labels;

  public ViewIFDSLocalAction(SWTTreeViewer viewer, TabulationResult<T, P, F> result, String pdfFile, String dotFile, String dotExe,
      String pdfViewExe, NodeDecorator<T> labels) {
    if (result == null) {
      throw new IllegalArgumentException("null result");
    }
    this.viewer = viewer;
    this.supergraph = result.getProblem().getSupergraph();
    this.pdfFile = pdfFile;
    this.dotFile = dotFile;
    this.dotExe = dotExe;
    this.pdfViewExe = pdfViewExe;
    this.labels = labels;
    setText("View Local Supergraph");
  }

  public ViewIFDSLocalAction(SWTTreeViewer viewer, TabulationResult<T, P, F> result, String psFile, String dotFile, String dotExe,
      String gvExe) {
    if (result == null) {
      throw new IllegalArgumentException("null result");
    }
    this.viewer = viewer;
    this.supergraph = result.getProblem().getSupergraph();
    this.pdfFile = psFile;
    this.dotFile = dotFile;
    this.dotExe = dotExe;
    this.pdfViewExe = gvExe;
    this.labels = new Labels<>(result);
    setText("View Local Supergraph");
  }

  private static class Labels<T, P, F> implements NodeDecorator<T> {
    private TabulationResult<T, P, F> result;

    Labels(TabulationResult<T, P, F> result) {
      this.result = result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getLabel(Object o) throws WalaException {
      T t = (T) o;
      if (t instanceof BasicBlockInContext) {
        BasicBlockInContext bb = (BasicBlockInContext) t;
        if (bb.getDelegate() instanceof IExplodedBasicBlock) {
          IExplodedBasicBlock delegate = (IExplodedBasicBlock) bb.getDelegate();
          String s = delegate.getNumber() + " " + result.getResult(t) + "\\n" + stringify(delegate.getInstruction());
          for (SSAPhiInstruction phi : Iterator2Iterable.make(delegate.iteratePhis())) {
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

  /**
   * Print a short-ish representation of s as a String
   */
  public static String stringify(SSAInstruction s) {
    if (s == null) {
      return null;
    }
    if (s instanceof SSAAbstractInvokeInstruction) {
      SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) s;
      String def = call.hasDef() ? Integer.valueOf(call.getDef()) + "=" : "";
      String result = def + "call " + call.getDeclaredTarget().getDeclaringClass().getName().getClassName() + "."
          + call.getDeclaredTarget().getName();
      result += " exc:" + call.getException();
      for (int i = 0; i < s.getNumberOfUses(); i++) {
        result += " ";
        result += s.getUse(i);
      }
      return result;
    }
    if (s instanceof SSAGetInstruction) {
      SSAGetInstruction g = (SSAGetInstruction) s;
      String fieldName = g.getDeclaredField().getName().toString();

      StringBuffer result = new StringBuffer();
      result.append(g.getDef());
      result.append(":=");
      result.append(g.isStatic() ? "getstatic " : "getfield ");
      result.append(fieldName);
      if (!g.isStatic()) {
        result.append(" ");
        result.append(g.getUse(0));
      }
      return result.toString();
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
      Predicate<T> filter = o -> supergraph.getProcOf(o).equals(proc);
      Graph<T> localGraph = GraphSlicer.prune(supergraph, filter);

      // spawn the viewer
      System.err.println("Spawn Viewer for " + proc);
      DotUtil.dotify(localGraph, labels, dotFile, pdfFile, dotExe);

      if (DotUtil.getOutputType() == DotUtil.DotOutputType.PDF) {
        PDFViewUtil.launchPDFView(pdfFile, pdfViewExe);
      }
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
    return pdfViewExe;
  }

  protected String getPsFile() {
    return pdfFile;
  }
}
