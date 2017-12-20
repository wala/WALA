/******************************************************************************
 * Copyright (c) 2002 - 2016 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Brian Pfretzschner - initial implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.nodejs.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.Test;

import com.ibm.wala.cast.js.nodejs.NodejsCallGraphBuilderUtil;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;

/**
 * @author Brian Pfretzschner &lt;brian.pfretzschner@gmail.com&gt;
 */
public class NodejsRequireTargetSelectorResolveTest {

	@Test
	public void testRequireSimple() throws Exception {
		URL fileUrl = getClass().getClassLoader().getResource("NodejsRequireTargetSelectorResolve/requireSimple/index.js");
		File file = new File(fileUrl.toURI());

		PropagationCallGraphBuilder builder = NodejsCallGraphBuilderUtil.makeCGBuilder(file);
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		String cgString = CG.toString();
		
		assertTrue(cgString.contains("Lmod/nodejsModule/moduleSource/exec>"));
		assertTrue(cgString.contains("Lmod/nodejsModule/moduleSource/SomeClass/hello>"));
		assertTrue(!cgString.contains("?"));
	}
	
	@Test
	public void testRequireStaticCircular() throws Exception {
		URL fileUrl = getClass().getClassLoader().getResource("NodejsRequireTargetSelectorResolve/requireStaticCircular/index.js");
		File file = new File(fileUrl.toURI());

		PropagationCallGraphBuilder builder = NodejsCallGraphBuilderUtil.makeCGBuilder(file);
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		String cgString = CG.toString();
		
		assertTrue(cgString.contains("Llib1/nodejsModule/moduleSource/lib1>"));
		assertTrue(cgString.contains("Llib2/nodejsModule/moduleSource/lib2>"));
		assertTrue(!cgString.contains("?"));
	}

	@Test
	public void testRequireDynamic() throws Exception {
		URL fileUrl = getClass().getClassLoader().getResource("NodejsRequireTargetSelectorResolve/requireDynamic/index.js");
		File file = new File(fileUrl.toURI());

		PropagationCallGraphBuilder builder = NodejsCallGraphBuilderUtil.makeCGBuilder(file);
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		String cgString = CG.toString();
		
		assertTrue(cgString.contains("Llib1/nodejsModule/moduleSource/lib1>"));
		assertTrue(cgString.contains("Llib2/nodejsModule/moduleSource/lib2>"));
		assertTrue(!cgString.contains("?"));
	}
	
	@Test
	public void testRequireNodeModules() throws Exception {
		URL fileUrl = getClass().getClassLoader().getResource("NodejsRequireTargetSelectorResolve/requireNodeModules/index.js");
		File file = new File(fileUrl.toURI());

		PropagationCallGraphBuilder builder = NodejsCallGraphBuilderUtil.makeCGBuilder(file);
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		String cgString = CG.toString();
		
		assertTrue(cgString.contains("Lnode_modules_lib_node_modules_sublib_sublib/nodejsModule/moduleSource"));
		assertTrue(!cgString.contains("?"));
	}
	
	@Test
	public void testRequireCoreModules() throws Exception {
		URL fileUrl = getClass().getClassLoader().getResource("NodejsRequireTargetSelectorResolve/requireCoreModules.js");
		File file = new File(fileUrl.toURI());

		PropagationCallGraphBuilder builder = NodejsCallGraphBuilderUtil.makeCGBuilder(file);
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		String cgString = CG.toString();
		
		assertTrue(cgString.contains("Lutil/nodejsModule/moduleSource/util"));
		assertTrue(cgString.contains("Lhttps/nodejsModule/moduleSource/https"));
		assertTrue(cgString.contains("Lhttp/nodejsModule/moduleSource/http"));
	}

}
