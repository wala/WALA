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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/** A synthetic method that calls all class initializers */
public class FakeWorldClinitMethod extends AbstractRootMethod {

  private static final Atom name = Atom.findOrCreateAsciiAtom("fakeWorldClinit");

  private static final Descriptor descr =
      Descriptor.findOrCreate(new TypeName[0], TypeReference.VoidName);

  public FakeWorldClinitMethod(
      final IClass fakeRootClass, AnalysisOptions options, IAnalysisCacheView cache) {
    super(
        MethodReference.findOrCreate(fakeRootClass.getReference(), name, descr),
        fakeRootClass.getClassHierarchy(),
        options,
        cache);
  }
}
