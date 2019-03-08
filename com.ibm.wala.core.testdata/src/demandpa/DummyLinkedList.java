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

public class DummyLinkedList {

  static class Element {
    Element next;
    Object data;
  }

  Element head = null;

  public void add(Object o) {
    if (head == null) {
      head = new Element();
      head.data = o;
    } else {
      Element tmp = head;
      while (tmp.next != null) {
        tmp = tmp.next;
      }
      Element newElement = new Element();
      newElement.data = o;
      tmp.next = newElement;
    }
  }

  public Object get(int ind) {
    Element tmp = head;
    for (int i = 0; i < ind; i++) {
      tmp = tmp.next;
    }
    return tmp.data;
  }

  public Iter iterator() {
    return new Iter() {

      @Override
      public Object next() {
        // just return some arbitrary element, from the point of view of flow-insensitive points-to
        // analysis
        Element tmp = head;
        while (tmp.data == tmp.next) { // shouldn't be able to interpret this condition
          tmp = tmp.next;
        }
        return tmp.data;
      }
    };
  }
}
