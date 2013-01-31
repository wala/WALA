/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, 
 *                Rogan Creswick <creswick@galois.com>, 
 *                Adam Foltzer <acfoltzer@galois.com>)
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
package org.scandroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scandroid.domain.CodeElement;
import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.flow.FlowAnalysis;
import org.scandroid.flow.InflowAnalysis;
import org.scandroid.flow.OutflowAnalysis;
import org.scandroid.flow.types.FlowType;
import org.scandroid.spec.AndroidSpecs;
import org.scandroid.spec.ISpecs;
import org.scandroid.util.AndroidAnalysisContext;
import org.scandroid.util.CGAnalysisContext;
import org.scandroid.util.CLISCanDroidOptions;
import org.scandroid.util.EntryPoints;
import org.scandroid.util.IEntryPointSpecifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class SeparateEntryAnalysis {
	private static final Logger logger = LoggerFactory
			.getLogger(SeparateEntryAnalysis.class);

	public static void main(String[] args) throws Exception {
		CLISCanDroidOptions options = new CLISCanDroidOptions(args, true);

		logger.info("Loading app.");
		AndroidAnalysisContext analysisContext = new AndroidAnalysisContext(
				options);

		URI summariesURI = options.getSummariesURI();
		InputStream summaryStream = null;
		if (null != summariesURI) {
			File summariesFile = new File(summariesURI);

			if (!summariesFile.exists()) {
				logger.error("Could not find summaries file: " + summariesFile);
				System.exit(1);
			}

			summaryStream = new FileInputStream(summariesFile);
		}

//		for (IClass c : analysisContext.getClassHierarchy()) {
//			logger.error(" class loaded: {}", c);
//		}
//		
		
		final List<Entrypoint> entrypoints = EntryPoints
				.defaultEntryPoints(analysisContext.getClassHierarchy());
		if (entrypoints == null || entrypoints.size() == 0) {
			throw new IOException("No Entrypoints Detected!");
		}

		for (Entrypoint entry : entrypoints) {
			logger.info("Entry point: " + entry);
		}

		if (options.separateEntries()) {
			int i = 1;
			for (final Entrypoint entry : entrypoints) {
				CGAnalysisContext<IExplodedBasicBlock> cgContext = new CGAnalysisContext<IExplodedBasicBlock>(
						analysisContext, new IEntryPointSpecifier() {
							@Override
							public List<Entrypoint> specify(
									AndroidAnalysisContext analysisContext) {
								return Lists.newArrayList(entry);
							}
						});
				logger.info("** Processing entry point " + i + "/"
						+ entrypoints.size() + ": " + entry);
				analyze(cgContext, summaryStream, null);
				i++;
			}
		} else {
			CGAnalysisContext<IExplodedBasicBlock> cgContext = new CGAnalysisContext<IExplodedBasicBlock>(
					analysisContext, new IEntryPointSpecifier() {
						@Override
						public List<Entrypoint> specify(
								AndroidAnalysisContext analysisContext) {
							return entrypoints;
						}
					});
			analyze(cgContext, summaryStream, null);
		}
	}

	/**
	 * @param analysisContext
	 * @param localEntries
	 * @param methodAnalysis
	 * @param monitor
	 * @return the number of permission outflows detected
	 * @throws IOException
	 */
	public static int analyze(
			CGAnalysisContext<IExplodedBasicBlock> analysisContext,
			InputStream summariesStream, IProgressMonitor monitor)
			throws IOException {
		try {
			logger.info("Supergraph size = "
					+ analysisContext.graph.getNumberOfNodes());

			Map<InstanceKey, String> prefixes;
			if (analysisContext.getOptions().stringPrefixAnalysis()) {
				logger.info("Running prefix analysis.");
				prefixes = UriPrefixAnalysis.runAnalysisHelper(
						analysisContext.cg, analysisContext.pa);
				logger.info("Number of prefixes = " + prefixes.values().size());
			} else {
				prefixes = new HashMap<InstanceKey, String>();
			}

			ISpecs specs = new AndroidSpecs();

			logger.info("Running inflow analysis.");
			Map<BasicBlockInContext<IExplodedBasicBlock>, Map<FlowType<IExplodedBasicBlock>, Set<CodeElement>>> initialTaints = InflowAnalysis
					.analyze(analysisContext, prefixes, specs);

			logger.info("  Initial taint size = " + initialTaints.size());

			logger.info("Running flow analysis.");
			IFDSTaintDomain<IExplodedBasicBlock> domain = new IFDSTaintDomain<IExplodedBasicBlock>();
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult = FlowAnalysis
					.analyze(analysisContext, initialTaints, domain, monitor);

			logger.info("Running outflow analysis.");
			Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> permissionOutflow = new OutflowAnalysis(
					analysisContext, specs).analyze(flowResult, domain);
			logger.info("  Permission outflow size = "
					+ permissionOutflow.size());

			// logger.info("Running Checker.");
			// Checker.check(permissionOutflow, perms, prefixes);

			logger.info("");
			logger.info("================================================================");
			logger.info("");

			for (Map.Entry<BasicBlockInContext<IExplodedBasicBlock>, Map<FlowType<IExplodedBasicBlock>, Set<CodeElement>>> e : initialTaints
					.entrySet()) {
				logger.info(e.getKey().toString());
				for (Map.Entry<FlowType<IExplodedBasicBlock>, Set<CodeElement>> e2 : e
						.getValue().entrySet()) {
					logger.info(e2.getKey() + " <- " + e2.getValue());
				}
			}
			for (Map.Entry<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> e : permissionOutflow
					.entrySet()) {
				logger.info(e.getKey().toString());
				for (FlowType t : e.getValue()) {
					logger.info("    --> " + t);
				}
			}

			// System.out.println("DOMAIN ELEMENTS");
			// for (int i = 1; i < domain.getSize(); i++) {
			// System.out.println("#"+i+" - "+domain.getMappedObject(i));
			// }
			// System.out.println("------");
			// for (CGNode n:loader.cg.getEntrypointNodes()) {
			// for (int i = 0; i < 6; i++)
			// {
			// try {
			// System.out.println(i+": ");
			// String[] s =
			// n.getIR().getLocalNames(n.getIR().getInstructions().length-1, i);
			//
			// for (String ss:s)
			// System.out.println("\t"+ss);
			// }
			// catch (Exception e) {
			// System.out.println("exception at " + i);
			// }
			// }
			// }
			//
			// System.out.println("------");
			// for (CGNode n:loader.cg.getEntrypointNodes()) {
			// for (SSAInstruction ssa: n.getIR().getInstructions()) {
			// // System.out.println("Definition " + ssa.getDef() + ":"+ssa);
			// System.out.println("Definition "+ssa);
			// }
			// }
			return permissionOutflow.size();
		} catch (com.ibm.wala.util.debug.UnimplementedError e) {
			logger.error("exception during analysis", e);
		}
		return 0;
	}
}
