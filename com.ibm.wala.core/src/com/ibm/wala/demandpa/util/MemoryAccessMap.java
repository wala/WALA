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
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

public interface MemoryAccessMap {

  /**
   * @return {@link Collection}&lt;{@link MemoryAccess}&gt;
   */
  public Collection<MemoryAccess> getFieldReads(PointerKey baseRef, IField field);

  /**
   * @return {@link Collection}&lt;{@link MemoryAccess}&gt;
   */
  public Collection<MemoryAccess> getFieldWrites(PointerKey baseRef, IField field);
  
  public Collection<MemoryAccess> getArrayReads(PointerKey arrayRef);

  public Collection<MemoryAccess> getArrayWrites(PointerKey arrayRef);

  public Collection<MemoryAccess> getStaticFieldReads(IField field);

  public Collection<MemoryAccess> getStaticFieldWrites(IField field);
  
  /**
   * get the heap model used in this memory access map
   */
  public HeapModel getHeapModel();  
}
