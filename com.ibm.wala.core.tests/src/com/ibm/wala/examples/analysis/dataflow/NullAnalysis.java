
package com.ibm.wala.examples.analysis.dataflow;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.cfg.Util;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IFlowFunctionMap;
import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationProblem;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.dataflow.IFDS.TabulationSolver;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * IFDS-based analysis for detecting null-pointer dereferences. The analysis runs in
 * the forward direction, tracking access paths that either may be null (if
 * some statement may have written null to the location) or may-not be null
 * (some statement wrote a non-null value into the location; if the may-not be
 * null fact is unreachable, then the variable must be null). A null-pointer
 * error is reported when a variable that may (or must) be null is
 * de-referenced.
 * 
 * @author Andrei Dan
 *
 */
public class NullAnalysis {
  /**
   * Maximum length of the Access Paths
   */
  protected final static int AP_LENGTH_BOUND = 2;

  /**
   * The id of the ZERO fact
   */
  public static int ZERO;

  /**
   * Propagate the analysis to library calls. If false, then the analysis will
   * assume that library calls return non null values.
   */
  protected final static boolean ANALYZE_LIBRARY = false;

  /**
   * Value number for the auxiliary variable used to collect facts about the
   * returned value of a function
   */
  public static final int AUX_RETURN_VAL_NUM = Integer.MAX_VALUE;

  /**
   * The super-graph over which tabulation is performed
   */
  protected final ICFGSupergraph supergraph;

  private TabulationSolver<BasicBlockInContext<IExplodedBasicBlock>, CGNode, NullAnalysisFact> solver;

  /**
   * The tabulation domain
   */
  protected final NullAnalysisDomain domain;

  /**
   * Returns the domain of this analysis
   * 
   * @return domain
   */
  public NullAnalysisDomain getDomain() {
    return domain;
  }

