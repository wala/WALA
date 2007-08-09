package com.ibm.wala.cast.java.ipa.slicer;

import java.util.Collection;

import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;

public class AstJavaSlicer extends Slicer {

 /*
   * Use the passed-in SDG
   */
  public static Collection<Statement> computeBackwardSlice(SDG sdg, Collection<Statement> ss, CallGraph cg, PointerAnalysis pa,
      DataDependenceOptions dOptions, ControlDependenceOptions cOptions) throws IllegalArgumentException {
    return computeSlice(sdg, ss, cg, pa, dOptions, cOptions, true);
  }

  /**
   * @param ss
   *          a collection of statements of interest
   */
  protected static Collection<Statement> computeSlice(SDG sdg, Collection<Statement> ss, CallGraph cg, PointerAnalysis pa,
      DataDependenceOptions dOptions, ControlDependenceOptions cOptions, boolean backward) {
    return computeSlice(sdg, ss, cg, pa, new AstJavaModRef(), dOptions, cOptions, backward);
  }

}
