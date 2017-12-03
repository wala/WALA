/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.callGraph;

import java.io.IOException;
import java.util.function.Function;

import org.junit.Test;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;

public class CHACallGraphTest {
  
  @Test public void testJava_cup() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testCHA(TestConstants.JAVA_CUP, TestConstants.JAVA_CUP_MAIN, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
  }
    
  public static CallGraph testCHA(String scopeFile, final String mainClass, final String exclusionsFile) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return testCHA(scopeFile, exclusionsFile, cha -> Util.makeMainEntrypoints(cha.getScope(), cha, mainClass));
  }
  
  public static CallGraph testCHA(String scopeFile, 
      String exclusionsFile,
      Function<IClassHierarchy, Iterable<Entrypoint>> makeEntrypoints) 
    throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException
  {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(scopeFile, exclusionsFile);
    IClassHierarchy cha = ClassHierarchyFactory.make(scope);
    
    CHACallGraph CG = new CHACallGraph(cha);
    CG.init(makeEntrypoints.apply(cha));
    
    return CG;
  }
  
  public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testCHA(args[0], args.length>1? args[1]: null, "Java60RegressionExclusions.txt");
  }
}
