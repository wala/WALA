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
 * Position.java
 *
 * Created on 20. Juni 2005, 10:32
 */

package com.ibm.wala.sourcepos;

/**
 * Represents a source file position. Source file positions are integers in the
 * format: {@code line-number << LINESHIFT + column-number}
 * 
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public final class Position {

  /** Represents the undefined position. */
  private static final int NOPOS = 0;
  /** bits to shift to get the line number */
  private static final int LINE_SHIFT = 10;
  /** the bit mask of the column number */
  private static final int COLUMN_MASK = 1023;

  /** Stores the position as unsigned integer. */
  private int position;

  /**
   * Creates the undefined position.
   */
  Position() {
    position = NOPOS;
  }

  /**
   * Creates a new instance of Position.
   * 
   * @param position
   *          the position as unsigned integer
   * @throws InvalidPositionException
   *           if the position is not undefined and the line or the column
   *           number is 0
   */
  Position(int position) throws InvalidPositionException {
    this.position = position;
    if (!isUndefined() && getLine() == 0)
      throw new InvalidPositionException(InvalidPositionException.Cause.LINE_NUMBER_ZERO);
    if (!isUndefined() && getColumn() == 0)
      throw new InvalidPositionException(InvalidPositionException.Cause.COLUMN_NUMBER_ZERO);
  }

  /**
   * Creates a new instance of Position.
   * 
   * @param line
   *          the line number
   * @param column
   *          the column number
   * @throws InvalidPositionException
   *           if the line or the column number is out of range or if the
   *           position is not undefined and the line or the column number is 0.
   *           The maximum line number is 4194303. The maximum column number is
   *           1023.
   */
  Position(int line, int column) throws InvalidPositionException {
    if (line < 0 || line >= 4194304) // 4194304 = 2^32 >>> LINE_SHIFT
      throw new InvalidPositionException(InvalidPositionException.Cause.LINE_NUMBER_OUT_OF_RANGE);
    if (column < 0 || column > COLUMN_MASK)
      throw new InvalidPositionException(InvalidPositionException.Cause.COLUMN_NUMBER_OUT_OF_RANGE);
    if (line == 0 && column != 0)
      throw new InvalidPositionException(InvalidPositionException.Cause.LINE_NUMBER_ZERO);
    if (line != 0 && column == 0)
      throw new InvalidPositionException(InvalidPositionException.Cause.COLUMN_NUMBER_ZERO);
    position = (line << LINE_SHIFT) + column;
  }

  /**
   * Returns the line number.
   * 
   * @return the line number
   */
  public final int getLine() {
    return position >>> LINE_SHIFT;
  }

  /**
   * Returns the column number.
   * 
   * @return the column number
   */
  public final int getColumn() {
    return position & COLUMN_MASK;
  }

  /**
   * Tests whether this position is undefined.
   * 
   * @return true if this position is undefined
   */
  public final boolean isUndefined() {
    return position == NOPOS;
  }

  /**
   * Tests whether this position is before the given position. If one of the
   * positions is undefined or {@code p} is null then false is returned.
   * 
   * @param p
   *          the position to test with
   * @return true if this position is greater
   */
  boolean isBefore(Position p) {
    return (p != null) && !isUndefined() && !p.isUndefined() && toLong() < p.toLong();
  }

  /**
   * Converts this position to a signed long variable.
   * 
   * @return this position as signed long
   */
  private long toLong() {
    return position & 0xFFFFFFFFL;
  }

  /**
   * Returns this position as an unsigned integer.
   * 
   * @return this position as unsigned integer
   */
  int toUnsignedInt() {
    return position;
  }

  @Override
  public String toString() {
    return getLine() + ":" + getColumn();
  }
}
