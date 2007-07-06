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

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.warnings.WalaException;

/**
 * @author sfink
 * 
 */
public class ViewAnnotatedIRAction extends ViewIRAction {

  private final BasicBlockDecorator dec;

  public ViewAnnotatedIRAction(SWTTreeViewer viewer, CallGraph cg, String psFile, String dotFile, String dotExe, String gvExe,
      BasicBlockDecorator dec) {
    super(viewer, cg, psFile, dotFile, dotExe, gvExe);
    this.dec = dec;
  }

  @Override
  public void run() {
    IR ir = getIRForSelection();
    // spawn the viewer
    System.err.println("Spawn IR Viewer for " + ir.getMethod());
    try {
      dec.setCurrentNode(getNodeForSelection());
      GhostviewUtil.ghostviewIR(getCg().getClassHierarchy(), ir, getPsFile(), getDotFile(), getDotExe(), getGvExe(), dec);
    } catch (WalaException e) {
      e.printStackTrace();
    }
  }

}
