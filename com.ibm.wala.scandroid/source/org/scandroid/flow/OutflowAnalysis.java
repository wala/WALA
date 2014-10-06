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
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *  Steve Suh           <suhsteve@gmail.com>
 *  
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>, Adam Foltzer <acfoltzer@galois.com>)
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.domain.InstanceKeyElement;
import org.scandroid.domain.LocalElement;
import org.scandroid.domain.ReturnElement;
import org.scandroid.flow.types.FlowType;
import org.scandroid.flow.types.ParameterFlow;
import org.scandroid.flow.types.ReturnFlow;
import org.scandroid.spec.CallArgSinkSpec;
import org.scandroid.spec.EntryArgSinkSpec;
import org.scandroid.spec.EntryRetSinkSpec;
import org.scandroid.spec.ISpecs;
import org.scandroid.spec.SinkSpec;
import org.scandroid.spec.StaticFieldSinkSpec;
import org.scandroid.util.CGAnalysisContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.intset.IntSet;

/**
 * @author acfoltzer
 * 
 */
public class OutflowAnalysis {
	private static final Logger logger = LoggerFactory
			.getLogger(OutflowAnalysis.class);

	private final CGAnalysisContext<IExplodedBasicBlock> ctx;
	private final CallGraph cg;
	private final ClassHierarchy cha;
	private final PointerAnalysis<InstanceKey> pa;
	private final ICFGSupergraph graph;
	private final ISpecs specs;

	public OutflowAnalysis(CGAnalysisContext<IExplodedBasicBlock> ctx,
			ISpecs specs) {
		this.ctx = ctx;
		this.cg = ctx.cg;
		this.cha = ctx.getClassHierarchy();
		this.pa = ctx.pa;
		this.graph = (ICFGSupergraph) ctx.graph;
		this.specs = specs;
	}