  /**
   * Returns the supergraph of the analyzed program
   * 
   * @return supergraph
   */
  public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
    return supergraph;
  }

  /**
   * Constructor of the MayNULL analysis
   * 
   * @param cg
   * @param cache
   */
  public NullAnalysis(CallGraph cg, AnalysisCache cache) {

    // we use an ICFGSupergraph, which basically adapts
    // ExplodedInterproceduralCFG to the ISupergraph interface
    this.supergraph = ICFGSupergraph.make(cg, cache);
    domain = new NullAnalysisDomain();

    // the ZERO fact
    ZERO = domain.add(new NullAnalysisFact(null, null, null));
  }

  /**
   * Returns the symbol table associated with the given node
   * 
   * @param n
   * @return symbol table
   */
  public SymbolTable getSymbolTable(BasicBlockInContext<IExplodedBasicBlock> n) {
    return n.getNode().getIR().getSymbolTable();
  }

  /**
   * Returns the SSA instruction which generated the given node
   * 
   * @param n
   * @return SSA instruction
   */
  public SSAInstruction getSSAInstr(BasicBlockInContext<IExplodedBasicBlock> n) {
    return n.getDelegate().getInstruction();
  }

  /**
   * Controls numbering of MayNULL and MayNotNULL facts for use in tabulation
   */
  @SuppressWarnings("serial")
  public class NullAnalysisDomain extends MutableMapping<NullAnalysisFact>
      implements TabulationDomain<NullAnalysisFact, BasicBlockInContext<IExplodedBasicBlock>> {

    @Override
    public boolean hasPriorityOver(PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p1,
        PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p2) {
      // don't worry about work-list priorities
      return false;
    }
  }

  /**
   * Returns a flow function that is a subset of the given function. The missing
   * (key, value) pairs are filtered, depending of the source and destination
   * nodes, according to the filteredEdges rules
   * 
   * @param func
   * @param src
   * @param dest
   * @return
   */
  protected IUnaryFlowFunction filter(IUnaryFlowFunction func, BasicBlockInContext<IExplodedBasicBlock> src,
      BasicBlockInContext<IExplodedBasicBlock> dest) {

    return func;
  }

  protected class NullAnalysisFlowFunctions implements IFlowFunctionMap<BasicBlockInContext<IExplodedBasicBlock>> {

    /**
     * The domain used for the MayNULL analysis
     */
    private final NullAnalysisDomain domain;

    protected NullAnalysisFlowFunctions(NullAnalysisDomain domain) {
      this.domain = domain;
    }

    /**
     * Adds to the entry Facts set mayNULL or mayNotNULL facts about constant
     * value numbers from the symbol table of the given entry block
     * 
     * @param entryBlk
     * @param entryFacts
     */
    private void getEntryBlkFacts(BasicBlockInContext<IExplodedBasicBlock> entryBlk, MutableIntSet entryFacts) {

      SymbolTable st = getSymbolTable(entryBlk);

      int maxSt = st.getMaxValueNumber();

      // for each entry of the symbol table
      for (int i = 1; i <= maxSt; i++) {
        if (st.isConstant(i) && !st.isNullConstant(i)) {
          // if we have a constant different than NULL, add MayNotNULL
          // fact about this value number
          NullAnalysisFact mayNotNULLFact = new NullAnalysisFact(entryBlk.getNode(), i, NullSet.MayNotNULL);
          int factIdx = domain.add(mayNotNULLFact);
          entryFacts.add(factIdx);
        }

        if (st.isNullConstant(i)) {
          // if we have a NULL constant, add a MayNULL fact about this
          // value number

          NullAnalysisFact mayNULLFact = new NullAnalysisFact(entryBlk.getNode(), i, NullSet.MayNULL);
          int factIdx = domain.add(mayNULLFact);
          entryFacts.add(factIdx);
        }
      }
    }

    /**
     * Compose two flow functions.
     * 
     * @param f1
     * @param f2
     * @return result(x) = f2(f1(x))
     */
    private IUnaryFlowFunction compose(final IUnaryFlowFunction f1, final IUnaryFlowFunction f2) {
      return new IUnaryFlowFunction() {
        @Override
        public IntSet getTargets(int d1) {
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
          IntSet imF1 = f1.getTargets(d1);

          IntIterator iterator = imF1.intIterator();

          while (iterator.hasNext()) {
            int f1 = iterator.next();
            result.addAll(f2.getTargets(f1));
          }
          return result;
        }
      };
    }

    /**
     * Returns true if we can propagate the given fact after the given PI
     * instruction. Used to stop propagating facts that contradict the condition
     * associated with the PI instruction.
     * 
     * @param piInstr
     * @param src
     * @param dest
     * @param factType
     * @return
     */
    private boolean canPropagate(SSAPiInstruction piInstr, BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, NullSet factType) {

      SSAConditionalBranchInstruction branchInstr = (SSAConditionalBranchInstruction) piInstr.getCause();

      boolean isUnaryCond = true;

      boolean isEq = branchInstr.getOperator().equals(Operator.EQ);
      boolean isNe = branchInstr.getOperator().equals(Operator.NE);
      boolean isTrueBranch = dest.getNumber() == Util.getTakenSuccessor(supergraph.getCFG(src), src.getDelegate()).getNumber();
      boolean isFalseBranch = dest.getNumber() == Util.getNotTakenSuccessor(supergraph.getCFG(src), src.getDelegate()).getNumber();

      if (!isTrueBranch && !isFalseBranch) {
        throw new RuntimeException("not true nor false branch");
      }

      int lhs = branchInstr.getUse(0);
      int rhs = branchInstr.getUse(1);
      SymbolTable st = getSymbolTable(src);

      if (st.isNullConstant(lhs) && st.isNullConstant(rhs)) {
        isUnaryCond = false;
      }

      if (piInstr.getSuccessor() != dest.getDelegate().getOriginalNumber()) {
        return false;
      }

      if ((isEq && isTrueBranch && isUnaryCond) || (isNe && isFalseBranch && isUnaryCond)) {
        // variable is equal to null => don't propagate may not be null

        if (factType.equals(NullSet.MayNotNULL)) {
          return false;
        } else {
          return true;
        }
      }

      if ((isNe && isTrueBranch && isUnaryCond) || (isEq && isFalseBranch && isUnaryCond)) {
        // variable is not equal to null => don't propagate may be null

        if (factType.equals(NullSet.MayNULL)) {
          return false;
        } else {
          return true;
        }
      }

      // return true if we do not have to filter the given fact
      return true;
    }

    /**
     * Returns the PI flow function between the given source and destination
     * nodes. Checks which facts to propagate, such that the condition is not
     * contradicted on the true/false branch
     * 
     * @param src
     * @param dest
     * @return
     */
    private IUnaryFlowFunction buildPiFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        final BasicBlockInContext<IExplodedBasicBlock> dest) {

      return new IUnaryFlowFunction() {

        @Override
        public IntSet getTargets(int d1) {
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();

          if (d1 == ZERO) {
            result.add(ZERO);
            return result;
          }

          NullAnalysisFact fact = domain.getMappedObject(d1);
          Iterator<SSAPiInstruction> piIter = src.iteratePis();

          result.add(d1);

          while (piIter.hasNext()) {
            SSAPiInstruction piInstr = piIter.next();

            // if it is a fact about piUseValueNum
            if (fact.node.equals(src.getNode()) && fact.ssaVarId == piInstr.getUse(0)) {

              if (canPropagate(piInstr, src, dest, fact.type)) {
                // propagate facts from piUseValueNum to
                // piDefValueNum
                int newFactId = domain.add(new NullAnalysisFact(src.getNode(), piInstr.getDef(), fact.accessPath, fact.type));
                result.add(newFactId);

              }
            }
          }
          return result;
        }
      };
    }

    /**
     * Returns the PHI flow function for the given destination node. It copies
     * the facts that hold for the PHI use value numbers to facts that hold for
     * the PHI defined value number.
     * 
     * @param dest
     * @return
     */
    private IUnaryFlowFunction buildPhiFunction(final BasicBlockInContext<IExplodedBasicBlock> dest) {
      return new IUnaryFlowFunction() {

        @Override
        public IntSet getTargets(int d1) {
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();

          if (d1 == ZERO) {
            result.add(ZERO);
            return result;
          }

          result.add(d1);

          NullAnalysisFact fact = domain.getMappedObject(d1);
          if (fact.node.equals(dest.getNode())) {

            Iterator<SSAPhiInstruction> phis = dest.iteratePhis();
            while (phis.hasNext()) {
              SSAPhiInstruction phi = phis.next();
              int phiDef = phi.getDef();

              for (int i = 0; i < phi.getNumberOfUses(); i++) {
                if (fact.ssaVarId == phi.getUse(i)) {
                  // propagate fact about PHI use to PHI
                  // defined

                  int newFactId = domain.add(new NullAnalysisFact(fact.node, phiDef, fact.accessPath, fact.type));
                  result.add(newFactId);

                }
              }
            }
          }
          return result;
        }
      };
    }

    /**
     * Propagate facts that hold for the returned value number to the
     * AUX_RETURN_VAL_NUM auxiliary value number
     * 
     * @param src
     * @param dest
     * @param entryFacts
     * @return
     */
    private IUnaryFlowFunction buildReturnFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, final MutableIntSet entryFacts) {

      SSAInstruction instr = getSSAInstr(src);
      final SSAReturnInstruction retInstr = (SSAReturnInstruction) instr;

      return new IUnaryFlowFunction() {

        @Override
        public IntSet getTargets(int d1) {

          if (d1 == ZERO) {
            entryFacts.add(ZERO);
            return entryFacts;
          }

          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
          result.add(d1);

          NullAnalysisFact fact = domain.getMappedObject(d1);

          for (int i = 0; i < retInstr.getNumberOfUses(); i++) {

            // if we have a fact about the returned value number
            if (fact.node.equals(src.getNode()) && fact.ssaVarId == retInstr.getUse(i)) {

              // the same fact holds for the auxiliary return
              // value number
              NullAnalysisFact auxReturnFact = new NullAnalysisFact(src.getNode(), AUX_RETURN_VAL_NUM, fact.accessPath, fact.type);

              int auxReturnFactId = domain.add(auxReturnFact);
              // propagate the fact for the auxiliary return
              // value
              result.add(auxReturnFactId);
            }
          }
          return result;
        }
      };
    }

    /**
     * Generate MayNotNULL facts about the defined value of NEW or GET (the
     * heap) instructions. This models the assumption that any value read from
     * the heap is not NULL.
     * 
     * @param src
     * @param dest
     * @param entryFacts
     * @return
     */
    private IUnaryFlowFunction buildNewFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, final MutableIntSet entryFacts) {

      final SSAInstruction instr = getSSAInstr(src);
      NullAnalysisFact newMayNonNull = new NullAnalysisFact(src.getNode(), instr.getDef(), NullSet.MayNotNULL);

      final int newMayNotNullId = domain.add(newMayNonNull);

      return new IUnaryFlowFunction() {
        @Override
        public IntSet getTargets(int d1) {
          if (d1 == ZERO) {
            // add ZERO and the MayNotNull set
            entryFacts.add(ZERO);
            entryFacts.add(newMayNotNullId);
            return entryFacts;
          }
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();

          // remove the MayNull facts about the value number
          NullAnalysisFact elem = domain.getMappedObject(d1);
          if (!elem.node.equals(src.getNode()) || !(elem.ssaVarId == instr.getDef()) || !elem.type.equals(NullSet.MayNULL)) {

            // identity function
            result.add(d1);
          }
          return result;
        }
      };
    }

    private IUnaryFlowFunction buildGetFieldFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, final MutableIntSet entryFacts) {
      SSAInstruction instr = getSSAInstr(src);
      SSAGetInstruction getInstr = (SSAGetInstruction) instr;
      final int lhs = getInstr.getDef();
      final int rhs = getInstr.getUse(0);
      final String field = getInstr.getDeclaredField().getName().toString();

      return new IUnaryFlowFunction() {
        @Override
        public IntSet getTargets(int d1) {
          if (d1 == ZERO) {
            // add ZERO and the MayNotNull set
            entryFacts.add(ZERO);

            return entryFacts;
          }
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
          result.add(d1);

          NullAnalysisFact elem = domain.getMappedObject(d1);
          if (elem.node.equals(src.getNode())) {
            if (elem.ssaVarId == rhs) {
              if (!elem.accessPath.isEmpty()) {
                if (elem.accessPath.get(0).equals(field)) {
                  // remove first element of the access path
                  List<String> newAccessPath = NullAnalysisFact.getEmptyAccessPath();
                  for (int i = 1; i < elem.accessPath.size(); i++) {
                    newAccessPath.add(elem.accessPath.get(i));
                  }
                  NullAnalysisFact newFact = new NullAnalysisFact(src.getNode(), lhs, newAccessPath, elem.type);
                  int newFactId = domain.add(newFact);
                  result.add(newFactId);
                }
              }
            }
          }
          return result;
        }
      };
    }

    private IUnaryFlowFunction buildPutFieldFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, final MutableIntSet entryFacts) {
      SSAInstruction instr = getSSAInstr(src);
      SSAPutInstruction putInstr = (SSAPutInstruction) instr;
      final int lhs = putInstr.getUse(0);
      final int rhs = putInstr.getUse(1);
      final String field = putInstr.getDeclaredField().getName().toString();

      return new IUnaryFlowFunction() {
        @Override
        public IntSet getTargets(int d1) {
          if (d1 == ZERO) {
            // add ZERO and the MayNotNull set
            entryFacts.add(ZERO);

            return entryFacts;
          }
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
          result.add(d1);

          NullAnalysisFact elem = domain.getMappedObject(d1);
          if (elem.node.equals(src.getNode())) {
            if (elem.ssaVarId == rhs) {
              if (elem.accessPath.size() < AP_LENGTH_BOUND) {
                // add one element to the access path
                List<String> newAccessPath = NullAnalysisFact.getEmptyAccessPath();
                newAccessPath.add(field);
                for (String ap : elem.accessPath) {
                  newAccessPath.add(ap);
                }
                NullAnalysisFact newFact = new NullAnalysisFact(src.getNode(), lhs, newAccessPath, elem.type);
                int newFactId = domain.add(newFact);
                result.add(newFactId);
              }
            }
          }
          return result;
        }
      };
    }

    private IUnaryFlowFunction buildCheckCastFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, final MutableIntSet entryFacts) {
      SSAInstruction instr = getSSAInstr(src);
      SSACheckCastInstruction checkCastInstr = (SSACheckCastInstruction) instr;
      final int lhs = checkCastInstr.getDef();
      final int rhs = checkCastInstr.getUse(0);

      return new IUnaryFlowFunction() {
        @Override
        public IntSet getTargets(int d1) {
          if (d1 == ZERO) {
            // add ZERO and the MayNotNull set
            entryFacts.add(ZERO);
            return entryFacts;
          }
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
          result.add(d1);

          NullAnalysisFact elem = domain.getMappedObject(d1);
          if (elem.node.equals(src.getNode())) {
            if (elem.ssaVarId == rhs) {

              NullAnalysisFact newFact = new NullAnalysisFact(src.getNode(), lhs, elem.accessPath, elem.type);
              int newFactId = domain.add(newFact);
              result.add(newFactId);
            }
          }
          return result;
        }
      };
    }

    @Override
    public IUnaryFlowFunction getNormalFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {

      final MutableIntSet entryFacts = MutableSparseIntSet.makeEmpty();
      IUnaryFlowFunction blockFunc;

      if (src.isEntryBlock()) {
        getEntryBlkFacts(src, entryFacts);
      }

      SSAInstruction instr = getSSAInstr(src);

      IUnaryFlowFunction piFunc = buildPiFunction(src, dest);
      IUnaryFlowFunction phiFunc = buildPhiFunction(dest);

      if (instr instanceof SSACheckCastInstruction) {
        // e.g. v1 = (Type)v2
        blockFunc = buildCheckCastFunction(src, dest, entryFacts);
      } else if (instr instanceof SSAGetInstruction && !((SSAGetInstruction) instr).isStatic()) {
        // e.g. v1 = v2.next
        blockFunc = buildGetFieldFunction(src, dest, entryFacts);
      } else if (instr instanceof SSAPutInstruction && !((SSAPutInstruction) instr).isStatic()) {
        // e.g. v1.next = v2
        blockFunc = buildPutFieldFunction(src, dest, entryFacts);
      } else if (instr instanceof SSAReturnInstruction) {
        // e.g. return v1
        blockFunc = buildReturnFunction(src, dest, entryFacts);

      } else if (instr instanceof SSANewInstruction
          || (instr instanceof SSAGetInstruction && ((SSAGetInstruction) instr).isStatic())) {
        // the defined value of NEW or GET (the heap) instructions
        // number may be not null
        // e.g. v1 = new ...
        // e.g. v1 = Class.field
        blockFunc = buildNewFunction(src, dest, entryFacts);

      } else {
        // default normal flow function
        blockFunc = new IUnaryFlowFunction() {
          @Override
          public IntSet getTargets(int d1) {
            if (d1 == ZERO) {
              entryFacts.add(ZERO);
              return entryFacts;
            }
            MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
            result.add(d1);
            return result;
          }
        };
      }

      // return blockFunction;
      IUnaryFlowFunction result = compose(compose(piFunc, blockFunc), phiFunc);

      return filter(result, src, dest);
    }

    @Override
    public IUnaryFlowFunction getCallFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        final BasicBlockInContext<IExplodedBasicBlock> dest, BasicBlockInContext<IExplodedBasicBlock> ret) {

      SSAInstruction ssaInstr = getSSAInstr(src);
      final SSAInvokeInstruction invokeInstr = (SSAInvokeInstruction) ssaInstr;

      IUnaryFlowFunction callFunc = new IUnaryFlowFunction() {
        @Override
        public IntSet getTargets(int d1) {

          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();

          boolean isInLibrary = !isInAppScope(dest.getNode());

          if (isInLibrary && !ANALYZE_LIBRARY) {
            // do not analyse the library code
            return result;
          }

          if (d1 == ZERO) {
            result.add(ZERO);
            if (invokeInstr.getCallSite().isVirtual()) {
              // it it's a virtual call, then propagate that the
              // first parameter (this) is not null
              int thisMayNotNullId = domain.add(new NullAnalysisFact(dest.getNode(), 1, NullSet.MayNotNULL));
              result.add(thisMayNotNullId);
            }

            return result;
          }
          // incoming fact
          NullAnalysisFact fact = domain.getMappedObject(d1);
          int numParams = invokeInstr.getNumberOfParameters();

          for (int i = 0; i < numParams; i++) {
            // if variable in incoming fact used as argument
            if (invokeInstr.getUse(i) == fact.ssaVarId) {
              // it it's a virtual call, then don't propagate that
              // the first parameter (this) may be null
              if (!invokeInstr.getCallSite().isStatic() && i == 0 && fact.type.equals(NullSet.MayNULL)
                  && fact.accessPath.isEmpty()) {
              } else {
                // propagate fact of same type to callee formal
                // parameter
                NullAnalysisFact newFact = new NullAnalysisFact(dest.getNode(), (i + 1), fact.accessPath, fact.type);
                int formalParamFactId = domain.add(newFact);
                result.add(formalParamFactId);
              }
            }
          }
          return result;
        }
      };
      return filter(callFunc, src, dest);
    }

    @Override
    public IFlowFunction getReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> call,
        final BasicBlockInContext<IExplodedBasicBlock> src, final BasicBlockInContext<IExplodedBasicBlock> dest) {

      SSAInstruction ssaInstr = getSSAInstr(call);
      final SSAInvokeInstruction ssaInvoke = (SSAInvokeInstruction) ssaInstr;

      final int calleeValueNum = ssaInvoke.getDef(0);

      IUnaryFlowFunction retFunc = new IUnaryFlowFunction() {
        // if there is a return value
        @Override
        public IntSet getTargets(int d1) {
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();

          if (d1 == ZERO) {
            result.add(ZERO);
            return result;
          }
          NullAnalysisFact fact = domain.getMappedObject(d1);

          // if the fact corresponds to an auxiliary returned value
          // number
          if (fact.node.equals(src.getNode()) && fact.ssaVarId == AUX_RETURN_VAL_NUM && ssaInvoke.getNumberOfDefs() > 1) {

            // propagate this type of fact to the value numbers in
            // the callee
            NullAnalysisFact newFact = new NullAnalysisFact(dest.getNode(), calleeValueNum, fact.accessPath, fact.type);
            int newFactID = domain.add(newFact);
            result.add(newFactID);
          }
          // if the fact corresponds to "this"
          if (fact.node.equals(src.getNode()) && fact.ssaVarId == 1) {

            // propagate the fact to variable in caller that
            // corresponds to "this" in callee
            NullAnalysisFact newFact = new NullAnalysisFact(dest.getNode(), ssaInvoke.getUse(0), fact.accessPath, fact.type);
            int newFactID = domain.add(newFact);

            result.add(newFactID);
          }

          return result;
        }
      };
      return filter(retFunc, src, dest);
    }

    @Override
    public IUnaryFlowFunction getCallToReturnFlowFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {

      SSAInstruction instr = getSSAInstr(src);
      final SSAInvokeInstruction invokeInstr = (SSAInvokeInstruction) instr;

      Iterator<? extends BasicBlockInContext<IExplodedBasicBlock>> calledNodes = getSupergraph().getCalledNodes(src);
      boolean isLibraryCall = true;

      while (calledNodes.hasNext()) {
        // check if all possible calledNodes are library calls
        BasicBlockInContext<IExplodedBasicBlock> calledNode = calledNodes.next();
        boolean aux = !isInAppScope(getSupergraph().getProcOf(calledNode));
        isLibraryCall = isLibraryCall && aux;
      }

      // set of facts that def variables that may not be null because they
      // are assigned the return value of a library call
      final MutableIntSet defMayNotNull = MutableSparseIntSet.makeEmpty();

      if (isLibraryCall && !ANALYZE_LIBRARY) {
        if (invokeInstr.getNumberOfDefs() > 0) {
          // if it is a library call and we do not analyze libraries,
          // then result of the call is MayNotNULL

          int defMayNotNullFactId = domain.add(new NullAnalysisFact(src.getNode(), invokeInstr.getDef(0), NullSet.MayNotNULL));
          defMayNotNull.add(defMayNotNullFactId);
        }
      }

      if (invokeInstr.getCallSite().isVirtual()) {

        // if the call is to a virtual method
        IUnaryFlowFunction callToRetFunc = new IUnaryFlowFunction() {
          @Override
          public IntSet getTargets(int d1) {

            int derefValueNum = invokeInstr.getUse(0);

            if (d1 == ZERO) {
              // the object on which the method is called is
              // MayNotNULL
              int mayNotNullFactId = domain.add(new NullAnalysisFact(src.getNode(), derefValueNum, NullSet.MayNotNULL));

              defMayNotNull.add(mayNotNullFactId);
              defMayNotNull.add(ZERO);
              return defMayNotNull;
            }
            MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();

            NullAnalysisFact fact = domain.getMappedObject(d1);

            // the object on which the method is called is not
            // MayNULL
            if (!fact.node.equals(src.getNode()) || !(fact.ssaVarId == derefValueNum) || !fact.type.equals(NullSet.MayNULL)) {
              result.add(d1);
            }
            return result;
          }
        };
        return filter(callToRetFunc, src, dest);
      }

      IUnaryFlowFunction callToRetFunc = new IUnaryFlowFunction() {
        @Override
        public IntSet getTargets(int d1) {

          if (d1 == ZERO) {
            defMayNotNull.add(ZERO);
            return defMayNotNull;
          }
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
          result.add(d1);
          return result;
        }
      };
      return filter(callToRetFunc, src, dest);
    }

    @Override
    public IUnaryFlowFunction getCallNoneToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {

      // I think this is the case of unreachable calls
      return getCallToReturnFlowFunction(src, dest);
    }
  }

  protected class NullAnalysisProblem
      implements TabulationProblem<BasicBlockInContext<IExplodedBasicBlock>, CGNode, NullAnalysisFact> {

    private NullAnalysisFlowFunctions flowFunctions = new NullAnalysisFlowFunctions(domain);

    @Override
    public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
      return supergraph;
    }

    @Override
    public Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds() {
      Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> result = HashSetFactory.make();

      for (BasicBlockInContext<IExplodedBasicBlock> bb : supergraph) {
        result.add(PathEdge.createPathEdge(bb, 0, bb, 0));
        break;
      }
      return result;
    }

    @Override
    public IMergeFunction getMergeFunction() {
      return null;
    }

    @Override
    public IFlowFunctionMap<BasicBlockInContext<IExplodedBasicBlock>> getFunctionMap() {
      return flowFunctions;
    }

    @Override
    public TabulationDomain<NullAnalysisFact, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
      return domain;
    }
  }

  public TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, NullAnalysisFact> analyze() {

    solver = TabulationSolver.make(new NullAnalysisProblem());
    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, NullAnalysisFact> analysisResult = null;

    try {
      analysisResult = solver.solve();
    } catch (CancelException e) {
      // this shouldn't happen
      assert false;
    }
    return analysisResult;
  }

  /**
   * Returns true if the given node is in the application, and not in the
   * library
   * 
   * @param node
   * @return
   */
  protected boolean isInAppScope(CGNode node) {
    return isInAppScope(node.getMethod());
  }

  protected boolean isInAppScope(IMethod method) {
    return !method.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Primordial);
  }

  protected boolean isInAppScope(BasicBlockInContext<IExplodedBasicBlock> node) {
    return isInAppScope(node.getMethod());
  }

}
