/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.shrike.shrikeBT;

public interface IUnaryOpInstruction extends IInstruction {

  public interface IOperator {}

  public static enum Operator implements IOperator {
    NEG;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  IOperator getOperator();

  String getType();
}
