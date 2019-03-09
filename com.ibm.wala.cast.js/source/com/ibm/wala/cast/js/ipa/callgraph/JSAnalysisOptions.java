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
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;

/** call graph construction options specific to JavaScript. */
public class JSAnalysisOptions extends AnalysisOptions {

  /**
   * should the analysis model the semantics of Function.prototype.call / apply? Defaults to true.
   */
  private boolean handleCallApply = true;

  private boolean useLoadFileTargetSelector = true;

  public JSAnalysisOptions(AnalysisScope scope, Iterable<? extends Entrypoint> e) {
    super(scope, e);
  }

  /** should the analysis model the semantics of Function.prototype.call / apply? */
  public boolean handleCallApply() {
    return handleCallApply;
  }

  public void setHandleCallApply(boolean handleCallApply) {
    this.handleCallApply = handleCallApply;
  }

  public boolean useLoadFileTargetSelector() {
    return useLoadFileTargetSelector;
  }

  public void setUseLoadFileTargetSelector(boolean useIt) {
    this.useLoadFileTargetSelector = useIt;
  }
}
