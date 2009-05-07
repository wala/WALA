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
 * This class provides read-only access to a constant pool. It gets subclassed for each class reader/editor toolkit you want to work
 * with.
 */
public abstract class ConstantPoolReader {
  /**
   * Retrieve the JVM constant pool item type (a Constants.CONSTANT_xxx value). This method should be overriden by a
   * toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract int getConstantPoolItemType(int index);

  /**
   * Retrieve the value of a CONSTANT_Integer constant pool item. This method should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract int getConstantPoolInteger(int index);

  /**
   * Retrieve the value of a CONSTANT_Float constant pool item. This method should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract float getConstantPoolFloat(int index);

  /**
   * Retrieve the value of a CONSTANT_Long constant pool item. This method should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract long getConstantPoolLong(int index);

  /**
   * Retrieve the value of a CONSTANT_Double constant pool item. This method should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract double getConstantPoolDouble(int index);

  /**
   * Retrieve the value of a CONSTANT_String constant pool item. This method should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract String getConstantPoolString(int index);

  /**
   * Retrieve the value of a CONSTANT_Class constant pool item in JVM internal class format (e.g., java/lang/Object). This method
   * should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract String getConstantPoolClassType(int index);

  /**
   * Retrieve the class part of a CONSTANT_FieldRef, CONSTANT_MethodRef, or CONSTANT_InterfaceMethodRef constant pool item, in JVM
   * internal class format (e.g., java/lang/Object). This method should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract String getConstantPoolMemberClassType(int index);

  /**
   * Retrieve the name part of a CONSTANT_FieldRef, CONSTANT_MethodRef, or CONSTANT_InterfaceMethodRef constant pool item, This
   * method should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract String getConstantPoolMemberName(int index);

  /**
   * Retrieve the type part of a CONSTANT_FieldRef, CONSTANT_MethodRef, or CONSTANT_InterfaceMethodRef constant pool item, in JVM
   * internal type format (e.g., Ljava/lang/Object;). This method should be overriden by a toolkit-specific subclass.
   * 
   * @param index the constant pool item to examine
   */
  public abstract String getConstantPoolMemberType(int index);
}