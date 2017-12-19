/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.Collection;

/**
 * A mapping that tells, for a given instruction s, what "names" does s def and use <em>indirectly</em>.
 * 
 * For example, an {@link SSALoadIndirectInstruction} takes as an argument a pointer value. This map tells which locals that pointer
 * may alias.
 * 
 * So if we have
 * 
 * Example A:
 * <pre>
 * v2 = SSAAddressOf v1;
 * v7 = #1;
 * v3 = SSALoadIndirect v2; (1)
 * </pre>
 * 
 * Then this map will tell us that instruction (1) indirectly uses whichever source-level entity (in the source or bytecode) v1 represents.
 * 
 * (Don't be confused by AddressOf v1 .. we're not actually taking the address of v1 ... we're taking the address of some source level entity (like a local variable in source code or bytecode) for which v1 is just an SSA name)
 * 
 * As a more complex example, when we have lexical scoping, we can have the following IR generated, which passes a local by reference:
 * 
 * Example B:
 * <tt>
 * foo:
 *   v3 = AddressOf v2;
 *   bar(v3)  (1)
 * bar(v1):
 *   StoreIndirect v1, #7.
 * </tt>
 * 
 * In this case, the instruction (1) potentially defs the locals aliased by v2. The lexical scoping support could/should use this
 * information to rebuild SSA accounting for the fact that (1) defs v2.
 * 
 */
public interface SSAIndirectionData<T extends SSAIndirectionData.Name> {

  /**
   * A Name is a mock interface introduced just for strong typing. A Name represents some semantic entity in the program
   * representation (e.g. a local variable in the source code or a local number in the bytecode)
   */
  public interface Name {
  }

  /**
   * Returns the set of "source" level names (e.g. local variables in bytecode or source code) for which this map holds information.
   */
  Collection<T> getNames();

  /**
   * For the instruction at the given index, and a source-level name, return the SSA value number which represents this
   * instruction's def of that name.
   * 
   * For example, in Example B in header comment above, suppose v2 referred to a "source"-entity called "Local1".
   * Since instruction (1) (call to bar) defs "Local1", we introduce in this table a new SSA value number, say v7, which represents
   * the value of "Local1" immediately after this instruction.   
   */
  int getDef(int instructionIndex, T name);

  /**
   * Record the fact that a particular instruction defs a particular SSA value number (newDef), representing the value of a "source" entity
   * "name".
   * 
   * @see #getDef
   */
  void setDef(int instructionIndex, T name, int newDef);

  /**
   * For the instruction at the given index, and a source-level name, return the SSA value number which represents this
   * instruction's use of that name.
   * 
   * For example, in Example A in header comment above, suppose v1 referred to a "source"-entity called "Local1".
   * Since instruction (1) (LoadIndirect) uses "Local1", we record in this table the SSA value number that represents "Local1"
   * immediately before instruction (1).   So if v1 and v7 both refer to "Local1", then (1) uses v7.  If v7 does NOT refer to "Local1",
   * then (1) uses v1.
   */
  int getUse(int instructionIndex, T name);

  /**
   *  @see #getUse
   */
  void setUse(int instructionIndex, T name, int newUse);

}
