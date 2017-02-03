/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;

public enum CAstBinaryOp implements IBinaryOpInstruction.IOperator {
    CONCAT, EQ, NE, LT, GE, GT, LE, STRICT_EQ, STRICT_NE;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
}
