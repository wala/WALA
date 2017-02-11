package com.ibm.wala.examples.drivers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.examples.analysis.dataflow.NullAnalysis;
import com.ibm.wala.examples.analysis.dataflow.NullAnalysis.NullAnalysisDomain;
import com.ibm.wala.examples.analysis.dataflow.NullAnalysisFact;
import com.ibm.wala.examples.analysis.dataflow.NullSet;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.NullTestPiPolicy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.warnings.Warnings;

public class NullAnalysisDriver {
	
	// more aggressive exclusions to avoid library blowup
	// in inter-procedural tests
	private static final String EXCLUSIONS = "java\\/awt\\/.*\n" + "javax\\/swing\\/.*\n" + "sun\\/awt\\/.*\n"
			+ "sun\\/swing\\/.*\n" + "com\\/sun\\/.*\n" + "sun\\/.*\n" + "org\\/netbeans\\/.*\n"
			+ "org\\/openide\\/.*\n" + "com\\/ibm\\/crypto\\/.*\n" + "com\\/ibm\\/security\\/.*\n"
			+ "org\\/apache\\/xerces\\/.*\n" + "java\\/security\\/.*\n" + "";

	
	public static void main(String[] args)
			throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException, IOException {

		long start = System.currentTimeMillis();
		Properties p = CommandLine.parse(args);
		String scopeFile = p.getProperty("scopeFile");
		if (scopeFile == null) {
			throw new IllegalArgumentException("must specify scope file");
		}
		String mainClass = p.getProperty("mainClass");
		if (mainClass == null) {
			throw new IllegalArgumentException("must specify main class");
		}
		AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, null,
				NullAnalysisDriver.class.getClassLoader());
		scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes("UTF-8"))));
		IClassHierarchy cha = ClassHierarchyFactory.make(scope);
		System.out.println(cha.getNumberOfClasses() + " classes");
		// System.out.println(Warnings.asString());
		Warnings.clear();
		AnalysisOptions options = new AnalysisOptions();
		Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha, mainClass);
		options.setEntrypoints(entrypoints);

		options.getSSAOptions().setPiNodePolicy(NullTestPiPolicy.createNullTestPiPolicy());

		// you can dial down reflection handling if you like
		options.setReflectionOptions(ReflectionOptions.NONE);
		AnalysisCache cache = new AnalysisCacheImpl();

		// other builders can be constructed with different Util methods
		CallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
		// CallGraphBuilder builder = Util.makeNCFABuilder(2, options, cache,
		// cha, scope);
		// CallGraphBuilder builder = Util.makeVanillaNCFABuilder(2, options,
		// cache, cha, scope);
		System.out.println("building call graph...");
		CallGraph cg = builder.makeCallGraph(options, null);

		long end = System.currentTimeMillis();

		System.out.println("done");
		System.out.println("took " + (end - start) + "ms");
		System.out.println(CallGraphStats.getStats(cg));

		NullAnalysis nullAnalysis = new NullAnalysis(cg, cache);

		start = System.currentTimeMillis();

		TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, NullAnalysisFact> result = nullAnalysis.analyze();;

		end = System.currentTimeMillis();
		
		System.out.println("done Null analysis");
		System.out.println("NULL analysis took " + (end - start) + "ms");
		
		
		
		Set<Pair<BasicBlockInContext<IExplodedBasicBlock>, Integer>> warnings = new HashSet<Pair<BasicBlockInContext<IExplodedBasicBlock>, Integer>>(),
				errors = new HashSet<Pair<BasicBlockInContext<IExplodedBasicBlock>, Integer>>();

		Iterator<BasicBlockInContext<IExplodedBasicBlock>> nodes = nullAnalysis.getSupergraph().iterator();
		NullAnalysisDomain domain = nullAnalysis.getDomain();

		while (nodes.hasNext()) {

			// for each node of the supergraph
			BasicBlockInContext<IExplodedBasicBlock> node = nodes.next();

			SSAInstruction instr = node.getDelegate().getInstruction();

			if (instr != null && instr instanceof SSAInvokeInstruction) {
				// if the node corresponds to an invoke instruction

				SSAInvokeInstruction invokeInstr = (SSAInvokeInstruction) instr;
				if (invokeInstr.getCallSite().isVirtual()) {
					// if it is a virtual invoke (obj.f())
					assert invokeInstr.getNumberOfUses() > 0;

					int derefValueNum = invokeInstr.getUse(0);
					int mayNullFactIdx = domain
							.getMappedIndex(new NullAnalysisFact(node.getNode(), derefValueNum, NullSet.MayNULL));
					int mayNotNullFactIdx = domain
							.getMappedIndex(new NullAnalysisFact(node.getNode(), derefValueNum, NullSet.MayNotNULL));
					IntSet reachableFacts = result.getResult(node);

					if (reachableFacts.contains(mayNullFactIdx)) {
						// if the dereferenced value number may be NULL
						if (!reachableFacts.contains(mayNotNullFactIdx)) {
							// if the dereferenced value number is certainly
							// NULL
							if (isInAppScope(node)) {
								// record an error, if the node is in app scope
								errors.add(Pair.make(node, mayNullFactIdx));

								printInvokeStatus(node, invokeInstr, reachableFacts, isInAppScope(node),
											"ERROR: NULL dereference");
							}
						} else {
							// if the dereferenced value number may be not NULL
							if (isInAppScope(node)) {
								// record a warning, if the node is in app scope
								warnings.add(Pair.make(node, mayNullFactIdx));
		
								printInvokeStatus(node, invokeInstr, reachableFacts, isInAppScope(node),
											"WARNING: possible NULL dereference");
							}
						}
					}
				}
			}
		}		
	}

	/**
	 * Used for printing analysis information about the given invoke statement
	 * 
	 * @param node
	 * @param invokeInstr
	 * @param reachableFacts
	 * @param inAppScope
	 * @param msg
	 */
	private static void printInvokeStatus(BasicBlockInContext<IExplodedBasicBlock> node,
			SSAInvokeInstruction invokeInstr, IntSet reachableFacts, boolean inAppScope, String msg) {

		System.out.println("-----");
		System.out.println("in: " + node.getMethod());
		System.out.println("  " + invokeInstr + " : " + reachableFacts);
		System.out.println("  deref: " + invokeInstr.getCallSite().getDeclaredTarget().getName());
		System.out.println("  " + msg);
	}

	/**
	 * Returns true if the given method is not in the Primordial class loader
	 * 
	 * @param method
	 * @return
	 */
	private static boolean isInAppScope(IMethod method) {
		return !method.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Primordial);
	}

	private static boolean isInAppScope(BasicBlockInContext<IExplodedBasicBlock> node) {
		boolean isInApp = isInAppScope(node.getMethod());
		return isInApp;
	}
	
}
