/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.demandpa.util;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import java.util.Collection;

public interface MemoryAccessMap {

  /**
   * @return {@link Collection}&lt;{@link MemoryAccess}&gt;
   */
  Collection<MemoryAccess> getFieldReads(PointerKey baseRef, IField field);

  /**
   * @return {@link Collection}&lt;{@link MemoryAccess}&gt;
   */
  Collection<MemoryAccess> getFieldWrites(PointerKey baseRef, IField field);

  Collection<MemoryAccess> getArrayReads(PointerKey arrayRef);

  Collection<MemoryAccess> getArrayWrites(PointerKey arrayRef);

  Collection<MemoryAccess> getStaticFieldReads(IField field);

  Collection<MemoryAccess> getStaticFieldWrites(IField field);

  /** get the heap model used in this memory access map */
  HeapModel getHeapModel();
}
