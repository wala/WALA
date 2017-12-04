/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
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
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Check properties of a call to clone() in RTA
 */
public class CloneTest extends WalaTestCase {

  @Test public void testClone() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope, cha);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildRTA(options, new AnalysisCacheImpl(),cha, scope);

    // Find node corresponding to java.text.MessageFormat.clone()
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/text/MessageFormat");
    MethodReference m = MethodReference.findOrCreate(t, "clone", "()Ljava/lang/Object;");
    CGNode node = cg.getNodes(m).iterator().next();

    // Check there's exactly one target for each super call in
    // MessageFormat.clone()
    for (CallSiteReference site : Iterator2Iterable.make(node.iterateCallSites())) {
      if (site.isSpecial()) {
        if (site.getDeclaredTarget().getDeclaringClass().equals(TypeReference.JavaLangObject)) {
          Set<CGNode> targets = cg.getPossibleTargets(node, site);
          if (targets.size() != 1) {
            System.err.println(targets.size() + " targets found for " + site);
            for (CGNode cgNode : targets) {
              System.err.println("  " + cgNode);
            }
            Assert.fail("found " + targets.size() + " targets for " + site + " in " + node);
          }
        }
      }
    }
  }
}
