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

import static com.ibm.wala.dalvik.test.util.Util.convertJarToDex;
import static com.ibm.wala.dalvik.test.util.Util.getJavaJar;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ExceptionReturnValueKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;

public class JVMLDalvikComparisonTest extends DalvikCallGraphTestBase {

	private static Pair<CallGraph,PointerAnalysis<InstanceKey>> makeJavaBuilder(String scopeFile, String mainClass) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
		AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(scopeFile, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
		ClassHierarchy cha = ClassHierarchyFactory.make(scope);
		Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
		AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
		SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
		CallGraph CG = builder.makeCallGraph(options);
		return Pair.make(CG, builder.getPointerAnalysis());
	}
	
	private static Set<Pair<CGNode,CGNode>> edgeDiff(CallGraph from, CallGraph to) {
		Set<Pair<CGNode,CGNode>> result = HashSetFactory.make();
		for(CGNode f : from) {
			if (! f.getMethod().isSynthetic()) {
			outer: for(CGNode t : from) {
				if (!t.getMethod().isSynthetic() && from.hasEdge(f, t)) {
					Set<CGNode> fts = to.getNodes(f.getMethod().getReference());
					Set<CGNode> tts = to.getNodes(t.getMethod().getReference());
					for(CGNode x : fts) {
						for(CGNode y : tts) {
							if (to.hasEdge(x, y)) {
								continue outer;
							}
						}
					}
					result.add(Pair.make(f, t));
				}
			}
			}
		}
		return result;
	}
	
	private static void test(String mainClass, String javaScopeFile) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException {
		Pair<CallGraph, PointerAnalysis<InstanceKey>> java = makeJavaBuilder(javaScopeFile, mainClass);

		AnalysisScope javaScope = java.fst.getClassHierarchy().getScope();
		String javaJarPath = getJavaJar(javaScope);
		File androidDex = convertJarToDex(javaJarPath);
		Pair<CallGraph,PointerAnalysis<InstanceKey>> android = makeDalvikCallGraph(null, null, mainClass, androidDex.getAbsolutePath());
	
		Set<MethodReference> androidMethods = applicationMethods(android.fst);
		Set<MethodReference> javaMethods = applicationMethods(java.fst);
		
		Iterator<Pair<CGNode, CGNode>> javaExtraEdges = edgeDiff(java.fst, android.fst).iterator();
		boolean fail = false;
		if (javaExtraEdges.hasNext()) {
			fail = true;
			Set<MethodReference> javaExtraNodes = HashSetFactory.make(javaMethods);
			javaExtraNodes.removeAll(androidMethods);		

			System.err.println(Iterator2Collection.toSet(javaExtraEdges));
			System.err.println(javaExtraNodes);
			
			System.err.println(android.fst);
			
			for(CGNode n : android.fst) {
				System.err.println("### " + n);
				if (n.getIR() != null) {
					System.err.println(n.getIR());
				
					System.err.println("return: " + android.snd.getPointsToSet(new ReturnValueKey(n)));
					System.err.println("exceptions: " + android.snd.getPointsToSet(new ExceptionReturnValueKey(n)));					
					for(int i = 1; i < n.getIR().getSymbolTable().getMaxValueNumber(); i++) {
						LocalPointerKey x = new LocalPointerKey(n, i);
						OrdinalSet<InstanceKey> s = android.snd.getPointsToSet(x);
						if (s != null && !s.isEmpty()) {
							System.err.println(i + ": " + s);
						}
					}
				}
			}
		}
		
		Assert.assertTrue(!fail);		
	}
	
	@Test
	public void testJLex() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException {
		test(TestConstants.JLEX_MAIN, TestConstants.JLEX);
	}

	@Test
	public void testJavaCup() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException {
		test(TestConstants.JAVA_CUP_MAIN, TestConstants.JAVA_CUP);
	}

	@Test
	public void testBCEL() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException {
		test(TestConstants.BCEL_VERIFIER_MAIN, TestConstants.BCEL);
	}
}
