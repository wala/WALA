/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, 
 *                Rogan Creswick <creswick@galois.com>, 
 *                Adam Foltzer <acfoltzer@galois.com>)
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *  Steve Suh    <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scandroid.domain.CodeElement;
import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.flow.functions.TaintTransferFunctions;
import org.scandroid.flow.types.FlowType;
import org.scandroid.util.CGAnalysisContext;

import com.ibm.wala.dataflow.IFDS.IFlowFunctionMap;
import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationProblem;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.dataflow.IFDS.TabulationSolver;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;


public class FlowAnalysis {

	public static <E extends ISSABasicBlock>
    TabulationResult<BasicBlockInContext<E>, CGNode, DomainElement> 
    analyze(final CGAnalysisContext<E> analysisContext,
          Map<BasicBlockInContext<E>,
          Map<FlowType<E>,Set<CodeElement>>> initialTaints,
          IFDSTaintDomain<E> d
          ) throws CancelRuntimeException {
        return analyze(analysisContext.graph, analysisContext.cg, analysisContext.pa, initialTaints, d);
    }
    
    public static <E extends ISSABasicBlock>
    TabulationResult<BasicBlockInContext<E>, CGNode, DomainElement> 
    analyze(final CGAnalysisContext<E> analysisContext,
          Map<BasicBlockInContext<E>,
          Map<FlowType<E>,Set<CodeElement>>> initialTaints,
          IFDSTaintDomain<E> d,
          IFlowFunctionMap<BasicBlockInContext<E>> flowFunctionMap
          ) throws CancelRuntimeException {
        return analyze(analysisContext.graph, analysisContext.cg, initialTaints, d, flowFunctionMap);
    }
    
    public static <E extends ISSABasicBlock>
	  TabulationResult<BasicBlockInContext<E>, CGNode, DomainElement> 
	  analyze(final ISupergraph<BasicBlockInContext<E>, 
			  CGNode> graph,
	          CallGraph cg,
	          PointerAnalysis<InstanceKey> pa,
	          Map<BasicBlockInContext<E>, Map<FlowType<E>,Set<CodeElement>>> initialTaints,
	          IFDSTaintDomain<E> d
	        ) {
				return analyze(graph, cg, initialTaints, d, new TaintTransferFunctions<>(d, pa));

//    			return analyze(graph, cg, pa, initialTaints, d,
//    					progressMonitor, new IDTransferFunctions<E>(d, graph, pa));

    	
//				return analyze(graph, cg, pa, initialTaints, d,
//						progressMonitor, new IFDSTaintFlowFunctionProvider<E>(d, graph, pa));
			}

	public static <E extends ISSABasicBlock>
      TabulationResult<BasicBlockInContext<E>, CGNode, DomainElement> 
      analyze(final ISupergraph<BasicBlockInContext<E>, 
    		  CGNode> graph,
              CallGraph cg,
              Map<BasicBlockInContext<E>, Map<FlowType<E>,Set<CodeElement>>> initialTaints,
              IFDSTaintDomain<E> d,
              final IFlowFunctionMap<BasicBlockInContext<E>> flowFunctionMap
            ) {

        final IFDSTaintDomain<E> domain = d;

        final List<PathEdge<BasicBlockInContext<E>>>
           initialEdges = new ArrayList<>();

        //Add PathEdges to the taints
        //Places that initial taints occur, and where they initially flow into
        for(BasicBlockInContext<E> taintBB:initialTaints.keySet())
        {
        	Map<FlowType<E>, Set<CodeElement>> bbTaints = initialTaints.get(taintBB);
        	for(FlowType<E> taintType:bbTaints.keySet())
            {
                for(CodeElement taintElement:bbTaints.get(taintType))
                {
                	BasicBlockInContext<E>[] entryBlocks = graph.getEntriesForProcedure(taintBB.getNode());
                	for (BasicBlockInContext<E> entryBlock : entryBlocks) {
                		//Add PathEdge <s_p,0> -> <n,d1>
                		initialEdges.add(PathEdge.createPathEdge(entryBlock, 0, taintBB, domain.getMappedIndex(new DomainElement(taintElement,taintType))));
                	}
                    //initialEdges.add(PathEdge.createPathEdge(e.getKey(), 0, e.getKey(), domain.getMappedIndex(new DomainElement(o,e2.getKey()))));
                }
            }
        }
        //Add PathEdges to the entry points of the supergraph <s_main,0> -> <s_main,0>
        for (CGNode entry : cg.getEntrypointNodes()) {
        	BasicBlockInContext<E>[] bbic = graph.getEntriesForProcedure(entry);
        	for (BasicBlockInContext<E> element : bbic)
				initialEdges.add(PathEdge.createPathEdge(element, 0, element, 0));
        }
        
        final TabulationProblem<BasicBlockInContext<E>, CGNode, DomainElement>
          problem =
            new TabulationProblem<BasicBlockInContext<E>, CGNode, DomainElement>() {

            @Override
            public TabulationDomain<DomainElement, BasicBlockInContext<E>> getDomain() {
                return domain;
            }

            @Override
            public IFlowFunctionMap<BasicBlockInContext<E>> getFunctionMap() {
                return flowFunctionMap;
            }

            @Override
            public IMergeFunction getMergeFunction() {
                return null;
            }

            @Override
            public ISupergraph<BasicBlockInContext<E>, CGNode> getSupergraph() {
                return graph;
            }

            @Override
            public Collection<PathEdge<BasicBlockInContext<E>>> initialSeeds() {
                return initialEdges;
//              CGNode entryProc = cfg.getCallGraph().getEntrypointNodes()
//                      .iterator().next();
//              BasicBlockInContext<ISSABasicBlock> entryBlock = cfg
//                      .getEntry(entryProc);
//              for (int i = 0; i < entryProc.getIR().getNumberOfParameters(); i++) {
//                  list.add(PathEdge.createPathEdge(entryBlock, 0, entryBlock,
//                          domain.getMappedIndex(new LocalElement(i + 1))));
//              }
//              return list;
            }

        };
        TabulationSolver<BasicBlockInContext<E>, CGNode, DomainElement> solver =
            TabulationSolver.make(problem/**, progressMonitor*/);

        try {
        	TabulationResult<BasicBlockInContext<E>,CGNode, DomainElement> flowResult = solver.solve();
//        	if (options.ifdsExplorer()) {
//        		for (int i = 1; i < domain.getSize(); i++) {        			
//                            			
//        		}
//        		GraphUtil.exploreIFDS(flowResult);
//        	}
            return flowResult;
        } catch (CancelException e) {
            throw new CancelRuntimeException(e);
        }
    }

}
