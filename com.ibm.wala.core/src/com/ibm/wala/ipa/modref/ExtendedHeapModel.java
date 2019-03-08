/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.modref;

import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

/**
 * An extension of a heap model that returns a {@link PointerKey} to represent an array length field
 */
public interface ExtendedHeapModel extends HeapModel {
  /**
   * @param I an InstanceKey representing an abstract array
   * @return the PointerKey that acts as a representation for the arraylength field of this abstract
   *     array
   */
  PointerKey getPointerKeyForArrayLength(InstanceKey I);
}
