/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.eclipse.tests;

import java.util.Collection;

import junit.framework.TestCase;

import org.eclipse.jdt.core.JavaModelException;

import com.ibm.wala.eclipse.cg.model.WalaCGModelWithMain;
import com.ibm.wala.eclipse.util.JdtUtil;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.warnings.WalaException;

/**
 * Smoke test on the Wala call graph Eclipse view
 * 
 * @author aying
 */
public class WalaCGModelTest extends TestCase {

	@Override
	public void setUp() {
		System.out.println("Importing projects into the test workspace");
		EclipseTestUtil.importZippedProject("HelloWorld.zip");
	}	

	public void testSmokeTest() throws WalaException, JavaModelException {
		// get the input
		String appJarFullPath = JdtUtil.getHelloWorldJar().getRawLocation().toString();

		// compute the call graph
		WalaCGModelWithMain model = new WalaCGModelWithMain(appJarFullPath);
		model.buildGraph();
		
		Collection roots = model.getRoots();
		assertNotNull(roots);
		
		Graph graph = model.getGraph();
		assertEquals(6, graph.getNumberOfNodes());
	}

}
