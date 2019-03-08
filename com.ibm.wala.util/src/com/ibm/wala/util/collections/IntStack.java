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
package com.ibm.wala.util.collections;

import java.util.Arrays;

/** A stack of integer primitives. This should be more efficient than a java.util.Stack */
public class IntStack {

  /** Comment for {@code top} */
  private int top = -1;
  /** Comment for {@code state} */
  private int state[] = new int[0];

  public void push(int i) {
    if (state.length <= (top + 1)) {
      state = Arrays.copyOf(state, state.length * 2 + 1);
    }

    state[++top] = i;
  }

  /** @return the int at the top of the stack */
  public int peek() {
    return state[top];
  }

  /**
   * pop the stack
   *
   * @return the int at the top of the stack
   */
  public int pop() {
    return state[top--];
  }

  /** @return true iff the stack is empty */
  public boolean isEmpty() {
    return top == -1;
  }

  /** @return the number of elements in the stack. */
  public int size() {
    return top + 1;
  }

  /** @return the ith int from the bottom of the stack */
  public int get(int i) {
    if (i < 0 || i > top) {
      throw new IndexOutOfBoundsException();
    }
    return state[i];
  }
}
