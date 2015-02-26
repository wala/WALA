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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.TemporaryFile;

public class DynamicDalvikComparisonTest extends DalvikCallGraphTestBase {

	private void test(boolean useAndroidLib, String mainClass, String javaScopeFile, String... args) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InvalidClassFileException, FailureException {
		AnalysisScope javaScope = CallGraphTestUtil.makeJ2SEAnalysisScope(javaScopeFile, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
		String javaJarPath = getJavaJar(javaScope);
		File androidDex = convertJarToDex(javaJarPath);
		Pair<CallGraph,PointerAnalysis<InstanceKey>> android = makeDalvikCallGraph(useAndroidLib, mainClass, androidDex.getAbsolutePath());

		dynamicCG(new File(javaJarPath), mainClass, args);
		
	    checkEdges(android.fst, new Predicate<MethodReference>() {
	        @Override
	        public boolean test(MethodReference t) {
	        	return t.getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application);
	        }
	      });
	}
	
	@Test
	public void testJLexJavaLib() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InvalidClassFileException, FailureException {
		File inputFile = TemporaryFile.urlToFile("sample.lex", getClass().getClassLoader().getResource("sample.lex"));
		test(false, TestConstants.JLEX_MAIN, TestConstants.JLEX, inputFile.getAbsolutePath());
	}

	@Test
	public void testJLexDexLib() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InvalidClassFileException, FailureException {
		if (walaProperties != null) {
		  File inputFile = TemporaryFile.urlToFile("sample.lex", getClass().getClassLoader().getResource("sample.lex"));
		  test(true, TestConstants.JLEX_MAIN, TestConstants.JLEX, inputFile.getAbsolutePath());
		}
	}

	@Test
	public void testJavaCupJavaLib() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InvalidClassFileException, FailureException {
		File inputFile = TemporaryFile.urlToFile("troff2html.cup", getClass().getClassLoader().getResource("troff2html.cup"));
		test(false, TestConstants.JAVA_CUP_MAIN, TestConstants.JAVA_CUP, inputFile.getAbsolutePath());
	}

	@Test
	public void testJavaCupDexLib() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InvalidClassFileException, FailureException {
    if (walaProperties != null) {
      File inputFile = TemporaryFile.urlToFile("troff2html.cup", getClass().getClassLoader().getResource("troff2html.cup"));
      test(true, TestConstants.JAVA_CUP_MAIN, TestConstants.JAVA_CUP, inputFile.getAbsolutePath());
    }
	}

}
