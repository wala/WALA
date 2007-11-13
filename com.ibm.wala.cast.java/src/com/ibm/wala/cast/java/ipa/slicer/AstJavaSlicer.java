package com.ibm.wala.cast.java.ipa.slicer;

import java.util.*;

import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cast.java.ipa.slicer.AstJavaSlicer;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.*;
import com.ibm.wala.util.graph.traverse.*;

public class AstJavaSlicer extends Slicer {

  /*
   * Use the passed-in SDG
   */
  public static Collection<Statement> computeBackwardSlice(SDG sdg, Collection<Statement> ss, CallGraph cg, PointerAnalysis pa,
      DataDependenceOptions dOptions, ControlDependenceOptions cOptions) throws IllegalArgumentException, CancelException {
    return computeSlice(sdg, ss, cg, pa, dOptions, cOptions, true);
  }

  /**
   * @param ss
   *            a collection of statements of interest
   * @throws CancelException
   */
  public static Collection<Statement> computeSlice(SDG sdg, Collection<Statement> ss, CallGraph cg, PointerAnalysis pa,
      DataDependenceOptions dOptions, ControlDependenceOptions cOptions, boolean backward) throws CancelException {
    return computeSlice(sdg, ss, cg, pa, new AstJavaModRef(), dOptions, cOptions, backward);
  }

  public static Set gatherAssertions(CallGraph CG, Collection partialRoots) {
    Set result = new HashSet();
    for (Iterator ns = DFS.getReachableNodes(CG, partialRoots).iterator(); ns.hasNext();) {
      CGNode n = (CGNode) ns.next();
      IR nir = n.getIR();
      SSAInstruction insts[] = nir.getInstructions();
      for (int i = 0; i < insts.length; i++) {
        if (insts[i] instanceof AstAssertInstruction) {
          result.add(new NormalStatement(n, i));
        }
      }
    }

    return result;
  }

  public static Pair<Collection<Statement>, SDG> computeAssertionSlice(CallGraph CG, PointerAnalysis pa, Collection partialRoots) throws IllegalArgumentException, CancelException {
    CallGraph pcg = PartialCallGraph.make(CG, new LinkedHashSet(partialRoots));
    SDG sdg = new SDG(pcg, pa, new AstJavaModRef(), DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
    Trace.println("SDG:\n" + sdg);
    return Pair.make(AstJavaSlicer.computeBackwardSlice(sdg, gatherAssertions(CG, partialRoots), pcg, pa,
        DataDependenceOptions.FULL, ControlDependenceOptions.FULL), sdg);
  }

}
