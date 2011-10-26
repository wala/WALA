/*
 * InvalidRangeException.java
 *
 * Created on 21. Juni 2005, 13:36
 */

package com.ibm.wala.sourcepos;

/**
 * An exception for invalid ranges.
 * 
 * @author Siegfried Weber
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
class InvalidRangeException extends Exception {

  private static final long serialVersionUID = 3534258510796557967L;

  /** possible causes for this exception */
  enum Cause {
    END_BEFORE_START, START_UNDEFINED, END_UNDEFINED
  }

  /** the cause for this exception */
  private Cause cause;

  /**
   * Constructs an instance of <code>InvalidRangeException</code> with the
   * specified cause.
   * 
   * @param c
   *          the cause
   */
  InvalidRangeException(Cause c) {
    cause = c;
  }

  /**
   * Returns the cause for this exception.
   * 
   * @return the cause for this exception
   */
  Cause getThisCause() {
    return cause;
  }
}
