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

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ecore.java.impl.JavaPackageImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author Julian Dolby
 *
 */
public class PiNodeCallGraphTest extends WalaTestCase {

  static {
    JavaPackageImpl.init();
  }

  public static void main(String[] args) {
    justThisTest(PiNodeCallGraphTest.class);
  }

  private static final String whateverName = TestConstants.PI_TEST_MAIN + "$Whatever";

  private static final String thisName = TestConstants.PI_TEST_MAIN + "$This";

  private static final String thatName = TestConstants.PI_TEST_MAIN + "$That";

  private static final ClassLoaderReference loader = ClassLoaderReference.Application;

  private static final TypeReference whateverRef = TypeReference.findOrCreate(loader, TypeName.string2TypeName(whateverName));

  private static final TypeReference thisRef = TypeReference.findOrCreate(loader, TypeName.string2TypeName(thisName));

  private static final TypeReference thatRef = TypeReference.findOrCreate(loader, TypeName.string2TypeName(thatName));

  private static final MethodReference thisBinaryRef = MethodReference.findOrCreate(thisRef,
      Atom.findOrCreateUnicodeAtom("binary"), Descriptor.findOrCreateUTF8("(" + whateverName + ";)V"));

  private static final MethodReference thatBinaryRef = MethodReference.findOrCreate(thatRef,
      Atom.findOrCreateUnicodeAtom("binary"), Descriptor.findOrCreateUTF8("(" + whateverName + ";)V"));

  private static final MemberReference unary2Ref = MethodReference.findOrCreate(whateverRef,
      Atom.findOrCreateUnicodeAtom("unary2"), Descriptor.findOrCreateUTF8("()V"));

  private CallGraph doGraph(boolean usePiNodes) throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.PI_TEST_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    options.getSSAOptions().setUsePiNodes(usePiNodes);
    warnings = new WarningSet();

    return CallGraphTestUtil.buildZeroCFA(options, cha, scope, warnings);
  }

  private void checkCallAssertions(CallGraph cg, int desiredNumberOfTargets, int desiredNumberOfCalls) {
    int numberOfCalls = 0;
    Set<CGNode> callerNodes = HashSetFactory.make();
    callerNodes.addAll(cg.getNodes(thisBinaryRef));
    callerNodes.addAll(cg.getNodes(thatBinaryRef));
    Assertions._assert(callerNodes.size() == 2);

    for (Iterator<CGNode> nodes = callerNodes.iterator(); nodes.hasNext();) {
      CGNode n = (CGNode) nodes.next();
      for (Iterator<CallSiteReference> sites = n.iterateSites(); sites.hasNext();) {
        CallSiteReference csRef = (CallSiteReference) sites.next();
        if (csRef.getDeclaredTarget().equals(unary2Ref)) {
          numberOfCalls++;
          Assertions._assert(n.getNumberOfTargets(csRef) == desiredNumberOfTargets);
        }
      }
    }

    Assertions._assert(numberOfCalls == desiredNumberOfCalls);
  }

  public void testNoPiNodes() throws ClassHierarchyException {
    checkCallAssertions(doGraph(false), 2, 2);
  }

  public void testPiNodes() throws ClassHierarchyException {
    checkCallAssertions(doGraph(true), 1, 2);
  }

}