/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/*
 * Range.java
 *
 * Created on 21. Juni 2005, 12:20
 */

package com.ibm.wala.sourcepos;

/**
 * This class represents a range in the source file.
 * 
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public class Range {

  /** stores the start position */
  private Position start;
  /** stores the end position */
  private Position end;

  /**
   * Creates an empty range.
   */
  Range() {
    start = new Position();
    end = new Position();
  }

  /**
   * Creates a new instance of Range with the given start and end position.
   * 
   * @param start
   *          the start position
   * @param end
   *          the end position
   * @throws InvalidRangeException
   *           if end is before start or if the range is not undefined and start
   *           or end is undefined.
   */
  Range(Position start, Position end) throws InvalidRangeException {
    if (start == null)
      throw new InvalidRangeException(InvalidRangeException.Cause.START_UNDEFINED);
    if (end == null)
      throw new InvalidRangeException(InvalidRangeException.Cause.END_UNDEFINED);
    if (end.isBefore(start))
      throw new InvalidRangeException(InvalidRangeException.Cause.END_BEFORE_START);
    else if (start.isUndefined() && !end.isUndefined())
      throw new InvalidRangeException(InvalidRangeException.Cause.START_UNDEFINED);
    else if (!start.isUndefined() && end.isUndefined())
      throw new InvalidRangeException(InvalidRangeException.Cause.END_UNDEFINED);
    this.start = start;
    this.end = end;
  }

  /**
   * Returns whether this range is undefined.
   * 
   * @return whether this range is undefined
   */
  boolean isUndefined() {
    return start.isUndefined();
  }

  /**
   * Tests whether this range is within the given range. Returns false if a
   * range is undefined or {@code r} is null.
   * 
   * @param r
   *          the range to test with
   * @return {@code true} if this range is within the given range
   */
  boolean isWithin(Range r) {
    return (r != null) && !start.isBefore(r.start) && !r.end.isBefore(end);
  }

  /**
   * Returns this range as an array of integers.
   * 
   * @return an array with the following entries: start line number, start
   *         column number, end line number, end column number
   */
  int[] toArray() {
    return new int[] { start.getLine(), start.getColumn(), end.getLine(), end.getColumn() };
  }

  /**
   * Returns the start position.
   * 
   * @return the start position
   */
  public Position getStartPosition() {
    return start;
  }

  /**
   * Returns the end position.
   * 
   * @return the end position
   */
  public Position getEndPosition() {
    return end;
  }

  @Override
  public String toString() {
    return (start.isUndefined() ? "<undefined>" : "(" + getStartPosition() + ") - (" + getEndPosition() + ")");
  }
}
