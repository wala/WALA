/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util;

/**
 * Platform-specific utility functions.
 */
public class PlatformUtil {

  /**
   * are we running on Mac OS X?
   */
  public static boolean onMacOSX() {
    // String osname = System.getProperty("os.name");
    // return osname.toLowerCase().contains("mac");
    return System.getProperty("mrj.version") != null;
  }

  /**
   * are we running on Linux?
   */
  public static boolean onLinux() {
    String osname = System.getProperty("os.name");
    return osname.equalsIgnoreCase("linux");
  }

  /**
   * are we running on Windows?
   */
  public static boolean onWindows() {
    String osname = System.getProperty("os.name");
    return osname.toLowerCase().contains("windows");
  }

  /**
   * are we running on IKVM?  see http://www.ikvm.net
   */
  public static boolean onIKVM() {
    return "IKVM.NET".equals(System.getProperty("java.runtime.name"));
  }

}
