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
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPiNodePolicy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.strings.Atom;

/**
 */
public class PiNodeCallGraphTest extends WalaTestCase {


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

  private static CallGraph doGraph(boolean usePiNodes) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.PI_TEST_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    SSAPiNodePolicy policy = usePiNodes ? SSAOptions.getAllBuiltInPiNodes() : null;
    options.getSSAOptions().setPiNodePolicy(policy);

    return CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(new DefaultIRFactory(), options.getSSAOptions()), cha, scope, false);
  }

  private static void checkCallAssertions(CallGraph cg, int desiredNumberOfTargets, int desiredNumberOfCalls, int numLocalCastCallees) {
  
    int numberOfCalls = 0;
    Set<CGNode> callerNodes = HashSetFactory.make();
    callerNodes.addAll(cg.getNodes(thisBinaryRef));
    callerNodes.addAll(cg.getNodes(thatBinaryRef));
    assert callerNodes.size() == 2;

    for (CGNode n : callerNodes) {
      for (CallSiteReference csRef : Iterator2Iterable.make(n.iterateCallSites())) {
        if (csRef.getDeclaredTarget().equals(unary2Ref)) {
          numberOfCalls++;
          assert cg.getNumberOfTargets(n, csRef) == desiredNumberOfTargets;
        }
      }
    }

    
    assert numberOfCalls == desiredNumberOfCalls;

    CGNode localCastNode = cg.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(loader, TestConstants.PI_TEST_MAIN), "localCast", "()V")).iterator().next();
    int actualLocalCastCallees = cg.getSuccNodeCount(localCastNode);
    Assert.assertEquals(numLocalCastCallees, actualLocalCastCallees);
  }

  @Test public void testNoPiNodes() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    checkCallAssertions(doGraph(false), 2, 2, 2);
  }

  @Test public void testPiNodes() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    checkCallAssertions(doGraph(true), 1, 2, 1);
  } 

}
