/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package demandpa;

public class DummyHashSet {

  private final DummyHashMap hashMap = new DummyHashMap();

  public void add(Object object) {
    hashMap.put(object, object);
  }

  public Iter iterator() {
    return hashMap.keyIter();
  }
}
