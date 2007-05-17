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

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;

public class JLexTest extends IRTests {

  public JLexTest() {
    super("JLexTest");
  }

  protected JavaSourceAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors) {
    return new JavaSourceAnalysisEngine() {
      protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, ClassHierarchy cha) {
        return Util.makeMainEntrypoints(EclipseProjectPath.SOURCE_REF, cha, new String[] { "LJLex/Main" });
      }
    };
  }

  protected String singleInputForTest() {
    return "JLex";
  }

  public void testJLex() {
    runTest(singleTestSrc(), rtJar, new String[] { "LJLex/Main" }, new GraphAssertions(), new SourceMapAssertions(), false);
  }

  protected String singlePkgInputForTest(String pkgName) {
    return "";
  }
}
