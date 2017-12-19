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

package com.ibm.wala.util.bytecode;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.BytecodeConstants;

/**
 * Provides minimal abstraction layer to a stream of bytecodes from the code attribute of a method.
 */
public class BytecodeStream implements BytecodeConstants {
  private final IMethod method;

  private final IClass declaringClass;

  private final int bcLength;

  private final byte[] bcodes;

  private int bcIndex;

  private int opcode;

  private boolean wide;

  /**
   * @param m the method containing the bytecodes
   * @param bc the array of bytecodes
   * @throws IllegalArgumentException if bc is null
   * @throws IllegalArgumentException if m is null
   */
  public BytecodeStream(IMethod m, byte[] bc) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (bc == null) {
      throw new IllegalArgumentException("bc is null");
    }
    method = m;
    declaringClass = m.getDeclaringClass();
    bcodes = bc;
    bcLength = bc.length;
    bcIndex = 0;
  }

  /**
   * Returns the method that this bytecode stream is from
   */
  public final IMethod method() {
    return method;
  }

  /**
   * Returns the declaring class that this bytecode stream is from
   */
  public final IClass declaringClass() {
    return declaringClass;
  }

  /**
   * Returns the length of the bytecode stream Returns 0 if the method doesn't have any bytecodes (i.e. is abstract or native)
   */
  public final int length() {
    return bcLength;
  }

  /**
   * Returns the current bytecode index
   */
  public final int index() {
    return bcIndex;
  }

  /**
   * Resets the stream to the beginning
   */
  public final void reset() {
    reset(0);
  }

  /**
   * Resets the stream to a given position Use with caution
   * 
   * @param index the position to reset the stream to
   */
  public final void reset(int index) {
    bcIndex = index;
  }

  /**
   * Does the stream have more bytecodes in it?
   */
  public final boolean hasMoreBytecodes() {
    return bcIndex < bcLength;
  }

  /**
   * Returns the opcode of the next instruction in the sequence without advancing to it
   * 
   * @return the opcode of the next instruction
   * @see #nextInstruction()
   */
  public final int peekNextOpcode() {
    return getUnsignedByte(bcIndex);
  }

  /**
   * Sets up the next instruction in the sequence
   * 
   * @return the opcode of the next instruction
   * @see #peekNextOpcode()
   */
  public final int nextInstruction() {
    opcode = readUnsignedByte();
    wide = (opcode == JBC_wide);
    return opcode;
  }

  /**
   * Returns the opcode of the current instruction in the sequence Note: if skipInstruction has been called, but nextInstruction has
   * not, this method will return the opcode of the skipped instruction!
   * 
   * @return the opcode of the current instruction
   * @see #nextInstruction()
   * @see #isWide()
   */
  public final int getOpcode() {
    return opcode;
  }

  /**
   * Are we currently processing a wide instruction?
   * 
   * @return true if current instruction is wide
   * @see #nextInstruction()
   * @see #getOpcode()
   */
  public final boolean isWide() {
    return wide;
  }

  /**
   * Skips the current instruction
   * 
   * @see #skipInstruction(int,boolean)
   */
  public final void skipInstruction() {
    int len = JBC_length[opcode] - 1;
    if (wide)
      len += len;
    if (len >= 0) {
      bcIndex += len;
    } else {
      skipSpecialInstruction(opcode);
    }
  }

  /**
   * Skips the current instruction (without using the opcode field) A slightly optimized version of skipInstruction()
   * 
   * @param opc current opcode
   * @param w whether current instruction follows wide
   * @see #skipInstruction()
   */
  public final void skipInstruction(int opc, boolean w) {
    int len = JBC_length[opc] - 1;
    if (w)
      len += len;
    if (len >= 0)
      bcIndex += len;
    else
      skipSpecialInstruction(opc);
  }

  /**
   * Returns a signed byte value Used for bipush
   * 
   * @return signed byte value
   */
  public final int getByteValue() {
    return readSignedByte();
  }

  /**
   * Returns a signed short value Used for sipush
   * 
   * @return signed short value
   */
  public final int getShortValue() {
    return readSignedShort();
  }

  /**
   * Returns the number of the local (as an unsigned byte) Used for iload, lload, fload, dload, aload, istore, lstore, fstore,
   * dstore, astore, iinc, ret
   * 
   * @return local number
   * @see #getWideLocalNumber()
   */
  public final int getLocalNumber() {
    return readUnsignedByte();
  }

  /**
   * Returns the wide number of the local (as an unsigned short) Used for iload, lload, fload, dload, aload, istore, lstore, fstore,
   * dstore, astore, iinc prefixed by wide
   * 
   * @return wide local number
   * @see #getLocalNumber()
   */
  public final int getWideLocalNumber() {
    return readUnsignedShort();
  }

  /**
   * Returns an increment value (as a signed byte) Used for iinc
   * 
   * @return increment
   * @see #getWideIncrement()
   */
  public final int getIncrement() {
    return readSignedByte();
  }

  /**
   * Returns an increment value (as a signed short) Used for iinc prefixed by wide
   * 
   * @return wide increment
   * @see #getIncrement()
   */
  public final int getWideIncrement() {
    return readSignedShort();
  }

  /**
   * Returns the offset of the branch (as a signed short) Used for if&lt;cond&gt;, ificmp&lt;cond&gt;, ifacmp&lt;cond&gt;, goto, jsr
   * 
   * @return branch offset
   * @see #getWideBranchOffset()
   */
  public final int getBranchOffset() {
    return readSignedShort();
  }

  /**
   * Returns the wide offset of the branch (as a signed int) Used for goto_w, jsr_w
   * 
   * @return wide branch offset
   * @see #getBranchOffset()
   */
  public final int getWideBranchOffset() {
    return readSignedInt();
  }

  /**
   * Skips the padding of a switch instruction Used for tableswitch, lookupswitch
   */
  public final void alignSwitch() {
    int align = bcIndex & 3;
    if (align != 0)
      bcIndex += 4 - align; // eat padding
  }

  /**
   * Returns the default offset of the switch (as a signed int) Used for tableswitch, lookupswitch
   * 
   * @return default switch offset
   */
  public final int getDefaultSwitchOffset() {
    return readSignedInt();
  }

  /**
   * Returns the lowest value of the tableswitch (as a signed int) Used for tableswitch
   * 
   * @return lowest switch value
   * @see #getHighSwitchValue()
   */
  public final int getLowSwitchValue() {
    return readSignedInt();
  }

  /**
   * Returns the highest value of the tableswitch (as a signed int) Used for tableswitch
   * 
   * @return highest switch value
   * @see #getLowSwitchValue()
   */
  public final int getHighSwitchValue() {
    return readSignedInt();
  }

  /**
   * Skips the offsets of a tableswitch instruction Used for tableswitch
   * 
   * @param num the number of offsets to skip
   * @see #getTableSwitchOffset(int)
   */
  public final void skipTableSwitchOffsets(int num) {
    bcIndex += (num << 2);
  }

  /**
   * Returns the numbered offset of the tableswitch (as a signed int) Used for tableswitch The "cursor" has to be positioned at the
   * start of the offset table NOTE: Will NOT advance cursor
   * 
   * @param num the number of the offset to retrieve
   * @return switch offset
   */
  public final int getTableSwitchOffset(int num) {
    return getSignedInt(bcIndex + (num << 2));
  }

  /**
   * Returns the offset for a given value of the tableswitch (as a signed int) or 0 if the value is out of range. Used for
   * tableswitch The "cursor" has to be positioned at the start of the offset table NOTE: Will NOT advance cursor
   * 
   * @param value the value to retrieve offset for
   * @param low the lowest value of the tableswitch
   * @param high the highest value of the tableswitch
   * @return switch offset
   */
  public final int computeTableSwitchOffset(int value, int low, int high) {
    if (value < low || value > high)
      return 0;
    return getSignedInt(bcIndex + ((value - low) << 2));
  }

  /**
   * Returns the number of match-offset pairs in the lookupswitch (as a signed int) Used for lookupswitch
   * 
   * @return number of switch pairs
   */
  public final int getSwitchLength() {
    return readSignedInt();
  }

  /**
   * Skips the match-offset pairs of a lookupswitch instruction Used for lookupswitch
   * 
   * @param num the number of match-offset pairs to skip
   * @see #getLookupSwitchValue(int)
   * @see #getLookupSwitchOffset(int)
   */
  public final void skipLookupSwitchPairs(int num) {
    bcIndex += (num << 3);
  }

  /**
   * Returns the numbered offset of the lookupswitch (as a signed int) Used for lookupswitch The "cursor" has to be positioned at
   * the start of the pair table NOTE: Will NOT advance cursor
   * 
   * @param num the number of the offset to retrieve
   * @return switch offset
   * @see #getLookupSwitchValue(int)
   */
  public final int getLookupSwitchOffset(int num) {
    return getSignedInt(bcIndex + (num << 3) + 4);
  }

  /**
   * Returns the numbered value of the lookupswitch (as a signed int) Used for lookupswitch The "cursor" has to be positioned at the
   * start of the pair table NOTE: Will NOT advance cursor
   * 
   * @param num the number of the value to retrieve
   * @return switch value
   * @see #getLookupSwitchOffset(int)
   */
  public final int getLookupSwitchValue(int num) {
    return getSignedInt(bcIndex + (num << 3));
  }

  /**
   * Returns the offset for a given value of the lookupswitch (as a signed int) or 0 if the value is not in the table. Used for
   * lookupswitch The "cursor" has to be positioned at the start of the offset table NOTE: Will NOT advance cursor WARNING: Uses
   * LINEAR search. Whoever has time on their hands can re-implement this as a binary search.
   * 
   * @param value the value to retrieve offset for
   * @param num the number of match-offset pairs in the lookupswitch
   * @return switch offset
   */
  public final int computeLookupSwitchOffset(int value, int num) {
    for (int i = 0; i < num; i++)
      if (getSignedInt(bcIndex + (i << 3)) == value)
        return getSignedInt(bcIndex + (i << 3) + 4);
    return 0;
  }

  /**
   * Skips the extra stuff after an invokeinterface instruction Used for invokeinterface
   */
  public final void alignInvokeInterface() {
    bcIndex += 2; // eat superfluous stuff
  }

  /**
   * Returns the element type (primitive) of the array (as an unsigned byte) Used for newarray
   * 
   * @return array element type
   */
  public final int getArrayElementType() {
    return readUnsignedByte();
  }

  /**
   * Returns the dimension of the array (as an unsigned byte) Used for multianewarray
   * 
   * @return array dimension
   */
  public final int getArrayDimension() {
    return readUnsignedByte();
  }

  /**
   * Returns the opcode of the wide instruction Used for wide Can be one of iload, lload, fload, dload, aload, istore, lstore,
   * fstore, dstore, astore, iinc
   * 
   * @return the opcode of the wide instruction
   */
  public final int getWideOpcode() {
    opcode = readUnsignedByte();
    return opcode;
  }

  /**
   * Returns the constant pool index of a constant (as an unsigned byte) Used for ldc
   * 
   * @return constant index
   * @see #getWideConstantIndex()
   */
  public final int getConstantIndex() {
    return readUnsignedByte();
  }

  /**
   * Returns the wide constant pool index of a constant (as an unsigned short) Used for ldc_w, ldc2_w
   * 
   * @return wide constant index
   * @see #getConstantIndex()
   */
  public final int getWideConstantIndex() {
    return readUnsignedShort();
  }

  // // HELPER FUNCTIONS

  // Skip a tableswitch or a lookupswitch instruction
  private void skipSpecialInstruction(int opcode) {
    switch (opcode) {
    case JBC_tableswitch: {
      alignSwitch();
      getDefaultSwitchOffset();
      int l = getLowSwitchValue();
      int h = getHighSwitchValue();
      skipTableSwitchOffsets(h - l + 1); // jump offsets
    }
      break;
    case JBC_lookupswitch: {
      alignSwitch();
      getDefaultSwitchOffset();
      int n = getSwitchLength();
      skipLookupSwitchPairs(n); // match-offset pairs
    }
      break;
    case JBC_wide: {
      int oc = getWideOpcode();
      int len = JBC_length[oc] - 1;
      bcIndex += len + len;
    }
      break;
    default:
      throw new NullPointerException();
    }
  }

  // // READ BYTECODES
  private final byte readSignedByte() {
    return bcodes[bcIndex++];
  }

  private final int readUnsignedByte() {
    return (bcodes[bcIndex++] & 0xFF);
  }

  private final int getUnsignedByte(int index) {
    return (bcodes[index] & 0xFF);
  }

  private final int readSignedShort() {
    int i = bcodes[bcIndex++] << 8;
    i |= (bcodes[bcIndex++] & 0xFF);
    return i;
  }

  private final int readUnsignedShort() {
    int i = (bcodes[bcIndex++] & 0xFF) << 8;
    i |= (bcodes[bcIndex++] & 0xFF);
    return i;
  }

  private final int readSignedInt() {
    int i = bcodes[bcIndex++] << 24;
    i |= (bcodes[bcIndex++] & 0xFF) << 16;
    i |= (bcodes[bcIndex++] & 0xFF) << 8;
    i |= (bcodes[bcIndex++] & 0xFF);
    return i;
  }

  private final int getSignedInt(int index) {
    int i = bcodes[index++] << 24;
    i |= (bcodes[index++] & 0xFF) << 16;
    i |= (bcodes[index++] & 0xFF) << 8;
    i |= (bcodes[index] & 0xFF);
    return i;
  }
}
