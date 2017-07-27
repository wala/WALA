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

import org.junit.Test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.test.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public class TestFieldBasedCG extends AbstractFieldBasedTest {
	private static final Object[][] assertionsForSimpleJS = new Object[][] {
		new Object[] { ROOT, new String[] { "suffix:simple.js" } },
		new Object[] { "suffix:simple.js", new String[] { "suffix:foo", "suffix:bar", "suffix:A" } },
		new Object[] { "suffix:foo", new String[] { "suffix:bar" } },
	    new Object[] { "suffix:aluis", new String[] { "suffix:aluis" } }
	};
	
	@Test
	public void testSimpleJSPessimistic() throws WalaException, Error, CancelException {
		runTest("tests/fieldbased/simple.js", assertionsForSimpleJS, BuilderType.PESSIMISTIC);
	}

	@Test
	public void testSimpleJSOptimistic() throws WalaException, Error, CancelException {
	  runTest("tests/fieldbased/simple.js", assertionsForSimpleJS, BuilderType.OPTIMISTIC);
	}
	
	@Test
	public void testSimpleJSWorklist() throws WalaException, Error, CancelException {
	  runTest("tests/fieldbased/simple.js", assertionsForSimpleJS, BuilderType.OPTIMISTIC_WORKLIST);
	}

	private static final Object[][] assertionsForOneShot = new Object[][] {
		new Object[] { ROOT, new String[] { "suffix:oneshot.js" } },
		new Object[] { "suffix:oneshot.js", new String[] { "suffix:f" } },
		new Object[] { "suffix:f", new String[] { "suffix:g" } }
	};
	
	@Test
	public void testOneshotPessimistic() throws WalaException, Error, CancelException {
		runTest("tests/fieldbased/oneshot.js", assertionsForOneShot, BuilderType.PESSIMISTIC);
	}
	
  @Test
  public void testOneshotOptimistic() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/oneshot.js", assertionsForOneShot, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testOneshotWorklist() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/oneshot.js", assertionsForOneShot, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final Object[][] assertionsForCallbacks = new Object[][] {
		new Object[] { ROOT, new String[] { "suffix:callbacks.js" } },
		new Object[] { "suffix:callbacks.js", new String[] { "suffix:f" } },
		new Object[] { "suffix:f", new String[] { "suffix:k", "suffix:n" } },
		new Object[] { "suffix:k", new String[] { "suffix:l", "suffix:p" } }
	};
	
	@Test
	public void testCallbacksOptimistic() throws WalaException, Error, CancelException {
		runTest("tests/fieldbased/callbacks.js", assertionsForCallbacks, BuilderType.OPTIMISTIC_WORKLIST);
	}
	
  @Test
  public void testCallbacksWorklist() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/callbacks.js", assertionsForCallbacks, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final Object[][] assertionsForLexical = new Object[][] {
		new Object[] { "suffix:h", new String[] { "suffix:g" } }
	};
	
	@Test
	public void testLexicalPessimistic() throws WalaException, Error, CancelException {
		runTest("tests/fieldbased/lexical.js", assertionsForLexical, BuilderType.PESSIMISTIC);
	}
	
  @Test
  public void testLexicalOptimistic() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/lexical.js", assertionsForLexical, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testLexicalWorklist() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/lexical.js", assertionsForLexical, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final Object[][] assertionsForReflectiveCall = new Object[][] {
		new Object[] { "suffix:h", new String[] { "suffix:Function_prototype_call", "suffix:Function_prototype_apply" } },
    new Object[] { "suffix:Function_prototype_call", new String[] { "suffix:f" } },
    new Object[] { "suffix:Function_prototype_apply", new String[] { "suffix:x" } },
		new Object[] { "suffix:f", new String[] { "suffix:k" } }
	};
	
	@Test
	public void testReflectiveCallOptimistic() throws WalaException, Error, CancelException {
		runTest("tests/fieldbased/reflective_calls.js", assertionsForReflectiveCall, BuilderType.OPTIMISTIC);
	}
	
  @Test
  public void testReflectiveCallWorklist() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/reflective_calls.js", assertionsForReflectiveCall, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final Object[][] assertionsForNew = new Object[][] {
	  new Object[] { "suffix:new.js", new String[] { "suffix:g", "suffix:f" } },
	  new Object[] { "suffix:g", new String[] { "!suffix:k" } }
	};
	
	@Test
	public void testNewOptimistic() throws WalaException, Error, CancelException {
	  runTest("tests/fieldbased/new.js", assertionsForNew, BuilderType.OPTIMISTIC);
	}
  
  @Test
  public void testNewWorklist() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/new.js", assertionsForNew, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final Object[][] assertionsForCallbacks2 = new Object[][] {
    new Object[] { "suffix:callbacks2.js", new String[] { "suffix:g" } },
    new Object[] { "suffix:g", new String[] { "suffix:k", "!suffix:l" } }
  };
  
  @Test
  public void testCallbacks2Optimistic() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/callbacks2.js", assertionsForCallbacks2, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testCallbacks2Worklist() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/callbacks2.js", assertionsForCallbacks2, BuilderType.OPTIMISTIC_WORKLIST);
  }

  // @Test
  public void testBug2979() throws WalaException, Error, CancelException {
    System.err.println(runTest("pages/2979.html", new Object[][]{}, BuilderType.PESSIMISTIC, BuilderType.OPTIMISTIC, BuilderType.OPTIMISTIC_WORKLIST));
  }

}
