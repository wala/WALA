/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.client.EclipseProjectSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;

public class JLexTest extends IRTests {

    public JLexTest() {
      super("JLexTest");
    }

    protected EclipseProjectSourceAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors) {
	return new EclipseProjectSourceAnalysisEngine() {
          protected Entrypoints 
            makeDefaultEntrypoints(AnalysisScope scope, ClassHierarchy cha) 
	  {
	    return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE_REF, cha, new String[]{ "LJLex/Main" });
	  }
	};
    }

    protected String singleInputForTest() {
	return "JLex/Main.java";
    }

    public void testJLex() {
      runTest(singleTestSrc(), rtJar, 
	      new String[]{ "LJLex/Main" },
	      new GraphAssertions(),
	      new SourceMapAssertions());
    }

    protected String singlePkgInputForTest(String pkgName) {
	return "";
    }
}
