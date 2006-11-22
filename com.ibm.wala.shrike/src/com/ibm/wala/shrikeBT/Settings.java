/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT;

/**
 * Settings for the whole ShrikeBT package.
 */
public class Settings {
  /**
   * Used to enable various checks if in DEBUG mode.
   */
  public static final boolean DEBUG = false;

  public int hashCode() {
    throw new Error("Settings.hashCode(): define a custom hash code to avoid non-determinancy");
  }
}