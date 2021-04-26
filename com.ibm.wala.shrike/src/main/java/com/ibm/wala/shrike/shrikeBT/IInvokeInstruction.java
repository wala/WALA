/*
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.shrike.shrikeBT;

/**
 * @author sfink
 *     <p>Basic functionality any invoke instruction should provide
 */
public interface IInvokeInstruction extends IInstruction {

  /** @return one of BytecodeConstants.INVOKE[SPECIAL|VIRTUAL|STATIC|INTERFACE] */
  IDispatch getInvocationCode();

  String getMethodSignature();

  String getMethodName();

  String getClassType();

  public interface IDispatch {

    boolean hasImplicitThis();
  }

  public static enum Dispatch implements IDispatch {
    VIRTUAL,
    SPECIAL,
    INTERFACE,
    STATIC {
      @Override
      public boolean hasImplicitThis() {
        return false;
      }
    };

    @Override
    public boolean hasImplicitThis() {
      return true;
    }
  }
}