	private void addEdge(
			Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> graph,
			FlowType<IExplodedBasicBlock> source,
			FlowType<IExplodedBasicBlock> dest) {
		Set<FlowType<IExplodedBasicBlock>> dests = graph.get(source);
		if (dests == null) {
			dests = new HashSet<FlowType<IExplodedBasicBlock>>();
			graph.put(source, dests);
		}
		dests.add(dest);
		logger.debug("added edge from {} to {}", source, dest);
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private void processArgSinks(
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult,
			IFDSTaintDomain<IExplodedBasicBlock> domain,
			Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> flowGraph,
			List<SinkSpec> sinkSpecs) {
		List<Collection<IMethod>> targetList = new ArrayList<Collection<IMethod>>();

		for (int i = 0; i < sinkSpecs.size(); i++) {
			Collection<IMethod> tempList = sinkSpecs.get(i).getNamePattern()
					.getPossibleTargets(cha);
			targetList.add(tempList);
		}

		// look for all uses of query function and taint the results with the
		// Uri used in those functions
		Iterator<BasicBlockInContext<IExplodedBasicBlock>> graphIt = graph
				.iterator();
		while (graphIt.hasNext()) {
			BasicBlockInContext<IExplodedBasicBlock> block = graphIt.next();

			Iterator<SSAInvokeInstruction> invokeInstrs = IteratorUtil.filter(
					block.iterator(), SSAInvokeInstruction.class);

			while (invokeInstrs.hasNext()) {
				SSAInvokeInstruction invInst = invokeInstrs.next();

				for (IMethod target : cha.getPossibleTargets(invInst
						.getDeclaredTarget())) {

					for (int i = 0; i < targetList.size(); i++) {
						if (!targetList.get(i).contains(target)) {
							continue;
						}
						logger.debug("Found target: " + target);
						int[] argNums = sinkSpecs.get(i).getArgNums();

						if (null == argNums) {
							int staticIndex = 0;
							if (target.isStatic()) {
								staticIndex = 1;
							}

							int targetParamCount = target
									.getNumberOfParameters() - staticIndex;
							argNums = SinkSpec.getNewArgNums(targetParamCount);
						}

						CGNode node = block.getNode();

						IntSet resultSet = flowResult.getResult(block);
						for (int j = 0; j < argNums.length; j++) {
							logger.debug("Looping over arg[" + j + "] of "
									+ argNums.length);

							// The set of flow types we're looking for:
							Set<FlowType<IExplodedBasicBlock>> taintTypeSet = HashSetFactory.make();

							LocalElement le = new LocalElement(
									invInst.getUse(argNums[j]));
							Set<DomainElement> elements = domain
									.getPossibleElements(le);
							if (elements != null) {
								for (DomainElement de : elements) {
									if (resultSet.contains(domain
											.getMappedIndex(de))) {
										logger.debug("added to taintTypeSpecs: "
												+ de.taintSource);
										taintTypeSet.add(de.taintSource);
									}
								}
							}

							LocalPointerKey lpkey = new LocalPointerKey(node,
									invInst.getUse(argNums[j]));
							for (InstanceKey ik : pa.getPointsToSet(lpkey)) {
								for (DomainElement de : domain
										.getPossibleElements(new InstanceKeyElement(
												ik))) {
									if (resultSet.contains(domain
											.getMappedIndex(de))) {
										logger.debug("added to taintTypeSpecs: "
												+ de.taintSource);
										taintTypeSet.add(de.taintSource);
									}
								}
							}

							for (FlowType<IExplodedBasicBlock> dest : sinkSpecs
									.get(i).getFlowType(block)) {
								for (FlowType<IExplodedBasicBlock> source : taintTypeSet) {
									logger.debug("added edge: " + source
											+ " \n \tto \n\t" + dest);
									// flow taint into uriIK
									addEdge(flowGraph, source, dest);
								}
							}
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private void processEntryArgs(
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult,
			IFDSTaintDomain<IExplodedBasicBlock> domain,
			Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> flowGraph,
			SinkSpec ss) {

		int[] newArgNums;
		for (IMethod im : ss.getNamePattern().getPossibleTargets(cha)) {
			// look for a tainted reply

			CGNode node = cg.getNode(im, Everywhere.EVERYWHERE);
			if (node == null) {
				logger.warn("null CGNode for {}", im.getSignature());
				continue;
			}

			BasicBlockInContext<IExplodedBasicBlock>[] entriesForProcedure = graph
					.getEntriesForProcedure(node);
			if (entriesForProcedure == null || 0 == entriesForProcedure.length) {
				logger.warn("procedure without entries {}", im.getSignature());
				continue;
			}
			if (1 != entriesForProcedure.length) {
				logger.error("More than one procedure entry.  (Are you sure you're using an ICFGSupergraph?)");
			}
			BasicBlockInContext<IExplodedBasicBlock> entryBlock = entriesForProcedure[0];

			newArgNums = ss.getArgNums();
			if (null == newArgNums) {
				int staticIndex = 1;
				if (im.isStatic()) {
					staticIndex = 0;
				}
				int targetParamCount = im.getNumberOfParameters() - staticIndex;

				newArgNums = SinkSpec.getNewArgNums(targetParamCount);
			}
			// for (BasicBlockInContext<E> block:
			// graph.getExitsForProcedure(node) ) {
			// IntIterator itr = flowResult.getResult(block).intIterator();
			// while (itr.hasNext()) {
			// int i = itr.next();
			// logger.debug("domain element at exit: "+domain.getMappedObject(i));
			//
			//
			// }
			// }
			for (int i = 0; i < newArgNums.length; i++) {

				// see if anything flowed into the args as sinks:
				for (DomainElement de : domain
						.getPossibleElements(new LocalElement(node.getIR()
								.getParameter(newArgNums[i])))) {

					for (BasicBlockInContext<IExplodedBasicBlock> block : graph
							.getExitsForProcedure(node)) {

						int mappedIndex = domain.getMappedIndex(de);
						if (flowResult.getResult(block).contains(mappedIndex)) {
							addEdge(flowGraph, de.taintSource,
									new ParameterFlow<IExplodedBasicBlock>(
											entryBlock, newArgNums[i], false));
						}
					}

					int mappedIndex = domain.getMappedIndex(de);
					if (flowResult.getResult(entryBlock).contains(mappedIndex)) {
						addEdge(flowGraph, de.taintSource,
								new ParameterFlow<IExplodedBasicBlock>(
										entryBlock, newArgNums[i], false));
					}

				}
				for (InstanceKey ik : pa.getPointsToSet(new LocalPointerKey(
						node, node.getIR().getParameter(newArgNums[i])))) {
					for (DomainElement de : domain
							.getPossibleElements(new InstanceKeyElement(ik))) {
						if (flowResult.getResult(entryBlock).contains(
								domain.getMappedIndex(de))) {
							logger.trace("found outflow in second EntryArgSink loop");
							addEdge(flowGraph, de.taintSource,
									new ParameterFlow<IExplodedBasicBlock>(
											entryBlock, newArgNums[i], false));
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private void processEntryRets(
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult,
			IFDSTaintDomain<IExplodedBasicBlock> domain,
			Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> flowGraph,
			SinkSpec ss) {

		for (IMethod im : ss.getNamePattern().getPossibleTargets(cha)) {
			// look for a tainted reply

			CGNode node = cg.getNode(im, Everywhere.EVERYWHERE);

			if (node == null) {
				logger.warn("could not find CGNode for SinkSpec {}", ss);
				continue;
			}

			BasicBlockInContext<IExplodedBasicBlock>[] exitsForProcedure = graph
					.getExitsForProcedure(node);
			if (exitsForProcedure == null || 0 == exitsForProcedure.length) {
				logger.warn("could not find exit blocks for SinkSpec {}", ss);
				continue;
			}

			final Set<DomainElement> possibleElements = domain
					.getPossibleElements(new ReturnElement());
			logger.debug("{} possible elements found for ReturnElement",
					possibleElements.size());
			for (DomainElement de : possibleElements) {
				logger.debug("processing domain element {}", de);
				for (BasicBlockInContext<IExplodedBasicBlock> block : exitsForProcedure) {
					logger.debug("{} instructions in block",
							block.getLastInstructionIndex());
					if (flowResult.getResult(block).contains(
							domain.getMappedIndex(de))) {
						logger.debug("original block has edge");
						addEdge(flowGraph, de.taintSource,
								new ReturnFlow<IExplodedBasicBlock>(block,
										false));
					}
					// Iterator<BasicBlockInContext<E>> it =
					// graph.getPredNodes(block);
					// while (it.hasNext()) {
					// BasicBlockInContext<E> realBlock = it.next();
					// if (realBlock.isExitBlock()) {
					// logger.warn("found edge to exit");
					// // addEdge(flowGraph,de.taintSource, new
					// ReturnFlow<E>(realBlock, false));
					// }
					// if(flowResult.getResult(realBlock).contains(domain.getMappedIndex(de)))
					// {
					// logger.debug("adding edge from {} to ReturnFlow",
					// de.taintSource);
					// addEdge(flowGraph,de.taintSource, new
					// ReturnFlow<E>(realBlock, false));
					// } else {
					// logger.debug("no edge from block {} for {}", realBlock,
					// de);
					// }
				}
			}

			for (BasicBlockInContext<IExplodedBasicBlock> block : exitsForProcedure) {
				Iterator<BasicBlockInContext<IExplodedBasicBlock>> it = graph
						.getPredNodes(block);
				while (it.hasNext()) {
					BasicBlockInContext<IExplodedBasicBlock> realBlock = it
							.next();
					final SSAInstruction inst = realBlock.getLastInstruction();
					if (null != inst && inst instanceof SSAReturnInstruction) {
						PointerKey pk = new LocalPointerKey(node,
								inst.getUse(0));
						for (InstanceKey ik : pa.getPointsToSet(pk)) {
							for (DomainElement ikElement : domain
									.getPossibleElements(new InstanceKeyElement(
											ik))) {
								if (flowResult.getResult(realBlock).contains(
										domain.getMappedIndex(ikElement))) {
									addEdge(flowGraph,
											ikElement.taintSource,
											new ReturnFlow<IExplodedBasicBlock>(
													realBlock, false));
								}
							}
						}
					}
				}
			}
		}
	}

	public Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> analyze(
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult,
			IFDSTaintDomain<IExplodedBasicBlock> domain) {
		return analyze(ctx.cg, ctx.getClassHierarchy(), ctx.graph, ctx.pa,
				flowResult, domain, specs);
	}

	public Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> analyze(
			CallGraph cg,
			ClassHierarchy cha,
			ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> graph,
			PointerAnalysis<InstanceKey> pa,
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult,
			IFDSTaintDomain<IExplodedBasicBlock> domain, ISpecs s) {

		logger.debug("****************************");
		logger.debug("* Running outflow analysis *");
		logger.debug("****************************");

		Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> taintFlow = HashMapFactory.make();

		SinkSpec[] ss = s.getSinkSpecs();
		logger.debug(ss.length + " sink Specs. ");

		for (int i = 0; i < ss.length; i++) {
			if (ss[i] instanceof EntryArgSinkSpec)
				processSinkSpec(flowResult, domain, taintFlow, ss[i]);
			else if (ss[i] instanceof CallArgSinkSpec)
				processSinkSpec(flowResult, domain, taintFlow, ss[i]);
			else if (ss[i] instanceof EntryRetSinkSpec)
				processSinkSpec(flowResult, domain, taintFlow, ss[i]);
			else if (ss[i] instanceof StaticFieldSinkSpec)
				processSinkSpec(flowResult, domain, taintFlow, ss[i]);
			else
				throw new UnsupportedOperationException(
						"SinkSpec not yet Implemented");
		}

		logger.info("************");
		logger.info("* Results: *");
		logger.info("************");

		logger.debug("{}", taintFlow.toString());

		/* TODO: re-enable this soon! */
		/*
		 * for(Entry<FlowType,Set<FlowType>> e: taintFlow.entrySet()) {
		 * WalaGraphToJGraphT walaJgraphT = new WalaGraphToJGraphT(flowResult,
		 * domain, e.getKey(), graph, cg); logger.debug("Source: " +
		 * e.getKey()); for(FlowType target:e.getValue()) {
		 * logger.debug("\t=> Sink: " + target); //logger.debug("SourceNode: "+
		 * e.getKey().getRelevantNode() +
		 * "\nSinkNode: "+target.getRelevantNode());
		 * walaJgraphT.calcPath(e.getKey().getRelevantNode(),
		 * target.getRelevantNode()); Iterator<DefaultEdge> edgeI =
		 * walaJgraphT.getPath().getEdgeList().iterator(); if (edgeI.hasNext())
		 * logger.debug("\t::Method Trace::"); int counter = 1; while
		 * (edgeI.hasNext()) { DefaultEdge edge = edgeI.next();
		 * logger.debug("\t\t#"+counter+": " +
		 * walaJgraphT.getJGraphT().getEdgeSource
		 * (edge).getMethod().getSignature() + " ==> " +
		 * walaJgraphT.getJGraphT()
		 * .getEdgeTarget(edge).getMethod().getSignature()); }
		 * 
		 * } }
		 */

		return taintFlow;
	}

	private void processSinkSpec(
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult,
			IFDSTaintDomain<IExplodedBasicBlock> domain,
			Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> flowGraph,
			SinkSpec ss) {
		Set<ISinkPoint> sinkPoints = calculateSinkPoints(ss);
		if (!(ss instanceof StaticFieldSinkSpec)) {
			logger.debug("for {}, sinkPoints={}", ss, sinkPoints);
		}
		for (ISinkPoint sinkPoint : sinkPoints) {
			for (FlowType<IExplodedBasicBlock> source : sinkPoint.findSources(
					ctx, flowResult, domain)) {
				addEdge(flowGraph, source, sinkPoint.getFlow());
			}
		}
	}

	private Set<ISinkPoint> calculateSinkPoints(SinkSpec sinkSpec) {
		if (sinkSpec instanceof EntryArgSinkSpec) {
			return calculateSinkPoints((EntryArgSinkSpec) sinkSpec);
		}
		if (sinkSpec instanceof CallArgSinkSpec) {
			return calculateSinkPoints((CallArgSinkSpec) sinkSpec);
		}
		if (sinkSpec instanceof EntryRetSinkSpec) {
			return calculateSinkPoints((EntryRetSinkSpec) sinkSpec);
		}
		if (sinkSpec instanceof StaticFieldSinkSpec) {
			return calculateSinkPoints((StaticFieldSinkSpec) sinkSpec);
		}
		throw new UnimplementedError();
	}

	private Set<ISinkPoint> calculateSinkPoints(EntryArgSinkSpec sinkSpec) {
		Set<ISinkPoint> points = HashSetFactory.make();

		Collection<IMethod> methods = sinkSpec.getNamePattern()
				.getPossibleTargets(cha);
		if (null == methods) {
			logger.warn("no methods found for sink spec {}", sinkSpec);
		}

		for (IMethod method : methods) {
			for (CGNode node : cg.getNodes(method.getReference())) {
				BasicBlockInContext<IExplodedBasicBlock> entryBlock = graph
						.getICFG().getEntry(node);
				BasicBlockInContext<IExplodedBasicBlock> exitBlock = graph
						.getICFG().getExit(node);
				for (int argNum : sinkSpec.getArgNums()) {
					final int ssaVal = node.getIR().getParameter(argNum);
					final ParameterFlow<IExplodedBasicBlock> sinkFlow = new ParameterFlow<IExplodedBasicBlock>(
							entryBlock, argNum, false);
					final LocalSinkPoint sinkPoint = new LocalSinkPoint(
							exitBlock, ssaVal, sinkFlow);
					points.add(sinkPoint);
				}
			}
		}
		return points;
	}

	private Set<ISinkPoint> calculateSinkPoints(final CallArgSinkSpec sinkSpec) {
		final Set<ISinkPoint> points = HashSetFactory.make();

		Collection<IMethod> methods = sinkSpec.getNamePattern()
				.getPossibleTargets(cha);
		if (null == methods) {
			logger.warn("no methods found for sink spec {}", sinkSpec);
		}

		Set<CGNode> callees = HashSetFactory.make();
		final Set<MethodReference> calleeRefs = HashSetFactory.make();
		for (IMethod method : methods) {
			callees.addAll(cg.getNodes(method.getReference()));
			calleeRefs.add(method.getReference());
		}
		logger.debug("callee nodes {}", callees);
		logger.debug("callee refs {}", calleeRefs);

		// for each possible callee
		for (CGNode callee : callees) {
			Iterator<CGNode> callers = cg.getPredNodes(callee);
			// for each possible caller of that callee
			while (callers.hasNext()) {
				final CGNode caller = callers.next();
				// look for invoke instructions
				caller.getIR().visitAllInstructions(new Visitor() {
					@Override
					public void visitInvoke(SSAInvokeInstruction invokeInst) {
						// if the invoke instruction targets a possible callee
						if (calleeRefs.contains(invokeInst.getDeclaredTarget())) {
							// look up the instruction's block in context
							// (surely there's a more straightforward way to do
							// this!)
							final SSAInstruction[] insts = graph.getICFG()
									.getCFG(caller).getInstructions();
							int invokeIndex = -1;
							for (int i = 0; i < insts.length; i++) {
								if (insts[i] instanceof SSAInvokeInstruction) {
									SSAInvokeInstruction invokeInst2 = (SSAInvokeInstruction) insts[i];
									if (invokeInst.getDeclaredTarget().equals(invokeInst2.getDeclaredTarget())) {
										invokeIndex = i;
										break;
									}
								}
							}
							if (invokeIndex == -1) {
								logger.error("couldn't find invoke instruction in caller node");
							}
							final IExplodedBasicBlock block = graph.getICFG()
									.getCFG(caller)
									.getBlockForInstruction(invokeIndex);
							BasicBlockInContext<IExplodedBasicBlock> callBlock = new BasicBlockInContext<IExplodedBasicBlock>(
									caller, block);

							for (int argNum : sinkSpec.getArgNums()) {
								// and add a sink point for each arg num
								final int ssaVal = invokeInst.getUse(argNum);
								final ParameterFlow<IExplodedBasicBlock> sinkFlow = new ParameterFlow<IExplodedBasicBlock>(
										callBlock, argNum, false);
								final LocalSinkPoint sinkPoint = new LocalSinkPoint(
										callBlock, ssaVal, sinkFlow);
								points.add(sinkPoint);
							}
						}
					}
				});
			}
		}

		return points;
	}

	private Set<ISinkPoint> calculateSinkPoints(EntryRetSinkSpec sinkSpec) {
		Set<ISinkPoint> points = HashSetFactory.make();

		Collection<IMethod> methods = sinkSpec.getNamePattern()
				.getPossibleTargets(cha);
		if (null == methods) {
			logger.warn("no methods found for sink spec {}", sinkSpec);
		}

		// for all possible returning methods
		for (IMethod method : methods) {
			// for all possible CGNodes of that method
			for (CGNode node : cg.getNodes(method.getReference())) {
				// get the unique (null) exit block
				BasicBlockInContext<IExplodedBasicBlock> nullExitBlock = graph
						.getICFG().getExit(node);
				// and for each predecessor to the exit block
				Iterator<BasicBlockInContext<IExplodedBasicBlock>> exitBlocks = graph
						.getPredNodes(nullExitBlock);
				while (exitBlocks.hasNext()) {
					// if that predecessor is a return instruction
					BasicBlockInContext<IExplodedBasicBlock> exitBlock = exitBlocks
							.next();
					final SSAInstruction inst = exitBlock.getDelegate()
							.getInstruction();
					if (inst instanceof SSAReturnInstruction) {
						// add a sink point for the instruction
						SSAReturnInstruction returnInst = (SSAReturnInstruction) inst;
						if (!returnInst.returnsVoid()) {
							final int ssaVal = returnInst.getResult();
							final ReturnFlow<IExplodedBasicBlock> sinkFlow = new ReturnFlow<IExplodedBasicBlock>(
									exitBlock, false);
							final LocalSinkPoint sinkPoint = new LocalSinkPoint(
									exitBlock, ssaVal, sinkFlow);
							points.add(sinkPoint);
						}
					}
				}
			}
		}

		return points;
	}

	private Set<ISinkPoint> calculateSinkPoints(StaticFieldSinkSpec sinkSpec) {
		Set<ISinkPoint> points = HashSetFactory.make();

		ICFGSupergraph graph = (ICFGSupergraph) ctx.graph;
		for (CGNode node : ctx.cg.getNodes(sinkSpec.getMethod().getReference())) {
			points.add(new StaticFieldSinkPoint(sinkSpec, graph.getICFG()
					.getExit(node)));
		}

		return points;
	}

}
