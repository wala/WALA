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

/** doesn't actually work; just for testing pointer analysis */
public class DummyHashMap {

  Object[] keys = new Object[10];

  Object[] values = new Object[10];

  public void put(Object object, Object object2) {
    int ind = object.hashCode() % keys.length;
    keys[ind] = object;
    values[ind] = object2;
  }

  public Iter keyIter() {
    return new Iter() {

      @Override
      public Object next() {
        return keys[0];
      }
    };
  }

  public Iter valuesIter() {
    return new Iter() {

      @Override
      public Object next() {
        return values[0];
      }
    };
  }

  public Object get(Object key1) {
    return values[0];
  }

  private Iter keyOrValueIter(int type) {
    return type == 0 ? keyIter() : valuesIter();
  }

  public Iter elements() {
    return keyOrValueIter(-1);
  }
}
