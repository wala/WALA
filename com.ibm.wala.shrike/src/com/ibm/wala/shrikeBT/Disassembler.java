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

import java.io.IOException;
import java.io.Writer;

/**
 * This is a very simple component to disassemble a ShrikeBT method. The disassembly is just the list of ShrikeBT instructions,
 * annotated with exception handler blocks and the mapping back to the original bytecodes.
 */
public class Disassembler {
  final private IInstruction[] instructions;

  final private ExceptionHandler[][] handlers;

  final private int[] instructionsToBytecodes;

  /**
   * Create a disassembler for a method.
   */
  public Disassembler(IInstruction[] instructions, ExceptionHandler[][] handlers, int[] instructionsToBytecodes) {
    this.instructions = instructions;
    this.handlers = handlers;
    this.instructionsToBytecodes = instructionsToBytecodes;
  }

  /**
   * Create a disassembler for a method.
   * 
   * @throws NullPointerException if data is null
   */
  public Disassembler(MethodData data) throws NullPointerException {
    this(data.getInstructions(), data.getHandlers(), data.getInstructionsToBytecodes());
  }

  /**
   * Write the disassembly to a stream. Each line is prefixed with 'prefix'.
   */
  public void disassembleTo(String prefix, Writer w) throws IOException {
    for (int j = 0; j < instructions.length; j++) {
      w.write(prefix + j + ": " + instructions[j] + " (" + instructionsToBytecodes[j] + ")\n");
      for (int k = 0; k < handlers[j].length; k++) {
        w.write(prefix + "    Handles " + handlers[j][k].catchClass + " at " + handlers[j][k].handler + "\n");
      }
    }
  }

  /**
   * Write the disassembly to a stream.
   */
  public void disassembleTo(Writer w) throws IOException {
    disassembleTo("", w);
  }
}
