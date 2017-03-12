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
 * Instructions are immutable objects. It is always legal to take a reference to an instruction and use it in any other context. You
 * never need to copy instructions. It is often a good idea to keep references to frequently used instructions cached in static
 * fields.
 * 
 * To generate an Instruction, locate the class for the instruction you want to generate, and invoke the appropriate "make" static
 * method on that class. The Util class has convenience methods for creating some of the more complex instructions using reflection
 * to fill in some of the needed information (e.g., makeGet, makePut, makeInvoke).
 * 
 * We simplify the bytecode instruction set a bit using some preprocessing and postprocessing:
 * 
 * There is no 'iinc' instruction. 'iinc' instructions are expanded to 'iload; bipush; iadd; istore' during decoding and reassembled
 * during compilation.
 * 
 * There are no 'jsr' or 'ret' instructions. Bytecode subroutines are expanded inline during decoding.
 * 
 * There are no 'ifeq', 'ifne', 'ifgt', 'ifge', 'iflt', 'ifle' instructions. These instructions are expanded to 'bipush 0; if_icmp'
 * during decoding and reassembled during compilation.
 * 
 * There are no 'ifnull' or 'ifnonnull' instructions. These instructions are expanded to 'aconst_null; if_acmp' during decoding and
 * reassembled during compilation.
 * 
 * All Java values, including longs and doubles, occupy just one word on the stack. Places where the JVM assumes differently are
 * fixed up during compilation. However, longs and double still take up two local variable slots (this usually doesn't matter to
 * instrumentation).
 * 
 * Control transfer instructions refer to target instructions using integers. These integers are usually indices into an array of
 * instructions.
 */
public abstract class Instruction implements Constants, Cloneable, IInstruction {
  public final static int[] noInstructions = new int[0];

  /**
   * Ensure that only this package can subclass Instruction.
   */
  Instruction(short opcode) {
    this.opcode = opcode;
  }

  final short opcode;

  /**
   * @return true if the instruction can "fall through" to the following instruction
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

  /**
   * @return an array containing the labels this instruction can branch to (not including the following instruction if this
   *         instruction 'falls through')
   */
  @Override
  public int[] getBranchTargets() {
    return noInstructions;
  }

  /**
   * @return an Instruction equivalent to this one but with any branch labels updated by looking them up in the targetMap array
   */
  @Override
  public IInstruction redirectTargets(int[] targetMap) {
    return this;
  }

  /**
   * @return the number of values this instruction pops off the working stack
   */
  @Override
  public int getPoppedCount() {
    return 0;
  }

  /**
   * @return the opcode selected for this instruction, or -1 if we don't know it yet
   */
  public final short getOpcode() {
    return opcode;
  }

  /**
   * Computes the type of data pushed onto the stack, or null if none is pushed.
   * 
   * @param poppedTypesToCheck the types of the data popped off the stack by this instruction; if poppedTypes is null, then we don't
   *          know the incoming stack types and the result of this method may be less accurate
   */
  @Override
  public String getPushedType(String[] poppedTypesToCheck) {
    return null;
  }

  /**
   * @return the JVM word size of the value this instruction pushes onto the stack, or 0 if this instruction doesn't push anything
   *         onto the stack.
   */
  @Override
  public byte getPushedWordSize() {
    return 0;
  }

  /**
   * Apply a Visitor to this instruction. We invoke the appropriate Visitor method according to the type of this instruction.
   */
  @Override
  public abstract void visit(IInstruction.Visitor v);

  /**
   * Subclasses must implement toString.
   */
  @Override
  public abstract String toString();

  /**
   * We're immutable so there's no need to clone any Instruction object.
   */
  @Override
  final public Object clone() {
    return this;
  }
}
