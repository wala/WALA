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
public class NodejsRequireJsonTest {

	@Test
	public void test() throws Exception {
		URL fileUrl = getClass().getClassLoader().getResource("NodejsRequireJsonTest/index.js");
		File file = new File(fileUrl.toURI());

		PropagationCallGraphBuilder builder = NodejsCallGraphBuilderUtil.makeCGBuilder(file);
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		String cgString = CG.toString();
		
		assertTrue(cgString.contains("Lempty/jsonModule>"));
		assertTrue(cgString.contains("Lnested/jsonModule>"));
		assertTrue(cgString.contains("Lpackage/jsonModule>"));
		assertTrue(!cgString.contains("?"));
	}
}
