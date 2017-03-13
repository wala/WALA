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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is a container for a bunch of information that we might know about a method. It's here for convenience so users can
 * just pass one MethodData around instead of passing around an array of instructions, an array of exception handler lists, etc.
 * 
 * It also provides a place to hang annotations. We provide a table mapping abstract "keys" to annotation objects. We also provide
 * an invalidation protocol so that the annotation objects can be notified of code changes and react appropriately. This is useful
 * for caching analysis results for a method.
 */
public final class MethodData {
  final private HashMap<Object, Results> map = new HashMap<>();

  final private int access;

  final private String classType;

  final private String name;

  final private String signature;

  private IInstruction[] instructions;

  private ExceptionHandler[][] handlers;

  private int[] instructionsToBytecodes;

  private boolean hasChanged = false;

  /**
   * Create information for a method, with no exception handlers and a dummy mapping of instructions to original bytecodes. 
   * @param access the access flags
   * @param classType the class in which the method is defined, in JVM type format (e.g., Ljava/lang/Object;)
   * @param name the method name
   * @param signature the method signature, in JVM type format (e.g., (ILjava/lang/Object;)V)
   * @param instructions the instructions making up the method
   */
  public static MethodData makeWithDefaultHandlersAndInstToBytecodes(int access, String classType, String name, String signature, IInstruction[] instructions) {
    ExceptionHandler[][] handlers = new ExceptionHandler[instructions.length][];
    Arrays.fill(handlers, new ExceptionHandler[0]);
    int[] i2b = new int[instructions.length];
    for (int i = 0; i < i2b.length; i++) {
      i2b[i] = i;
    }
    return new MethodData(access, classType, name, signature, instructions, handlers, i2b);    
  }
  
  /**
   * Gather the information for a method "from scratch".
   * 
   * @param access the access flags
   * @param classType the class in which the method is defined, in JVM type format (e.g., Ljava/lang/Object;)
   * @param name the method name
   * @param signature the method signature, in JVM type format (e.g., (ILjava/lang/Object;)V)
   * @param instructions the instructions making up the method
   * @param handlers a list of exception handlers for each instruction
   * @param instructionsToBytecodes a map stating, for each instruction, the offset of the original bytecode instruction(s) giving
   *          rise to this instruction
   */
  public MethodData(int access, String classType, String name, String signature, IInstruction[] instructions,
      ExceptionHandler[][] handlers, int[] instructionsToBytecodes) {
    this.classType = classType;
    this.access = access;
    this.name = name;
    this.signature = signature;
    this.instructions = instructions;
    this.handlers = handlers;
    this.instructionsToBytecodes = instructionsToBytecodes;

    if (instructions == null) {
      throw new IllegalArgumentException("Instruction array cannot be null");
    }
    if (handlers == null) {
      throw new IllegalArgumentException("Handler array cannot be null");
    }
    if (instructionsToBytecodes == null) {
      throw new IllegalArgumentException("InstructionToBytecodes array cannot be null");
    }
    if (instructions.length != handlers.length) {
      throw new IllegalArgumentException("Handlers array must be the same length as the instructions");
    }
    if (instructions.length != instructionsToBytecodes.length) {
      throw new IllegalArgumentException("Bytecode map array must be the same length as the instructions");
    }
  }

  /**
   * Gather the information for a method after it has been decoded.
   * 
   * @param d the decoder which has decoded the method
   * @param access the access flags
   * @param classType the class in which the method is defined, in JVM type format (e.g., Ljava/lang/Object;)
   * @param name the method name
   * @param signature the method signature, in JVM type format (e.g., (ILjava/lang/Object;)V)
   * @throws NullPointerException if d is null
   */
  public MethodData(Decoder d, int access, String classType, String name, String signature) throws NullPointerException {
    this(access, classType, name, signature, d.getInstructions(), d.getHandlers(), d.getInstructionsToBytecodes());
  }

  public void setHasChanged() {
    hasChanged = true;
  }

  /**
   * @return the method signature, in JVM format
   */
  public String getSignature() {
    return signature;
  }

  /**
   * @return the method name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the method access flags
   */
  public int getAccess() {
    return access;
  }

  /**
   * @return the JVM type for the class defining the method (e.g., Ljava/lang/Object;)
   */
  public String getClassType() {
    return classType;
  }

  /**
   * @return whether or not the method is static
   */
  public boolean getIsStatic() {
    return (access & Constants.ACC_STATIC) != 0;
  }

  /**
   * @return whether or not the method is synchronized
   */
  public boolean getIsSynchronized() {
    return (access & Constants.ACC_SYNCHRONIZED) != 0;
  }

  /**
   * @return the exception handler lists
   */
  public ExceptionHandler[][] getHandlers() {
    return handlers;
  }

  /**
   * @return the instruction array
   */
  public IInstruction[] getInstructions() {
    return instructions;
  }

  /**
   * @return the map from instructions to bytecode offsets
   */
  public int[] getInstructionsToBytecodes() {
    return instructionsToBytecodes;
  }

  /**
   * Annotation objects implement this Results interface. The Results interface is used to notify an annotation that the method code
   * has been updated.
   */
  public static interface Results {
    /**
     * This method is called just before the code for a method changes. The existing instructions, handlers, etc can be read from
     * the current info.
     * 
     * @param info the method data this annotation is attached to
     * @param newInstructions the instructions the method will change to
     * @param newHandlers the handler lists the method will change to
     * @param newInstructionMap the instructions-to-bytecodes map the method will change to
     * @return true to remove the object from the info set, for example because the annotation is now invalid
     */
    public boolean notifyUpdate(MethodData info, IInstruction[] newInstructions, ExceptionHandler[][] newHandlers,
        int[] newInstructionMap);
  }

  /**
   * Get the annotation for the given key.
   * 
   * @return the annotation or null if there isn't one
   */
  public Results getInfo(Object key) {
    return map.get(key);
  }

  /**
   * Set the annotation for the given key.
   */
  public void putInfo(Object key, Results value) {
    map.put(key, value);
  }

  void update(IInstruction[] instructions, ExceptionHandler[][] handlers, int[] newInstructionMap, int[] instructionsToBytecodes) {
    for (Iterator<Object> i = map.keySet().iterator(); i.hasNext();) {
      Object key = i.next();
      Results r = map.get(key);
      if (r.notifyUpdate(this, instructions, handlers, newInstructionMap)) {
        i.remove();
      }
    }

    this.instructions = instructions;
    this.handlers = handlers;
    this.instructionsToBytecodes = instructionsToBytecodes;
    hasChanged = true;
  }

  /**
   * @return true iff the code has been updated at least once
   */
  public boolean getHasChanged() {
    return hasChanged;
  }

  @Override
  public String toString() {
    return getClassType() + "." + getName() + getSignature();
  }
}
