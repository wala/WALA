/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.shrike.shrikeBT;

public interface IMemoryOperation {

  /**
   * Denotes whether this instruction is taking the address of whatever location it refers to.
   *
   * @return whether this instruction is taking the address of a location
   */
  public boolean isAddressOf();
}
