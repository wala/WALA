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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;

/** An object that abstracts how to model pointers in the heap. */
public interface PointerKeyFactory {
  /**
   * @return the PointerKey that acts as a representative for the class of pointers that includes
   *     the local variable identified by the value number parameter.
   */
  PointerKey getPointerKeyForLocal(CGNode node, int valueNumber);

  /**
   * @return the PointerKey that acts as a representative for the class of pointers that includes
   *     the local variable identified by the value number parameter.
   */
  FilteredPointerKey getFilteredPointerKeyForLocal(
      CGNode node, int valueNumber, FilteredPointerKey.TypeFilter filter);

  /**
   * @return the PointerKey that acts as a representative for the class of pointers that includes
   *     the return value for a node
   */
  PointerKey getPointerKeyForReturnValue(CGNode node);

  /**
   * @return the PointerKey that acts as a representative for the class of pointers that includes
   *     the exceptional return value
   */
  PointerKey getPointerKeyForExceptionalReturnValue(CGNode node);

  /**
   * @return the PointerKey that acts as a representative for the class of pointers that includes
   *     the contents of the static field
   */
  PointerKey getPointerKeyForStaticField(IField f);

  /**
   * @return the PointerKey that acts as a representation for the class of pointers that includes
   *     the given instance field.
   */
  PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field);

  /**
   * TODO: expand this API to differentiate between different array indices
   *
   * @param I an InstanceKey representing an abstract array
   * @return the PointerKey that acts as a representation for the class of pointers that includes
   *     the given array contents.
   */
  PointerKey getPointerKeyForArrayContents(InstanceKey I);
}
