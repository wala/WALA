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

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.TypeReference;

/** An object that abstracts how to model instances in the heap. */
public interface InstanceKeyFactory {
  /** @return the instance key that represents a particular allocation */
  public abstract InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation);

  /**
   * @return the instance key that represents the array allocated as the dim_th dimension at a
   *     particular allocation
   */
  public abstract InstanceKey getInstanceKeyForMultiNewArray(
      CGNode node, NewSiteReference allocation, int dim);

  /**
   * @return the instance key that represents a constant with value S, when considered as a
   *     particular type
   */
  public abstract <T> InstanceKey getInstanceKeyForConstant(TypeReference type, T S);

  /**
   * @return the instance key that represents the exception of type _type_ thrown by a particular
   *     PEI.
   */
  public abstract InstanceKey getInstanceKeyForPEI(
      CGNode node, ProgramCounter instr, TypeReference type);

  /**
   * @param objType TODO
   * @return the instance key that represents the metadata object obj
   */
  public abstract InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType);
}
