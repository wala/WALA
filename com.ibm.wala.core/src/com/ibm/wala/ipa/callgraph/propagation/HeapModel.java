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

import com.ibm.wala.ipa.cha.IClassHierarchy;
import java.util.Iterator;

/** A {@link HeapModel} embodies how a pointer analysis abstracts heap locations. */
public interface HeapModel extends InstanceKeyFactory, PointerKeyFactory {

  /** @return an Iterator of all PointerKeys that are modeled. */
  Iterator<PointerKey> iteratePointerKeys();

  /** @return the governing class hierarchy for this heap model */
  IClassHierarchy getClassHierarchy();
}
