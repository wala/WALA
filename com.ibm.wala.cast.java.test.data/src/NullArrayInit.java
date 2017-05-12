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
public class NullArrayInit {
  String[] x = {null};

  @SuppressWarnings("unused")
  public static void main(String[] args) {
    new NullArrayInit();
    Object a[] = new Object[] {null,null};
    Object b[] = {null};
    String c[] = {null};
    String d[] = {null,null};
    String e[] = {null,"hello",null};
    String f[] = new String[] {null};
    String g[] = new String[] {null,null,null};
    String j[][] = { {null,null}, {null} };
  }
}
