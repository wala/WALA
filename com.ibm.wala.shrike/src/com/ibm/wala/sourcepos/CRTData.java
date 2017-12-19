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
 * CRTData.java
 *
 * Created on 7. Juni 2005, 11:48
 */

package com.ibm.wala.sourcepos;

import com.ibm.wala.sourcepos.InvalidRangeException.Cause;

/**
 * This class represents an entry in the CharacterRangeTable.
 * 
 * @see CRTFlags
 * @see CRTable
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public final class CRTData {

  private static final String WARN_INVALID_PC_RANGE = "Error at CRT entry %1$s: the program counter start index (%2$s) must be greater or equal than the end index (%3$s).";
  private static final String WARN_INVALID_START_LINE_NUMBER = "Error at CRT entry %1$s: the line number of the source start position must not be 0.";
  private static final String WARN_INVALID_START_COLUMN_NUMBER = "Error at CRT entry %1$s: the column number of the source start position not be 0.";
  private static final String WARN_INVALID_END_LINE_NUMBER = "Error at CRT entry %1$s: the line number of the source end position must not be 0.";
  private static final String WARN_INVALID_END_COLUMN_NUMBER = "Error at CRT entry %1$s: the column number of the source end position must not be 0.";
  private static final String WARN_END_BEFORE_START = "Error at CRT entry %1$s: the source end position (%3$s) is before the source start position (%2$s).";
  private static final String WARN_START_UNDEFINED = "Error at CRT entry %1$s: the source start position is undefined.";
  private static final String WARN_END_UNDEFINED = "Error at CRT entry %1$s: the source end position is undefined.";

  /** start index in the code array of the code attribute */
  private final int pc_start_index;
  /** end index in the code array of the code attribute */
  private final int pc_end_index;
  /** positions in the source file */
  private final Range source_positions;
  /**
   * flags
   * 
   * @see CRTFlags
   */
  private final CRTFlags flags;

  /**
   * Creates a new instance of CRTData. {@code source_start_position} and
   * {@code source_end_position} can be {@code 0} to show that a range in the
   * code array of the code attribute has no source positions.
   * 
   * @param pc_start_index
   *          start index in the code array of the code attribute as unsigned
   *          short
   * @param pc_end_index
   *          end index in the code array of the code attribute as unsigned
   *          short
   * @param source_start_position
   *          start position in the source file as unsigned int
   * @param source_end_position
   *          end position in the source file as unsigned int
   * @param flags
   *          flags defined in {@link CRTFlags}
   * @throws InvalidCRTDataException
   *           if a parameter violates one of the following conditions:
   *           <ul>
   *           <li>{@code pc_start_index < pc_end_index}</li>
   *           <li>{@code source_start_position} is a valid position.</li>
   *           <li>{@code source_end_position} is a valid position.</li>
   *           <li>{@code source_start_position <= source_end_position}</li>
   *           <li>{@code source_start_position} and {@code source_end_position}
   *           must be a valid range.</li>
   *           <li>{@code flags} must contain valid flags.</li>
   *           </ul>
   */
  CRTData(short pc_start_index, short pc_end_index, int source_start_position, int source_end_position, short flags)
      throws InvalidCRTDataException {
    this.pc_start_index = pc_start_index & 0xFFFF;
    this.pc_end_index = pc_end_index & 0xFFFF;
    if (pc_start_index > pc_end_index)
      throw new InvalidCRTDataException(WARN_INVALID_PC_RANGE, this.pc_start_index, this.pc_end_index);

    Position source_start = null;
    try {
      source_start = new Position(source_start_position);
    } catch (InvalidPositionException e) {
      switch (e.getThisCause()) {
      case LINE_NUMBER_ZERO:
        throw new InvalidCRTDataException(WARN_INVALID_START_LINE_NUMBER);
      case COLUMN_NUMBER_ZERO:
        throw new InvalidCRTDataException(WARN_INVALID_START_COLUMN_NUMBER);
      default:
        assert false;
      }
    }

    Position source_end = null;
    try {
      source_end = new Position(source_end_position);
    } catch (InvalidPositionException e) {
      switch (e.getThisCause()) {
      case LINE_NUMBER_ZERO:
        throw new InvalidCRTDataException(WARN_INVALID_END_LINE_NUMBER);
      case COLUMN_NUMBER_ZERO:
        throw new InvalidCRTDataException(WARN_INVALID_END_COLUMN_NUMBER);
      default:
        assert false;
      }
    }

    Range range = null;
    try {
      range = new Range(source_start, source_end);
    } catch (InvalidRangeException e) {
      final Cause cause = e.getThisCause();
      switch (cause) {
      case END_BEFORE_START:
        throw new InvalidCRTDataException(WARN_END_BEFORE_START, source_start.toString(), source_end.toString());
      case START_UNDEFINED:
        throw new InvalidCRTDataException(WARN_START_UNDEFINED);
      case END_UNDEFINED:
        throw new InvalidCRTDataException(WARN_END_UNDEFINED);
      default:
        throw new UnsupportedOperationException(String.format("cannot convert %s into an InvalidCRTDataException", cause));
      }
    } finally {
      this.source_positions = range;
    }

    this.flags = new CRTFlags(flags);
  }

  public final CRTFlags getFlags() {
    return this.flags;
  }

  /**
   * Tests whether the given index lies within the range of this data.
   * 
   * @param pc
   *          the index to test
   * @return whether the given index lies within the range of this data or not.
   */
  public final boolean isInRange(int pc) {
    return pc_start_index <= pc && pc <= pc_end_index;
  }

  /**
   * Tests whether the given data is consistently with this data. To be
   * consistently with another data this data has to be equal or more precise.
   * Otherwise the datas are contradictory.
   * 
   * @param d
   *          the data to test with
   * @return whether the given data is consistently.
   */
  public final boolean matches(CRTData d) {
    return d != null && (isMorePrecise(d) || d.isMorePrecise(this));
  }

  /**
   * Tests whether this data is equal to or more precise than the given data.
   * This data is equal or more precise if the program counter range and the
   * source range lie within the range of the given data. If this data or the
   * parameter have no source positions, only program counter range decides.
   * 
   * @param d
   *          the data to test with
   * @return whether this data is equal to or more precise than the given one.
   */
  public final boolean isMorePrecise(CRTData d) {
    return d != null && pc_start_index >= d.pc_start_index && pc_end_index <= d.pc_end_index
        && (source_positions.isWithin(d.source_positions) || hasNoPosition() || d.hasNoPosition());
  }

  /**
   * Returns {@code true} if this data has no source position.
   * 
   * @return {@code true} if this data has no source position.
   */
  private boolean hasNoPosition() {
    return source_positions.isUndefined();
  }

  /**
   * Returns the source positions.
   * 
   * @return The returned array consists of four positions: the start line
   *         number, the start column number, the end line number, the end
   *         column number
   */
  public final Range getSourceInfo() {
    return source_positions;
  }

  @Override
  public String toString() {
    return "(Range [pc]: " + pc_start_index + "-" + pc_end_index + ") => " + source_positions.toString();
  }
}
