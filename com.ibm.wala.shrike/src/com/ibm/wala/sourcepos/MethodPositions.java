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
 * MethodPositions.java
 *
 * Created on 23. Mai 2005, 14:49
 */

package com.ibm.wala.sourcepos;

import java.io.DataInputStream;
import java.io.IOException;

import com.ibm.wala.sourcepos.InvalidRangeException.Cause;

/**
 * This class represents the MethodPositions attribute.
 * 
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public final class MethodPositions extends PositionsAttribute {

  /** Stores the attribute name of this attribute */
  public static final String ATTRIBUTE_NAME = "joana.sourceinfo.MethodPositions";

  private static final String ERR_COLUMN_ZERO = "Error in MethodPositions attribute: Invalid column number in %1$s.";

  private static final String ERR_LINE_ZERO = "Error in MethodPositions attribute: Invalid line number in %1$s.";

  private static final String ERR_RANGE_UNDEFINED = "Error in MethodPositions attribute: %1$s and %2$s are undefined.";

  private static final String ERR_SET_RANGE_UNDEFINED = "Error in MethodPositions attribute: Invalid positions, so %1$s and %2$s are set undefined.";

  private static final String ERR_POSITION_UNDEFINED = "Error in MethodPositions attribute: %1$s is undefined.";

  private static final String ERR_END_BEFORE_START = "Error in MethodPositions attribute: %2$s (%4$s) is before %1$s (%3$s).";

  private static final String ERR_UNKNOWN_REASON = "Error in MethodPositions attribute: unknown reason %1$s.";

  private static final String WARN_INVALID_BLOCK_END = "Warning in MethodPositions attribute: Invalid method block end position.";

  private static final String WARN_PARAMETER_NOT_IN_DECLARATION = "Warning in MethodPositions attribute: Parameter not in the declaration range.";

  /** positions of the method declaration */
  private Range declaration;
  /** positions of the method parameters */
  private Range parameter;
  /** positions of the method block */
  private Range block_end;

  /**
   * Creates a new instance of MethodPositions
   * 
   * @param data
   *          the byte array containing the attribute
   * @throws IOException
   *           if the attribute can't be read.
   */
  public MethodPositions(byte[] data) throws IOException {
    super(data);
    if (Debug.PRINT_CHARACTER_RANGE_TABLE) {
      Debug.info("MethodPositions found: ");
      Debug.info(toString());
    }
  }

  @Override
  protected final void readData(DataInputStream in) throws IOException {
    declaration = readRange(in, "declaration_start", "declaration_end", false);
    parameter = readRange(in, "parameter_start", "parameter_end", true);
    block_end = readRange(in, "block_end_start", "block_end_end", false);
    if (!parameter.isUndefined()
        && (!declaration.getStartPosition().isBefore(parameter.getStartPosition()) || !parameter.getEndPosition().isBefore(
            declaration.getEndPosition())))
      Debug.warn(WARN_PARAMETER_NOT_IN_DECLARATION);
    if (!declaration.getEndPosition().isBefore(block_end.getStartPosition()))
      Debug.warn(WARN_INVALID_BLOCK_END);
  }

  /**
   * Reads a range from the input stream.
   * 
   * @param in
   *          the input stream
   * @param startVarName
   *          the variable name for the start position
   * @param endVarName
   *          the variable name for the end position
   * @param undefinedAllowed
   *          {@code true} if the range may be undefined.
   * @return the range
   * @throws IOException
   *           if the input stream cannot be read
   */
  private static Range readRange(DataInputStream in, String startVarName, String endVarName, boolean undefinedAllowed) throws IOException {
    boolean valid = true;
    Range range = null;
    Position start = null;
    Position end = null;
    try {
      start = readPosition(in, startVarName);
    } catch (InvalidPositionException e) {
      valid = false;
    }
    try {
      end = readPosition(in, endVarName);
    } catch (InvalidPositionException e) {
      valid = false;
    }
    if (valid) {
      try {
        range = new Range(start, end);
      } catch (InvalidRangeException e) {
        final Cause thisCause = e.getThisCause();
        switch (thisCause) {
        case END_BEFORE_START:
          Debug.warn(ERR_END_BEFORE_START, startVarName, endVarName, start, end);
          break;
        case START_UNDEFINED:
          Debug.warn(ERR_POSITION_UNDEFINED, startVarName);
          break;
        case END_UNDEFINED:
          Debug.warn(ERR_POSITION_UNDEFINED, endVarName);
          break;
        default:
          Debug.warn(ERR_UNKNOWN_REASON, thisCause);
        }
      }
    }
    if (range == null) {
      range = new Range();
      Debug.warn(ERR_SET_RANGE_UNDEFINED, startVarName, endVarName);
    }
    if (range.isUndefined() && !undefinedAllowed) {
      Debug.warn(ERR_RANGE_UNDEFINED, startVarName, endVarName);
    }

    return range;
  }

  /**
   * Reads a position from the input stream.
   * 
   * @param in
   *          the input stream
   * @param varName
   *          the variable name for this position
   * @throws IOException
   *           if the input stream cannot be read
   * @throws InvalidPositionException
   *           if the read position is invalid
   */
  private static Position readPosition(DataInputStream in, String varName) throws IOException, InvalidPositionException {
    Position pos = null;
    try {
      pos = new Position(in.readInt());
    } catch (InvalidPositionException e) {
      switch (e.getThisCause()) {
      case LINE_NUMBER_ZERO:
        Debug.warn(ERR_LINE_ZERO, varName);
        throw e;
      case COLUMN_NUMBER_ZERO:
        Debug.warn(ERR_COLUMN_ZERO, varName);
        throw e;
      default:
        assert false;
      }
    }
    return pos;
  }

  /**
   * Returns the source position range of the method declaration.
   * 
   * @return the source position range of the method declaration
   */
  public final Range getHeaderInfo() {
    return declaration;
  }

  /**
   * Returns the source position range of the method parameter declaration.
   * 
   * @return the source position range of the method parameter declaration
   */
  public final Range getMethodInfo() {
    return parameter;
  }

  /**
   * Returns the source position range of the end of the method block.
   * 
   * @return the source position range of the end of the method block
   */
  public final Range getFooterInfo() {
    return block_end;
  }

  @Override
  public String toString() {
    return "header: " + declaration + " params: " + parameter + " footer:" + block_end;
  }
}
