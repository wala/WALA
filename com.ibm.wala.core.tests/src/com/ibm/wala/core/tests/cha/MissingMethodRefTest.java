/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.cha;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import java.io.IOException;
import org.junit.Test;

public class MissingMethodRefTest extends WalaTestCase {

  /** Test handling of a method reference to a method that is missing */
  @Test
  public void testMissingMethodRef() throws IOException, ClassHierarchyException, CancelException {
    AnalysisScope scope = null;
    scope =
        AnalysisScopeReader.readJavaScope(
            TestConstants.WALA_TESTDATA,
            (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"),
            DupFieldsTest.class.getClassLoader());
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "LMissingMethodRef");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    // should not throw an NPE
    CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
  }
}
