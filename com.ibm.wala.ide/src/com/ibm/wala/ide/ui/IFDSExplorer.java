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

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;

/**
 * Explore the result of an IFDS problem with an SWT viewer and ghostview.
 */
public class IFDSExplorer {

  /**
   * absolute path name to invoke dot
   */
  protected static String dotExe = null;

  /**
   * Absolute path name to invoke viewer
   */
  protected static String viewerExe = null;

  private static final boolean PRINT_DOMAIN = true;

  public static void setDotExe(String newDotExe) {
    dotExe = newDotExe;
  }

  public static void setGvExe(String newGvExe) {
    viewerExe = newGvExe;
  }

  public static <T, P, F> void viewIFDS(TabulationResult<T, P, F> r, Collection<? extends P> roots) throws WalaException {
    viewIFDS(r, roots, null);
  }

  public static <T, P, F> void viewIFDS(TabulationResult<T, P, F> r, Collection<? extends P> roots, NodeDecorator<T> labels)
      throws WalaException {
    Properties p = null;
    try {
      p = WalaProperties.loadProperties();
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    String scratch = p.getProperty(WalaProperties.OUTPUT_DIR);
    viewIFDS(r, roots, labels, scratch);
  }

  public static <T, P, F> void viewIFDS(TabulationResult<T, P, F> r, Collection<? extends P> roots, NodeDecorator<T> labels,
      String scratchDirectory) throws WalaException {
    if (r == null) {
      throw new IllegalArgumentException("r is null");
    }
    assert dotExe != null;

    // dump the domain to stderr
    if (PRINT_DOMAIN) {
      System.err.println("Domain:\n" + r.getProblem().getDomain().toString());
    }

    String irFileName = null;
    switch (DotUtil.getOutputType()) {
    case PDF:
      irFileName = "ir.pdf";
      break;
    case PS:
    case EPS:
      irFileName = "ir.ps";
      break;
    case SVG:
      irFileName = "ir.svg";
      break;
    }
    String outputFile = scratchDirectory + File.separatorChar + irFileName;
    String dotFile = scratchDirectory + File.separatorChar + "ir.dt";

    final SWTTreeViewer v = new SWTTreeViewer();
    Graph<? extends P> g = r.getProblem().getSupergraph().getProcedureGraph();
    v.setGraphInput(g);
    v.setBlockInput(true);
    v.setRootsInput(roots);
    ViewIFDSLocalAction<T, P, F> action = (labels == null ? new ViewIFDSLocalAction<>(v, r, outputFile, dotFile, dotExe,
        viewerExe) : new ViewIFDSLocalAction<>(v, r, outputFile, dotFile, dotExe, viewerExe, labels));
    v.getPopUpActions().add(action);
    v.run();

  }

  /**
   * Calls {@link #viewIFDS(TabulationResult)} with roots computed by {@link InferGraphRoots}.
   */
  public static <T, P, F> void viewIFDS(TabulationResult<T, P, F> r) throws WalaException {
    if (r == null) {
      throw new IllegalArgumentException("null r");
    }
    Collection<? extends P> roots = InferGraphRoots.inferRoots(r.getProblem().getSupergraph().getProcedureGraph());
    viewIFDS(r, roots);
  }

  public static String getDotExe() {
    return dotExe;
  }

  public static String getGvExe() {
    return viewerExe;
  }

}
