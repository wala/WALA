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

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.viz.DotUtil.DotOutputType;

/**
 * Explore the result of an IFDS problem with an SWT viewer and ghostview.
 * 
 * @author Stephen Fink
 */
public class IFDSExplorer {

  /**
   * absolute path name to invoke dot
   */
  protected static String dotExe = null;

  /**
   * Absolute path name to invoke ghostview
   */
  protected static String gvExe = null;

  public static void setDotExe(String newDotExe) {
    dotExe = newDotExe;
  }

  public static void setGvExe(String newGvExe) {
    gvExe = newGvExe;
  }

  public static <T, P> void viewIFDS(TabulationResult<T, P> r, Collection<? extends P> roots) throws WalaException {
    viewIFDS(r, roots, null);
  }

  public static <T, P> void viewIFDS(TabulationResult<T, P> r, Collection<? extends P> roots, NodeDecorator labels)
      throws WalaException {
    if (r == null) {
      throw new IllegalArgumentException("r is null");
    }
    assert gvExe != null;
    assert dotExe != null;

    // dump the domain to stderr
    System.err.println("Domain:\n" + r.getProblem().getDomain().toString());

    Properties p = null;
    try {
      p = WalaProperties.loadProperties();
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    String outputFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar
        + (DotUtil.getOutputType() == DotOutputType.PS ? "ir.ps" : "ir.svg");
    String dotFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + "ir.dt";

    final SWTTreeViewer v = new SWTTreeViewer();
    Graph<? extends P> g = r.getProblem().getSupergraph().getProcedureGraph();
    v.setGraphInput(g);
    v.setBlockInput(true);
    v.setRootsInput(roots);
    ViewIFDSLocalAction<T, P> action = (labels == null ? new ViewIFDSLocalAction<T, P>(v, r, outputFile, dotFile, dotExe, gvExe) : new ViewIFDSLocalAction<T, P>(v, r, outputFile, dotFile, dotExe, gvExe, labels));
    v.getPopUpActions().add(action);
    v.run();

  }

  /**
   * Calls {@link #viewIFDS(TabulationResult)} with roots computed by {@link InferGraphRoots}.
   */
  public static <T, P> void viewIFDS(TabulationResult<T, P> r) throws WalaException {
    Collection<? extends P> roots = InferGraphRoots.inferRoots(r.getProblem().getSupergraph().getProcedureGraph());
    viewIFDS(r, roots);

  }

  public static String getDotExe() {
    return dotExe;
  }

  public static String getGvExe() {
    return gvExe;
  }

}
