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
package com.ibm.wala.util.warnings;

import com.ibm.wala.util.debug.Assertions;

/**
 * A warning message. These are ordered first by severity, and then by lexicographic order.
 */
public abstract class Warning implements Comparable {

  public final static byte MILD = 0;

  public final static byte MODERATE = 1;

  public final static byte SEVERE = 2;

  public final static byte CLIENT_MILD = 3;

  public final static byte CLIENT_MODERATE = 4;

  public final static byte CLIENT_SEVERE = 5;

  public final static byte N_LEVELS = 6;

  private byte level;

  public Warning(byte level) {
    this.level = level;
  }

  public Warning() {
    this.level = MILD;
  }

  /*
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   * 
   * @throws ClassCastException if o is not a Warning
   */
  public int compareTo(Object o) throws ClassCastException {
    if (o == null) {
      return -1;
    }
    Warning other = (Warning) o;
    if (level < other.level) {
      return -1;
    } else if (level > other.level) {
      return 1;
    } else {
      return getMsg().compareTo(other.getMsg());
    }
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof Warning) {
      Warning other = (Warning) obj;
      return (getMsg().equals(other.getMsg()) && getLevel() == other.getLevel());
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    return 1499 * getMsg().hashCode() + getLevel();
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("[").append(severityString()).append("] ");
    result.append(getMsg());
    return result.toString();
  }

  protected String severityString() {
    switch (level) {
    case MILD:
      return ("mild");
    case MODERATE:
      return ("Moderate");
    case SEVERE:
      return ("SEVERE");
    case CLIENT_MILD:
      return ("Client mild");
    case CLIENT_MODERATE:
      return ("Client moderate");
    case CLIENT_SEVERE:
      return ("Client severe");
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public byte getLevel() {
    return level;
  }

  /**
   * Must return the same String always -- this is required by the implementation of hashCode.
   */
  public abstract String getMsg();

  public void setLevel(byte b) {
    level = b;
  }
}
