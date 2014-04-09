package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;

/**
 * call graph construction options specific to JavaScript.
 */
public class JSAnalysisOptions extends AnalysisOptions {

  /**
   * should the analysis model the semantics of Function.prototype.call / apply?
   * Defaults to true.
   */
  private boolean handleCallApply = true;
  
  private boolean useLoadFileTargetSelector = true;

  /**
   * should the analysis employ additional context sensitivity for more precise handling of lexical accesses?  if true, then:
   * 
   * <ol>
   * <li>Employ a {@link CallerSiteContext} for calls to constructors of functions that may perform a lexical access.</li>  
   * <li>Employ a {@link CallerSiteContext} for calls to functions that may perform a lexical access.</li> 
   * </ol>
   * 
   * The above helps to avoid conflation of lexical variables associated with distinct {@link CGNode}s.  
   */
  private boolean usePreciseLexical = false;
  
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

  public boolean useLoadFileTargetSelector() {
    return useLoadFileTargetSelector;
  }

  public void setUseLoadFileTargetSelector(boolean useIt) {
    this.useLoadFileTargetSelector = useIt;
  }

  public boolean usePreciseLexical() {
    return usePreciseLexical;
  }

  public void setUsePreciseLexical(boolean usePreciseLexical) {
    this.usePreciseLexical = usePreciseLexical;
  }
}
