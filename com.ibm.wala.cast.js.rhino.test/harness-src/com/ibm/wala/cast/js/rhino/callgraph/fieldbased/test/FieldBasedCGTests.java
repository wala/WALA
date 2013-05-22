/******************************************************************************
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test.CGUtil.BuilderType;
import com.ibm.wala.cast.js.test.TestJSCallGraphShape;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.CallGraph2JSON;
import com.ibm.wala.util.WalaException;

public class FieldBasedCGTests extends TestJSCallGraphShape {
	protected CGUtil util;

	@Override
	@Before
	public void setUp() throws Exception {
		util = new CGUtil(new CAstRhinoTranslatorFactory());
	}
	
	private void runTest(String script, Object[][] assertions, BuilderType... builderTypes) throws IOException, WalaException, Error {
	  for(BuilderType builderType : builderTypes) {
	    URL url = FieldBasedCGTests.class.getClassLoader().getResource(script);
	    JSCallGraph cg = util.buildCG(url, builderType);
	    System.out.println(cg);
	    try {
	      verifyGraphAssertions(cg, assertions);
	    } catch(AssertionFailedError afe) {
	      throw new AssertionFailedError(builderType + ": " + afe.getMessage());
	    }
	  }
	}
	
	@SuppressWarnings("unused")
	private void dumpCG(JSCallGraph cg) {
		CallGraph2JSON.IGNORE_HARNESS = false;
		Map<String, Set<String>> edges = CallGraph2JSON.extractEdges(cg);
		for(String callsite : edges.keySet())
			for(String callee : edges.get(callsite))
				System.out.println(callsite + " -> " + callee);
	}

	private static final Object[][] assertionsForSimpleJS = new Object[][] {
		new Object[] { ROOT, new String[] { "suffix:simple.js" } },
		new Object[] { "suffix:simple.js", new String[] { "suffix:foo", "suffix:bar", "suffix:A", "suffix:Function" } },
		new Object[] { "suffix:foo", new String[] { "suffix:bar" } },
	    new Object[] { "suffix:aluis", new String[] { "suffix:aluis" } }
	};
	
	@Test
	public void testSimpleJS() throws IOException, WalaException, Error {
		runTest("tests/fieldbased/simple.js", assertionsForSimpleJS, BuilderType.PESSIMISTIC, BuilderType.OPTIMISTIC, BuilderType.OPTIMISTIC_WORKLIST);
	}

	private static final Object[][] assertionsForOneShot = new Object[][] {
		new Object[] { ROOT, new String[] { "suffix:oneshot.js" } },
		new Object[] { "suffix:oneshot.js", new String[] { "suffix:f" } },
		new Object[] { "suffix:f", new String[] { "suffix:g" } }
	};
	
	@Test
	public void testOneshot() throws IOException, WalaException, Error {
		runTest("tests/fieldbased/oneshot.js", assertionsForOneShot, BuilderType.PESSIMISTIC, BuilderType.OPTIMISTIC, BuilderType.OPTIMISTIC_WORKLIST);
	}
	
	private static final Object[][] assertionsForCallbacks = new Object[][] {
		new Object[] { ROOT, new String[] { "suffix:callbacks.js" } },
		new Object[] { "suffix:callbacks.js", new String[] { "suffix:f" } },
		new Object[] { "suffix:f", new String[] { "suffix:k", "suffix:n" } },
		new Object[] { "suffix:k", new String[] { "suffix:l", "suffix:p" } }
	};
	
	@Test
	public void testCallbacks() throws IOException, WalaException, Error {
		runTest("tests/fieldbased/callbacks.js", assertionsForCallbacks, BuilderType.OPTIMISTIC, BuilderType.OPTIMISTIC_WORKLIST);
	}
	
	private static final Object[][] assertionsForLexical = new Object[][] {
		new Object[] { "suffix:h", new String[] { "suffix:g" } }
	};
	
	@Test
	public void testLexical() throws IOException, WalaException, Error {
		runTest("tests/fieldbased/lexical.js", assertionsForLexical, BuilderType.PESSIMISTIC, BuilderType.OPTIMISTIC, BuilderType.OPTIMISTIC_WORKLIST);
	}
	
	private static final Object[][] assertionsForReflectiveCall = new Object[][] {
		new Object[] { "suffix:h", new String[] { "suffix:Function_prototype_call" } },
		new Object[] { "suffix:f", new String[] { "suffix:k" } }
	};
	
	@Test
	public void testReflectiveCall() throws IOException, WalaException, Error {
		runTest("tests/fieldbased/reflective_calls.js", assertionsForReflectiveCall, BuilderType.OPTIMISTIC, BuilderType.OPTIMISTIC_WORKLIST);
	}
	
	private static final Object[][] assertionsForNew = new Object[][] {
	  new Object[] { "suffix:new.js", new String[] { "suffix:g", "suffix:f" } },
	  new Object[] { "suffix:g", new String[] { "!suffix:k" } }
	};
	
	@Test
	public void testNew() throws IOException, WalaException, Error {
	  runTest("tests/fieldbased/new.js", assertionsForNew, BuilderType.OPTIMISTIC, BuilderType.OPTIMISTIC_WORKLIST);
	}
  
  private static final Object[][] assertionsForCallbacks2 = new Object[][] {
    new Object[] { "suffix:callbacks2.js", new String[] { "suffix:g" } },
    new Object[] { "suffix:g", new String[] { "suffix:k", "!suffix:l" } }
  };
  
  @Test
  public void testCallbacks2() throws IOException, WalaException, Error {
    runTest("tests/fieldbased/callbacks2.js", assertionsForCallbacks2, BuilderType.OPTIMISTIC, BuilderType.OPTIMISTIC_WORKLIST);
  }
}
