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

public class NativeBridge {

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
