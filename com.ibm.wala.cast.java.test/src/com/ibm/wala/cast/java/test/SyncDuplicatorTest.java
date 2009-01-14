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

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public abstract class SyncDuplicatorTest extends IRTests {

  public SyncDuplicatorTest(String name) {
    super(name, null);
  }

  protected final static CallSiteReference testMethod = CallSiteReference.make(0, MethodReference.findOrCreate(TypeReference
      .findOrCreate(EclipseProjectPath.SOURCE_REF, TypeName.string2TypeName("LMonitor2")), Atom.findOrCreateUnicodeAtom("test"),
      Descriptor.findOrCreateUTF8("(Ljava/lang/Object;)Z")), IInvokeInstruction.Dispatch.STATIC);

  public void testMonitor2() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

}
