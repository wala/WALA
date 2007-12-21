package com.ibm.wala.cast.java.ipa.slicer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.traverse.DFS;

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
    return new AstJavaSlicer().computeSlice(sdg, ss, cg, pa, new AstJavaModRef(), dOptions, cOptions, backward);
  }

  public static Set<Statement> gatherAssertions(CallGraph CG, Collection<CGNode> partialRoots) {
    Set<Statement> result = new HashSet<Statement>();
    for (Iterator<CGNode> ns = DFS.getReachableNodes(CG, partialRoots).iterator(); ns.hasNext();) {
      CGNode n = ns.next();
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

  public static Pair<Collection<Statement>, SDG> computeAssertionSlice(CallGraph CG, PointerAnalysis pa, Collection<CGNode> partialRoots) throws IllegalArgumentException, CancelException {
    CallGraph pcg = PartialCallGraph.make(CG, new LinkedHashSet<CGNode>(partialRoots));
    SDG sdg = new SDG(pcg, pa, new AstJavaModRef(), DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
    Trace.println("SDG:\n" + sdg);
    return Pair.make(AstJavaSlicer.computeBackwardSlice(sdg, gatherAssertions(CG, partialRoots), pcg, pa,
        DataDependenceOptions.FULL, ControlDependenceOptions.FULL), sdg);
  }

}
