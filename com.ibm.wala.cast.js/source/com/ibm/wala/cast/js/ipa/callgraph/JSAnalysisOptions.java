package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;

/**
 * call graph construction options specific to JavaScript.
 */
public class JSAnalysisOptions extends AnalysisOptions {

  /**
   * should the analysis model the semantics of Function.prototype.call / apply?
   * Defaults to true.
   */
  private boolean handleCallApply = true;

  public JSAnalysisOptions(AnalysisScope scope, Iterable<? extends Entrypoint> e) {
    super(scope, e);
  }

  /**
   * should the analysis model the semantics of Function.prototype.call / apply?
   */
  public boolean handleCallApply() {
    return handleCallApply;
  }

  public void setHandleCallApply(boolean handleCallApply) {
    this.handleCallApply = handleCallApply;
  }

}
