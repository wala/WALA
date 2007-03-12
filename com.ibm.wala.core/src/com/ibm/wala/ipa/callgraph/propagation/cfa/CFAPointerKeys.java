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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ArrayInstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKeyWithFilter;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.util.debug.Assertions;

public class CFAPointerKeys implements PointerKeyFactory {

  public CFAPointerKeys() {
  }

  public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
    if (Assertions.verifyAssertions) {
      if (valueNumber <= 0) {
        Assertions._assert(valueNumber > 0, "illegal value number: " + valueNumber + " in " + node);
      }
    }
    return new LocalPointerKey(node, valueNumber);
  }

  public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, FilteredPointerKey.TypeFilter filter) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(filter != null);
      if (valueNumber <= 0) {
        Assertions._assert(valueNumber > 0, "illegal value number: " + valueNumber + " in " + node);
      }
    }
    // TODO: add type filters!
    return new LocalPointerKeyWithFilter(node, valueNumber, filter);
  }

  public PointerKey getPointerKeyForReturnValue(CGNode node) {
    return new ReturnValueKey(node);
  }

  public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
    return new ExceptionReturnValueKey(node);
  }

  public PointerKey getPointerKeyForStaticField(IField f) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(f != null, "null f");
    }
    return new StaticFieldKey(f);
  }

  public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
    assert field != null;
    return new InstanceFieldKey(I, field);
  }

  public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
    return new ArrayInstanceKey(I);
  }
}
