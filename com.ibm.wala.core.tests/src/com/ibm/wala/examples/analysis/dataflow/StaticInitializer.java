/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.examples.analysis.dataflow;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationSolver;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSet;

public class StaticInitializer {

  /**
   * used for resolving field references in putstatic instructions
   */
  private final IClassHierarchy cha;

  /**
   * the supergraph over which tabulation is performed
   */
  private final ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph;
  
  
  private final InitializerDomain domain = new InitializerDomain();
  
  private Map<BasicBlockInContext<IExplodedBasicBlock>, List<IClass>> initialized;
  
  
  public final Map<BasicBlockInContext<IExplodedBasicBlock>, List<IClass>> getInitialized() {
    return initialized;
  }

  public StaticInitializer(CallGraph cg) {
    cha = cg.getClassHierarchy();
    supergraph = ICFGSupergraph.make(cg);
  }
  
  /**
   * controls numbering of putstatic instructions for use in tabulation
   */
  private class InitializerDomain extends MutableMapping<IClass> implements
      TabulationDomain<IClass, BasicBlockInContext<IExplodedBasicBlock>> {

    private static final long serialVersionUID = -1897766113586243833L;

    @Override
    public boolean hasPriorityOver(PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p1,
        PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p2) {
      // don't worry about worklist priorities
      return false;
    }

  }
  
  private class InitializerFlowFunctions implements IPartiallyBalancedFlowFunctions<BasicBlockInContext<IExplodedBasicBlock>> {

    private final InitializerDomain domain;

    protected InitializerFlowFunctions(InitializerDomain domain) {
      this.domain = domain;
    }

    /**
     * the flow function for flow from a callee to caller where there was no flow from caller to callee; just the identity function
     * 
     * @see ReachingDefsProblem
     */
    @Override
    public IFlowFunction getUnbalancedReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      return IdentityFlowFunction.identity();
    }

