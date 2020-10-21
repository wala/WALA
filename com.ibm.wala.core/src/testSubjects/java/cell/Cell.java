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
package cell;

public class Cell<T> {
  private T field;

  public Cell(T t1) {
    field = t1;
  }

  public void set(T t2) {
    this.field = t2;
  }
}
