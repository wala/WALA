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
package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.*;

public class NativeBridge {

  protected final CAst Ast;

  protected NativeBridge(CAst Ast) {
    this.Ast = Ast;
  }

  protected static native void initialize();
  
  /*
   *  trying to modularize shared library loading like this seems to 
   * cause trouble on certain VMs.  (guess which? :)
   *
  static {
    System.loadLibrary("cast");
    initialize();
  }
  */
}
