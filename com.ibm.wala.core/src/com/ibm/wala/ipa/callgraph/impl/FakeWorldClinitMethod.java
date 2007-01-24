/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.Atom;

/**
 * 
 * A synthetic method that calls all class initializers
 * 
 * @author sfink
 */
public class FakeWorldClinitMethod extends AbstractRootMethod {

  private static final Atom name = Atom.findOrCreateAsciiAtom("fakeWorldClinit");

  private static final MethodReference worldClinitMethod = MethodReference.findOrCreate(FakeRootClass.FAKE_ROOT_CLASS, name, Descriptor
      .findOrCreateUTF8("()V"));
  
  public FakeWorldClinitMethod(final ClassHierarchy cha, AnalysisOptions options) {
    super(worldClinitMethod, cha, options);
  }
}