    /**
     * flow function from caller to callee; just the identity function
     */
    @Override
    public IUnaryFlowFunction getCallFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, BasicBlockInContext<IExplodedBasicBlock> ret) {
      return IdentityFlowFunction.identity();
    }

    /**
     * flow function from call node to return node when there are no targets for the call site; not a case we are expecting
     */
    @Override
    public IUnaryFlowFunction getCallNoneToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      // if we're missing callees, just keep what information we have
      return IdentityFlowFunction.identity();
    }

    /**
     * flow function from call node to return node at a call site when callees exist. We kill everything; surviving facts should
     * flow out of the callee
     */
    @Override
    public IUnaryFlowFunction getCallToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      return KillEverything.singleton();
    }

    /**
     * flow function for normal intraprocedural edges
     */
    @Override
    public IUnaryFlowFunction getNormalFlowFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      final IExplodedBasicBlock ebb = src.getDelegate();
      SSAInstruction instruction = ebb.getInstruction();
      if (instruction instanceof SSAPutInstruction) {
        final SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
        if (putInstr.isStatic()) {
          return new IUnaryFlowFunction() {

            @Override
            public IntSet getTargets(int d1) {
              System.out.println(ebb.toString());
              System.out.println(d1);
              // first, gen this statement
              int factNum = domain.getMappedIndex(cha.lookupClass(putInstr.getDeclaredField().getDeclaringClass()));
              System.out.println(factNum);
              assert factNum != -1;
              MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
              result.add(factNum);
              // if incoming statement is some different statement that defs the same static field, kill it; otherwise, keep it
              if (d1 != factNum) {
                  result.add(d1);
              }
              return result;
            }

            @Override
            public String toString() {
              return "Initializer Normal Flow";
            }
          };
        }
      } else if(instruction instanceof SSAGetInstruction) {
        final SSAGetInstruction getInstr = (SSAGetInstruction) instruction;
        if (getInstr.isStatic()) { //Auf konstante �berpr�fen
          return new IUnaryFlowFunction() {

            @Override
            public IntSet getTargets(int d1) {
              // first, gen this statement
              int factNum = domain.getMappedIndex(cha.lookupClass(getInstr.getDeclaredField().getDeclaringClass()));
              assert factNum != -1;
              MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
              result.add(factNum);
              // if incoming statement is some different statement that defs the same static field, kill it; otherwise, keep it
              if (d1 != factNum) {
                  result.add(d1);
              }
              return result;
            }

            @Override
            public String toString() {
              return "Initializer Normal Flow";
            }
          };
        }
      } else if (instruction instanceof SSANewInstruction) {
        final SSANewInstruction newInstr = (SSANewInstruction) instruction;
        return new IUnaryFlowFunction() {

          @Override
          public IntSet getTargets(int d1) {
            // first, gen this statement
            int factNum = domain.getMappedIndex(cha.lookupClass(newInstr.getConcreteType()));
            assert factNum != -1;
            MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
            result.add(factNum);
            // if incoming statement is some different statement that defs the same static field, kill it; otherwise, keep it
            if (d1 != factNum) {
                result.add(d1);
            }
            return result;
          }

          @Override
          public String toString() {
            return "Initializer Normal Flow";
          }
        };
        
      } else if (instruction instanceof SSAInvokeInstruction) {
        final SSAInvokeInstruction invInstr = (SSAInvokeInstruction) instruction;
        if (invInstr.isStatic()) {
          return new IUnaryFlowFunction() {

            @Override
            public IntSet getTargets(int d1) {
              System.out.println("Invoke!");
              // first, gen this statement
              int factNum = domain.getMappedIndex(cha.lookupClass(invInstr.getDeclaredTarget().getDeclaringClass()));
              assert factNum != -1;
              MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
              result.add(factNum);
              // if incoming statement is some different statement that defs the same static field, kill it; otherwise, keep it
              if (d1 != factNum) {
                  result.add(d1);
              }
              return result;
            }

            @Override
            public String toString() {
              return "Initializer Normal Flow";
            }
          };
        }
      }
      
      // identity function when src block isn't for a putstatic
      return IdentityFlowFunction.identity();
    }

    /**
     * standard flow function from callee to caller; just identity
     */
    @Override
    public IFlowFunction getReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> call,
        BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dest) {
      return IdentityFlowFunction.identity();
    }

  }
  
  private class ReachingDefsProblem implements
  PartiallyBalancedTabulationProblem<BasicBlockInContext<IExplodedBasicBlock>, CGNode, IClass> {

    private InitializerFlowFunctions flowFunctions = new InitializerFlowFunctions(domain);

    /**
     * path edges corresponding to all putstatic instructions, used as seeds for the analysis
     */
    private Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds = collectInitialSeeds();
    
    /**
     * we use the entry block of the CGNode as the fake entry when propagating from callee to caller with unbalanced parens
     */
    @Override
    public BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(BasicBlockInContext<IExplodedBasicBlock> node) {
      final CGNode cgNode = node.getNode();
      return getFakeEntry(cgNode);
    }
    
    /**
     * we use the entry block of the CGNode as the "fake" entry when propagating from callee to caller with unbalanced parens
     */
    private BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(final CGNode cgNode) {
      BasicBlockInContext<IExplodedBasicBlock>[] entriesForProcedure = supergraph.getEntriesForProcedure(cgNode);
      assert entriesForProcedure.length == 1;
      return entriesForProcedure[0];
    }
    
    /**
     * collect the putstatic instructions in the call graph as {@link PathEdge} seeds for the analysis
     */
    private Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> collectInitialSeeds() {
      Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> result = HashSetFactory.make();
      for (BasicBlockInContext<IExplodedBasicBlock> bb : supergraph) {
        IExplodedBasicBlock ebb = bb.getDelegate();
        SSAInstruction instruction = ebb.getInstruction();
        if (instruction instanceof SSAPutInstruction) {
          SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
          if (putInstr.isStatic()) {
            final CGNode cgNode = bb.getNode();
            IClass fact = cha.lookupClass(putInstr.getDeclaredField().getDeclaringClass());
            int factNum = domain.add(fact);
            BasicBlockInContext<IExplodedBasicBlock> fakeEntry = getFakeEntry(cgNode);
            // note that the fact number used for the source of this path edge doesn't really matter
            result.add(PathEdge.createPathEdge(fakeEntry, factNum, bb, factNum));
    
          }
        } else if (instruction instanceof SSAGetInstruction) {
          SSAGetInstruction getInstr = (SSAGetInstruction) instruction;
          if (getInstr.isStatic()) {
            final CGNode cgNode = bb.getNode();
            IClass fact = cha.lookupClass(getInstr.getDeclaredField().getDeclaringClass());
            int factNum = domain.add(fact);
            BasicBlockInContext<IExplodedBasicBlock> fakeEntry = getFakeEntry(cgNode);
            // note that the fact number used for the source of this path edge doesn't really matter
            result.add(PathEdge.createPathEdge(fakeEntry, factNum, bb, factNum));
    
          }
        } else if (instruction instanceof SSANewInstruction) {
          SSANewInstruction newInstr = (SSANewInstruction) instruction;
          final CGNode cgNode = bb.getNode();
          IClass fact = cha.lookupClass(newInstr.getConcreteType());
          int factNum = domain.add(fact);
          BasicBlockInContext<IExplodedBasicBlock> fakeEntry = getFakeEntry(cgNode);
          // note that the fact number used for the source of this path edge doesn't really matter
          result.add(PathEdge.createPathEdge(fakeEntry, factNum, bb, factNum));
        } else if (instruction instanceof SSAInvokeInstruction) {
          SSAInvokeInstruction invInstr = (SSAInvokeInstruction) instruction;
          if (invInstr.isStatic()) {
            final CGNode cgNode = bb.getNode();
            IClass fact = cha.lookupClass(invInstr.getDeclaredTarget().getDeclaringClass());
            int factNum = domain.add(fact);
            BasicBlockInContext<IExplodedBasicBlock> fakeEntry = getFakeEntry(cgNode);
            // note that the fact number used for the source of this path edge doesn't really matter
            result.add(PathEdge.createPathEdge(fakeEntry, factNum, bb, factNum));
          }
        } 
      }
      return result;
    }
    
    @Override
    public IPartiallyBalancedFlowFunctions<BasicBlockInContext<IExplodedBasicBlock>> getFunctionMap() {
      return flowFunctions;
    }
    
    @Override
    public TabulationDomain<IClass, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
      return domain;
    }
    
    /**
     * we don't need a merge function; the default unioning of tabulation works fine
     */
    @Override
    public IMergeFunction getMergeFunction() {
      return null;
    }
    
    @Override
    public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
      return supergraph;
    }
    
    @Override
    public Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds() {
      return initialSeeds;
    }

}

  /**
  * perform the tabulation analysis and return the {@link TabulationResult}
  */
  public TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, IClass> analyze() {
    PartiallyBalancedTabulationSolver<BasicBlockInContext<IExplodedBasicBlock>, CGNode, IClass> solver = PartiallyBalancedTabulationSolver
        .createPartiallyBalancedTabulationSolver(new ReachingDefsProblem(), null);
    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, IClass> result = null;
    try {
      result = solver.solve();
    } catch (CancelException e) {
      // this shouldn't happen 
      assert false;
    }
    return result;
  }

  public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
    return supergraph;
  }

  public TabulationDomain<IClass, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
    return domain;
  }
  
  
}
