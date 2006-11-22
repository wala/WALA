/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.internationalization;


/**
 * Main utility class used as an entry point to access an 
 * {@link com.ibm.wala.util.internationalization.IStringBundle} instance.
 * Each project has to define its own StringBundle class to get access to their own properties file.
 * 
 * @author egeay
 */
public final class StringBundle {
  
  /**
   * Returns an unique instance of {@link com.ibm.wala.util.internationalization.IStringBundle}
   * implementation.
   */
  public static IStringBundle getInstance() {
    return INSTANCE;
  }
  
  //--- Private code
  
  private static final IStringBundle INSTANCE = new GenericStringBundle( 
      "WalaUtilMessages", //$NON-NLS-1$
      StringBundle.class.getClassLoader() );
  
  private StringBundle() {}

}
