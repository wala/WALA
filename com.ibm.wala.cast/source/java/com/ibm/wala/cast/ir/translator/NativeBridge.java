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

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.core.plugin.CorePlugin;

public class NativeBridge {

  protected final CAst Ast;

  protected static boolean isInitialized;
  
  protected NativeBridge(CAst Ast) {
    this.Ast = Ast;
  }

  protected static native void initialize();
  
  static {
    isInitialized = false;
    if (CorePlugin.IS_RESOURCES_BUNDLE_AVAILABLE) {
      System.loadLibrary("cast");
      initialize();
      isInitialized = true;
    }
  }
}
