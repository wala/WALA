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

import org.eclipse.core.resources.ResourcesPlugin;

import com.ibm.wala.cast.tree.CAst;

public class NativeBridge {

  protected final CAst Ast;

  protected static boolean isInitialized;
  
  protected NativeBridge(CAst Ast) {
    this.Ast = Ast;
  }

  protected static native void initialize();
  
  private static boolean amRunningInEclipse() {
    try {
      return ResourcesPlugin.getWorkspace() != null;
    } catch (IllegalStateException e) {
      return false;
    } catch (Error e) {
      return false;
    }
  }
  
  static {
    isInitialized = false;
    if (amRunningInEclipse()) {
      System.loadLibrary("cast");
      initialize();
      isInitialized = true;
    }
  }
}
