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
package recurse;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author sfink
 *     <p>A simple exercise in recursive data structures.
 */
public class NList implements Collection<Object> {

  final int value;
  final NList next;

  public NList(int value) {
    this.value = value;
    if (value > 0) {
      this.next = new NList(value - 1);
    } else {
      this.next = null;
    }
  }

  public static void main(String[] args) {
    new NList(100);
  }

  @Override
  public int size() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public boolean isEmpty() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean contains(Object o) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Iterator<Object> iterator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object[] toArray() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean add(Object o) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean remove(Object o) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean addAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void clear() {
    // TODO Auto-generated method stub

  }

  @Override
  public Object[] toArray(Object[] a) {
    // TODO Auto-generated method stub
    return null;
  }
}
