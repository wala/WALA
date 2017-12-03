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
import java.net.URI;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.TemporaryFile;

public abstract class DynamicDalvikComparisonTest extends DalvikCallGraphTestBase {

	protected void test(URI[] androidLibs, String mainClass, String javaScopeFile, String... args) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException, ClassNotFoundException, SecurityException, InvalidClassFileException, FailureException {
		AnalysisScope javaScope = CallGraphTestUtil.makeJ2SEAnalysisScope(javaScopeFile, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
		String javaJarPath = getJavaJar(javaScope);
		File androidDex = convertJarToDex(javaJarPath);
		Pair<CallGraph,PointerAnalysis<InstanceKey>> android = makeDalvikCallGraph(androidLibs, null, mainClass, androidDex.getAbsolutePath());

		dynamicCG(new File(javaJarPath), mainClass, args);
		
		checkEdges(android.fst, t -> t.getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application));
	}
	
  protected File testFile(String file) throws IOException {
    File inputFile = TemporaryFile.urlToFile(file, getClass().getClassLoader().getResource(file));
    inputFile.deleteOnExit();
    return inputFile;
  }

}
