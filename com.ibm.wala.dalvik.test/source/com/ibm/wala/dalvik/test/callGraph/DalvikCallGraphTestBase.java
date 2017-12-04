/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.dalvik.test.callGraph;

import static com.ibm.wala.dalvik.test.util.Util.makeDalvikScope;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.core.tests.shrike.DynamicCallGraphTestBase;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.LocatorFlags;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.TemporaryFile;

public class DalvikCallGraphTestBase extends DynamicCallGraphTestBase {
	
	protected static <T> Set<T> processCG(CallGraph cg, Predicate<CGNode> filter, Function<CGNode,T> map) {
		Set<T> result = HashSetFactory.make();
		for(CGNode n : cg) {
			if (filter.test(n)) {
				result.add(map.apply(n));
			}
		}
		return result;
	}
	
	protected static Set<MethodReference> applicationMethods(CallGraph cg) {
		return processCG(cg,
			t -> t.getMethod().getReference().getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application),
			object -> object.getMethod().getReference());
	}
	

	public void dynamicCG(File javaJarPath, String mainClass, String... args) throws FileNotFoundException, IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, IllegalArgumentException, InterruptedException {
		File F;
		try (final FileInputStream in = new FileInputStream(javaJarPath)) {
		  F = TemporaryFile.streamToFile(new File("test_jar.jar"), in);
		}
		F.deleteOnExit();
		instrument(F.getAbsolutePath());
		run(mainClass.substring(1).replace('/', '.'), "LibraryExclusions.txt", args);
	}


	@SuppressWarnings("unused")
  private static SSAContextInterpreter makeDefaultInterpreter(AnalysisOptions options, IAnalysisCacheView cache) {
		return new DefaultSSAInterpreter(options, cache) {
			@Override
			public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
				return 
					new MapIterator<>(
						new FilterIterator<>(
								node.getIR().iterateAllInstructions(), 
								SSANewInstruction.class::isInstance), 
						object -> ((SSANewInstruction)object).getNewSite()
					);
			}
		};
	}
	
	public static Pair<CallGraph, PointerAnalysis<InstanceKey>> makeAPKCallGraph(URI[] androidLibs, File androidAPIJar, String apkFileName, IProgressMonitor monitor, ReflectionOptions policy) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
		AnalysisScope scope = makeDalvikScope(androidLibs, androidAPIJar, apkFileName);

		final IClassHierarchy cha = ClassHierarchyFactory.make(scope);

		IAnalysisCacheView cache = new AnalysisCacheImpl(new DexIRFactory());

		List<? extends Entrypoint> es = getEntrypoints(cha);

		assert ! es.isEmpty();
		
		AnalysisOptions options = new AnalysisOptions(scope, es);
		options.setReflectionOptions(policy);
		
		// SSAPropagationCallGraphBuilder cgb = Util.makeZeroCFABuilder(options, cache, cha, scope, null, makeDefaultInterpreter(options, cache));
		SSAPropagationCallGraphBuilder cgb = Util.makeZeroCFABuilder(options, cache, cha, scope);
  
		CallGraph callGraph = cgb.makeCallGraph(options, monitor);
		
		PointerAnalysis<InstanceKey> ptrAnalysis = cgb.getPointerAnalysis();
		
		return Pair.make(callGraph, ptrAnalysis);
	}

  public static List<? extends Entrypoint> getEntrypoints(final IClassHierarchy cha) {
    Set<LocatorFlags> flags = HashSetFactory.make();
		flags.add(LocatorFlags.INCLUDE_CALLBACKS);
		flags.add(LocatorFlags.EP_HEURISTIC);
		flags.add(LocatorFlags.CB_HEURISTIC);
		AndroidEntryPointLocator eps = new AndroidEntryPointLocator(flags);
		List<? extends Entrypoint> es = eps.getEntryPoints(cha);
    return es;
  }
	
	public static Pair<CallGraph, PointerAnalysis<InstanceKey>> makeDalvikCallGraph(URI[] androidLibs, File androidAPIJar, String mainClassName, String dexFileName) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
		AnalysisScope scope = makeDalvikScope(androidLibs, androidAPIJar, dexFileName);
		
		final IClassHierarchy cha = ClassHierarchyFactory.make(scope);

		TypeReference mainClassRef = TypeReference.findOrCreate(ClassLoaderReference.Application, mainClassName);
		IClass mainClass = cha.lookupClass(mainClassRef);
		assert mainClass != null;

		System.err.println("building call graph for " + mainClass + ":" + mainClass.getClass());
		
		Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha, mainClassName);
		
		IAnalysisCacheView cache = new AnalysisCacheImpl(new DexIRFactory());

		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

		SSAPropagationCallGraphBuilder cgb = Util.makeZeroCFABuilder(options, cache, cha, scope);
  
		CallGraph callGraph = cgb.makeCallGraph(options);

		MethodReference mmr = MethodReference.findOrCreate(mainClassRef, "main", "([Ljava/lang/String;)V");
		assert !callGraph.getNodes(mmr).isEmpty();
		
		PointerAnalysis<InstanceKey> ptrAnalysis = cgb.getPointerAnalysis();
		
		return Pair.make(callGraph, ptrAnalysis);
	}

}
