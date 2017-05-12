/******************************************************************************
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
public class ArrayLiteral1 {
    @SuppressWarnings("unused")
    public static void main(String[] args) {
      ArrayLiteral1 al1= new ArrayLiteral1();
      int[] a= new int[] { 0, 1, 2, 3, 5 };
      Object[] b= new Object[] { null, "hi", new Integer(55), new int[] { 3, 1, 4 } };
    }
}
