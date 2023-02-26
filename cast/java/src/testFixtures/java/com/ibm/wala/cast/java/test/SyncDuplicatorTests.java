/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import org.junit.Test;

public abstract class SyncDuplicatorTests extends IRTests {

  public SyncDuplicatorTests() {
    super(null);
  }

  protected static final CallSiteReference testMethod =
      CallSiteReference.make(
          0,
          MethodReference.findOrCreate(
              TypeReference.findOrCreate(
                  JavaSourceAnalysisScope.SOURCE, TypeName.string2TypeName("LMonitor2")),
              Atom.findOrCreateUnicodeAtom("test"),
              Descriptor.findOrCreateUTF8(Language.JAVA, "(Ljava/lang/Object;)Z")),
          IInvokeInstruction.Dispatch.STATIC);

  @Test
  public void testMonitor2() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> result =
        runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
    System.err.println(result.fst);
  }
}
