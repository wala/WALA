/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.vis;

import com.ibm.wala.core.viz.viewer.PaPanel;
import com.ibm.wala.core.viz.viewer.WalaViewer;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;

public class JsViewer extends WalaViewer {

  private static final long serialVersionUID = 1L;

  public JsViewer(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
    super(cg, pa);
  }

  @Override
  protected PaPanel createPaPanel(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
    return new JsPaPanel(cg, pa);
  }
}
