package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;

public class ReflectiveCallVertex extends Vertex {

  // method containing the call
  private final FuncVertex caller;

  // PC of the call site
  private final CallSiteReference site;

  // the call instruction itself
  private final JavaScriptInvoke invk;

  public ReflectiveCallVertex(FuncVertex caller, CallSiteReference site, JavaScriptInvoke invk) {
    this.caller = caller;
    this.site = site;
    this.invk = invk;
  }

  public FuncVertex getCaller() {
    return caller;
  }

  public CallSiteReference getSite() {
    return site;
  }

  public JavaScriptInvoke getInstruction() {
    return invk;
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitReflectiveCallVertex(this);
  }

  @Override
  public String toString() {
    return "ReflectiveCallee(" + caller + ", " + site + ')';
  }

  @Override
  public String toSourceLevelString(IAnalysisCacheView cache) {
    IClass concreteType = caller.getConcreteType();
    AstMethod method = (AstMethod) concreteType.getMethod(AstMethodReference.fnSelector);
    return "ReflectiveCallee("
        + method.getSourcePosition(site.getProgramCounter()).prettyPrint()
        + ")";
  }
}
