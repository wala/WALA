/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.demandpa.util;

import java.util.Collection;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

public interface MemoryAccessMap {

  /**
   * @return Collection<FieldAccess>
   */
  public abstract Collection<MemoryAccess> getFieldReads(PointerKey baseRef, IField field);

  /**
   * @return Collection<FieldAccess>
   */
  public abstract Collection<MemoryAccess> getFieldWrites(PointerKey baseRef, IField field);
  
  public abstract Collection<MemoryAccess> getArrayReads(PointerKey arrayRef);

  public abstract Collection<MemoryAccess> getArrayWrites(PointerKey arrayRef);

  public abstract Collection<MemoryAccess> getStaticFieldReads(IField field);

  public abstract Collection<MemoryAccess> getStaticFieldWrites(IField field);
}