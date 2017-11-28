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

import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 * The MethodEditor is the core of the ShrikeBT code rewriting mechanism. To rewrite code, construct a MethodEditor initialized with
 * the intial code for the method. Then perform a series of passes. In each pass you call beginPass(), insert a number of patches
 * using the insert...() or replace...() methods, then optionally call applyPatches() to update the code with your changes, then
 * call endPass(). The end of each pass updates the code in the MethodData that you passed in, so the new code can be extracted from
 * that MethodData object. Note that if applyPatches() is not called, or it is called but no patches had been inserted, then the
 * code will not be updated and that pass is essentially aborted.
 * 
 * A patch is simply a subclass of MethodEditor.Patch, representing a code sequence to insert into the method. Each patch class
 * implements one method, emitTo(), which writes the patch code into the code stream using the provided MethodEditor.Output
 * instance. Anonymous inner classes are very useful for writing patches.
 * 
 * Patches can be inserted at the following points:
 * <ul>
 * <li>Before the start of the method
 * <li>Before or after a particular instruction
 * <li>Replacing a particular instruction
 * <li>Handling an exception on a particular instruction
 * <li>Handling an exception anywhere in the method
 * <li>After the end of the method, where code will not normally be executed, but can be branched to by another patch
 * </ul>
 * Patch application is deterministic; if two patches are applied at the same point, then the order of application determines the
 * order of code generation, in a way specified by the particular application point. See the patch application methods below.
 * 
 * MethodEditor relies on labels. A label is an integer representing a point in the code. Labels are valid only during a single
 * pass; at the end of each pass, instructions are reordered and old labels become invalid. At the beginning of a pass every
 * instruction in the instructions array is labelled with the index of that instruction in the array. During instrumentation new
 * labels can be allocated by calling MethodEditor.allocateLabel(); control instructions can be created referring to these new
 * labels or the existing labels. At the end of a pass, as patch code is spliced into the method body, all instructions are updated
 * to refer to the new labels which are simply the indices of instructions in the instruction array.
 */
public final class MethodEditor {
  private static final ExceptionHandler[] noHandlers = new ExceptionHandler[0];

  /** Records which original bytecode instruction each Instruction belongs to. */
  private int[] instructionsToBytecodes;

  private IInstruction[] instructions;

  private ExceptionHandler[][] handlers;

  final private MethodData methodInfo;

  // working
  private static final int BEFORE_PASS = 0x01;

  private static final int DURING_PASS = 0x02;

  private static final int EMITTING_CODE = 0x04;

  private static final int BEFORE_END_PASS = 0x08;

  private int state = BEFORE_PASS;

  private int patchCount;

  private Patch[] beforePatches;

  private Patch[] afterPatches;

  private Patch[] lastAfterPatches;

  private Patch[] replacementPatches;

  private Patch methodStartPatches;

  private Patch afterMethodPatches;

  private HandlerPatch[] instructionHandlerPatches;

  private HandlerPatch methodHandlerPatches;

  private int nextLabel;

  /**
   * This patch lets us stuff an exception handler into the code.
   */
  private static class HandlerPatch {
    final HandlerPatch next;

    final String catchClass;

    final int label;

    final Patch patch;

    HandlerPatch(HandlerPatch next, String catchClass, int label, Patch patch) {
      this.next = next;
      this.catchClass = catchClass;
      this.label = label;
      this.patch = patch;
    }
  }

  /**
   * Build an editor for the given method. This editor will write back its changes to the method info.
   * 
   * @throws IllegalArgumentException if info is null
   */
  public MethodEditor(MethodData info) {
    if (info == null) {
      throw new IllegalArgumentException("info is null");
    }
    methodInfo = info;
    instructionsToBytecodes = info.getInstructionsToBytecodes();
    instructions = info.getInstructions();
    handlers = info.getHandlers();
  }

  /**
   * Build an editor for specific method data. After patching the code you can retrieve the new code, handlers and
   * instructions-to-bytecode-offsets map.
   */
  public MethodEditor(Instruction[] instructions, ExceptionHandler[][] handlers, int[] instructionsToBytecodes) {
    if (instructions == null) {
      throw new IllegalArgumentException("null instructions");
    }
    if (handlers == null) {
      throw new IllegalArgumentException("null handlers");
    }
    methodInfo = null;
    this.instructionsToBytecodes = instructionsToBytecodes;
    this.instructions = instructions;
    this.handlers = handlers;
  }

  private void verifyState(int state) {
    if ((state & this.state) == 0) {
      throw new IllegalArgumentException(getStateMessage(state));
    }
  }

  private static String getStateMessage(int state) {
    switch (state) {
    case BEFORE_PASS:
      return "This operation can only be performed before or after an editing pass";
    case DURING_PASS:
      return "This operation can only be performed during an editing pass";
    case EMITTING_CODE:
      return "This operation can only be performed while applying patches and emitting code";
    case BEFORE_END_PASS:
      return "This operation can only be performed after applying patches";
    default:
      return "This operation cannot be performed in this state";
    }
  }

  /**
   * @return the current handler array
   */
  public ExceptionHandler[][] getHandlers() {
    verifyState(BEFORE_PASS | DURING_PASS);
    return handlers;
  }

  /**
   * @return the current instruction array
   */
  public IInstruction[] getInstructions() {
    verifyState(BEFORE_PASS | DURING_PASS);
    return instructions;
  }

  /**
   * @return the current instructions-to-bytecode-offsets map
   */
  public int[] getInstructionsToBytecodes() {
    verifyState(BEFORE_PASS | DURING_PASS);
    return instructionsToBytecodes;
  }

  static ExceptionHandler[] mergeHandlers(ExceptionHandler[] h1, ExceptionHandler[] h2) {
    if (h1.length == 0) {
      return h2;
    } else if (h2.length == 0) {
      return h1;
    } else {
      ExceptionHandler[] r = new ExceptionHandler[h1.length + h2.length];
      System.arraycopy(h1, 0, r, 0, h1.length);
      System.arraycopy(h2, 0, r, h1.length, h2.length);
      return r;
    }
  }

  /**
   * Output is the interface that patches use to emit their code into a method body.
   */
  public final static class Output {
    final ArrayList<IInstruction> newInstructions = new ArrayList<>();

    final ArrayList<ExceptionHandler[]> newInstructionHandlers = new ArrayList<>();

    int[] instructionsToBytecodes = new int[10];

    final int[] labelDefs;

    ExceptionHandler[] additionalHandlers;

    int originalBytecode;

    boolean codeChanged = false;

    Output(int numLabels) {
      labelDefs = new int[numLabels];
    }

    /**
     * Emit a label definition at the current point in the code. The label must have been previously allocated using
     * MethodEditor.allocateLabel.
     */
    public void emitLabel(int label) {
      labelDefs[label] = newInstructions.size();
    }

    /**
     * Emit an instruction at the current point in the code.
     */
    public void emit(Instruction i) {
      codeChanged = true;
      internalEmitInstruction(i);
    }

    /**
     * Emit an instruction with some exception handlers at the current point in the code.
     */
    public void emit(Instruction i, ExceptionHandler[] handlers) {
      codeChanged = true;

      int s = newInstructions.size();
      if (s + 1 > instructionsToBytecodes.length) {
        int[] t = new int[instructionsToBytecodes.length * 2];
        System.arraycopy(instructionsToBytecodes, 0, t, 0, instructionsToBytecodes.length);
        instructionsToBytecodes = t;
      }

      instructionsToBytecodes[s] = originalBytecode;
      newInstructions.add(i);
      newInstructionHandlers.add(mergeHandlers(handlers, additionalHandlers));
    }

    void internalEmitInstruction(IInstruction i) {
      int s = newInstructions.size();
      if (s + 1 > instructionsToBytecodes.length) {
        int[] t = new int[instructionsToBytecodes.length * 2];
        System.arraycopy(instructionsToBytecodes, 0, t, 0, instructionsToBytecodes.length);
        instructionsToBytecodes = t;
      }

      instructionsToBytecodes[s] = originalBytecode;
      newInstructions.add(i);
      newInstructionHandlers.add(additionalHandlers);
    }

    /**
     * Emit a list of instructions at the current point in the code.
     */
    public void emit(Instruction[] instrs) {
      emit(instrs, noHandlers);
    }

    /**
     * Emit a list of instructions with some exception handlers at the current point in the code. All the instructions are covered
     * by all the handlers.
     */
    public void emit(Instruction[] instrs, ExceptionHandler[] handlers) {
      if (instrs.length == 0) {
        return;
      }
      codeChanged = true;

      int s = newInstructions.size();
      if (s + instrs.length > instructionsToBytecodes.length) {
        int[] t = new int[instructionsToBytecodes.length * 2 + instrs.length];
        System.arraycopy(instructionsToBytecodes, 0, t, 0, instructionsToBytecodes.length);
        instructionsToBytecodes = t;
      }

      ExceptionHandler[] hs = mergeHandlers(handlers, additionalHandlers);
      for (int i = 0; i < instrs.length; i++) {
        instructionsToBytecodes[s + i] = originalBytecode;
        newInstructions.add(instrs[i]);
        newInstructionHandlers.add(hs);
      }
    }
  }

  /**
   * This class is subclassed for each kind of patch that you want to apply.
   */
  public static abstract class Patch {
    Patch next;

    public Patch() {
    }

    final Patch insert(Patch next) {
      this.next = next;
      return this;
    }

    /**
     * Override this method to emit the code for your patch.
     */
    public abstract void emitTo(Output w);
  }

  /**
   * This must be called before inserting any patches.
   */
  public void beginPass() {
    verifyState(BEFORE_PASS);
    state = DURING_PASS;

    nextLabel = instructions.length;
    beforePatches = new Patch[instructions.length];
    afterPatches = new Patch[instructions.length];
    lastAfterPatches = new Patch[instructions.length];
    replacementPatches = new Patch[instructions.length];
    instructionHandlerPatches = new HandlerPatch[instructions.length];
    methodStartPatches = null;
    afterMethodPatches = null;
    methodHandlerPatches = null;
    patchCount = 0;
  }

  /**
   * This must be called after inserting any patches.
   */
  public void endPass() {
    state = BEFORE_PASS;

    // lose references to the stored patches so they can be garbage collected
    beforePatches = null;
    afterPatches = null;
    lastAfterPatches = null;
    replacementPatches = null;
    instructionHandlerPatches = null;
    methodStartPatches = null;
    afterMethodPatches = null;
    methodHandlerPatches = null;
  }

  /**
   * Allocate a fresh label. This must be called during a pass and not during code emission.
   */
  public int allocateLabel() throws IllegalArgumentException {
    verifyState(DURING_PASS);
    return nextLabel++;
  }

  /**
   * Insert code to be executed whenever the method is entered. This code is not protected by any exception handlers (other than
   * handlers declared in the patch).
   * 
   * When multiple 'start' patches are given, the last one added is first in execution order.
   * 
   * @throws IllegalArgumentException if p is null
   */
  public void insertAtStart(Patch p) {
    if (p == null) {
      throw new IllegalArgumentException("p is null");
    }
    verifyState(DURING_PASS);
    methodStartPatches = p.insert(methodStartPatches);
    patchCount++;
  }

  /**
   * Insert code to be executed before the instruction. Branches to the instruction will branch to this code. Exception handlers
   * that cover the instruction will be extended to cover the patch.
   * 
   * When multiple 'before' patches are given, the last one added is first in execution order.
   * 
   * @throws IllegalArgumentException if p is null
   */
  public void insertBefore(int i, Patch p) {
    if (p == null) {
      throw new IllegalArgumentException("p is null");
    }
    verifyState(DURING_PASS);
    beforePatches[i] = p.insert(beforePatches[i]);
    patchCount++;
  }

  /**
   * Insert code to be executed after the instruction. This code will only execute if the instruction "falls through". For example,
   * code inserted after a "goto" will never be executed. Likewise if the instruction throws an execution the 'after' code will not
   * be executed. Exception handlers that cover the instruction will be extended to cover the patch.
   * 
   * When multiple 'after' patches are given, the last one added is LAST in execution order.
   */
  public void insertAfter(int i, Patch p) {
    verifyState(DURING_PASS);
    try {
      if (afterPatches[i] == null) {
        lastAfterPatches[i] = afterPatches[i] = p.insert(null);
      } else {
        lastAfterPatches[i].next = p;
        lastAfterPatches[i] = p.insert(null);
      }
      patchCount++;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid i", e);
    }
  }

  /**
   * Insert code to replace the instruction. Exception handlers that cover the instruction will cover the patch.
   * 
   * Multiple replacements are not allowed.
   * 
   * @throws NullPointerException if p is null
   * @throws IllegalArgumentException if p is null
   */
  public void replaceWith(int i, Patch p) throws NullPointerException {
    if (p == null) {
      throw new IllegalArgumentException("p is null");
    }
    verifyState(DURING_PASS);
    if (replacementPatches[i] != null) {
      throw new IllegalArgumentException("Instruction " + i + " cannot be replaced more than once");
    }
    replacementPatches[i] = p.insert(null);
    patchCount++;
  }

  /**
   * An "instruction exception handler" handles exceptions generated by a specific instruction (including patch code that may be
   * inserted before, after, or instead of the instruction in this pass). If the patch code falls through, control resumes with the
   * next instruction. This exception handler handles exceptions before any exception handler already attached to the instruction.
   * Furthermore, the patch itself is covered by the exception handlers already attached to the instruction.
   * 
   * If multiple instruction exception handlers are given, then the last one added handles the exception first; if an exception is
   * rethrown, then the next-to-last one added handles that exception, etc.
   */
  public void addInstructionExceptionHandler(int i, String catchClass, Patch p) {
    verifyState(DURING_PASS);
    try {
      instructionHandlerPatches[i] = new HandlerPatch(instructionHandlerPatches[i], catchClass, allocateLabel(), p);
      patchCount++;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid i: " + i, e);
    }
  }

  /**
   * A "method exception handler" handles exceptions generated anywhere in the method. The patch code must not fall through; it must
   * return or throw an exception.
   * 
   * If multiple method exception handlers are given, then the last one added handles the exception first; if an exception is
   * rethrown, then the next-to-last one added handles that exception, etc.
   */
  public void addMethodExceptionHandler(String catchClass, Patch p) {
    verifyState(DURING_PASS);
    if (p == null) {
      throw new IllegalArgumentException("null p");
    }
    methodHandlerPatches = new HandlerPatch(methodHandlerPatches, catchClass, allocateLabel(), p);
    patchCount++;
  }

  /**
   * This method inserts code that will be placed after the method body. This code will not be executed normally, but it can emit
   * label definitions that other patches can branch to. No exception handlers cover this code (other than exception handlers
   * emitted by patch p itself).
   * 
   * @throws IllegalArgumentException if p is null
   */
  public void insertAfterBody(Patch p) {
    if (p == null) {
      throw new IllegalArgumentException("p is null");
    }
    verifyState(DURING_PASS);
    afterMethodPatches = p.insert(afterMethodPatches);
    patchCount++;
  }

  /**
   * @return the MethodData used to create this editor, or null if no MethodData is linked to this editor
   */
  public MethodData getData() {
    return methodInfo;
  }

  private static ExceptionHandler[] makeExceptionArray(HandlerPatch hp) {
    if (hp == null) {
      return noHandlers;
    } else {
      int count = 0;
      for (HandlerPatch hpIterator = hp; hpIterator != null; hpIterator = hpIterator.next) {
        count++;
      }

      ExceptionHandler[] patchedHandlers = new ExceptionHandler[count];
      count = 0;
      for (HandlerPatch hpIterator = hp; hpIterator != null; hpIterator = hpIterator.next) {
        patchedHandlers[count] = new ExceptionHandler(hpIterator.label, hpIterator.catchClass);
        count++;
      }

      return patchedHandlers;
    }
  }

  /**
   * This method finishes a pass. All code is updated; instructions are reordered and old labels may not be valid.
   * 
   * If no patches were issued, we don't need to do anything at all; this case is detected quickly and no updates are made.
   * 
   * @return true iff non-trivial patches were applied
   */
  public boolean applyPatches() throws IllegalArgumentException {
    verifyState(DURING_PASS);
    state = EMITTING_CODE;

    if (patchCount == 0) {
      state = BEFORE_END_PASS;
      // no patches issued; drop out
      return false;
    }

    Output w = new Output(nextLabel);

    int[] oldInstructionsToNew = new int[instructions.length];

    w.additionalHandlers = noHandlers;
    w.originalBytecode = 0;
    for (Patch p = methodStartPatches; p != null; p = p.next) {
      p.emitTo(w);
    }

    ExceptionHandler[] methodHandlers = makeExceptionArray(methodHandlerPatches);
    if (methodHandlers.length > 0) {
      // if a method handler is added, the code has changed even though
      // the patch might not emit any actual code
      w.codeChanged = true;
    }

    for (int i = 0; i < instructions.length; i++) {
      ExceptionHandler[] basicHandlers = mergeHandlers(handlers[i], methodHandlers);
      HandlerPatch hp = instructionHandlerPatches[i];

      w.emitLabel(i);
      w.originalBytecode = instructionsToBytecodes[i];

      w.additionalHandlers = basicHandlers;
      for (Patch p = beforePatches[i]; p != null; p = p.next) {
        p.emitTo(w);
      }

      w.additionalHandlers = mergeHandlers(makeExceptionArray(hp), basicHandlers);
      Patch replace = replacementPatches[i];
      if (replace == null) {
        oldInstructionsToNew[i] = w.newInstructions.size();
        w.internalEmitInstruction(instructions[i]);
      } else {
        // a patch might delete an instruction, in which case the
        // code is definitely modified
        w.codeChanged = true;
        oldInstructionsToNew[i] = -1;
        replace.emitTo(w);
      }

      w.additionalHandlers = basicHandlers;
      for (Patch p = afterPatches[i]; p != null; p = p.next) {
        p.emitTo(w);
      }

      if (hp != null) {
        // if an instruction handler is added, the code has changed even though
        // the patch might not emit any actual code
        w.codeChanged = true;

        GotoInstruction branchOver = GotoInstruction.make(i + 1);

        w.internalEmitInstruction(branchOver);
        for (HandlerPatch hpIterator = hp; hpIterator != null; hpIterator = hpIterator.next) {
          w.additionalHandlers = mergeHandlers(makeExceptionArray(hpIterator.next), basicHandlers);
          w.emitLabel(hpIterator.label);
          hpIterator.patch.emitTo(w);
          w.internalEmitInstruction(branchOver);
        }
      }
    }

    w.originalBytecode = 0;

    for (HandlerPatch hpIterator = methodHandlerPatches; hpIterator != null; hpIterator = hpIterator.next) {
      w.additionalHandlers = makeExceptionArray(hpIterator.next);
      w.emitLabel(hpIterator.label);
      hpIterator.patch.emitTo(w);
    }

    w.additionalHandlers = noHandlers;
    for (Patch p = afterMethodPatches; p != null; p = p.next) {
      p.emitTo(w);
    }

    state = BEFORE_END_PASS;

    if (!w.codeChanged) {
      return false;
    }

    instructions = new Instruction[w.newInstructions.size()];
    handlers = new ExceptionHandler[instructions.length][];
    instructionsToBytecodes = new int[instructions.length];

    w.newInstructions.toArray(instructions);
    w.newInstructionHandlers.toArray(handlers);
    System.arraycopy(w.instructionsToBytecodes, 0, instructionsToBytecodes, 0, instructionsToBytecodes.length);

    int[] labelDefs = w.labelDefs;

    int[] newInstructionsToOld = new int[instructions.length];

    for (int i = 0; i < instructions.length; i++) {
      instructions[i] = instructions[i].redirectTargets(labelDefs);
      newInstructionsToOld[i] = -1;
    }

    // We want to update each exception handler array exactly once
    IdentityHashMap<ExceptionHandler, Object> adjustedHandlers = null;
    for (int i = 0; i < handlers.length; i++) {
      ExceptionHandler[] hs = handlers[i];
      if (hs.length > 0 && (i == 0 || hs != handlers[i - 1])) {
        if (adjustedHandlers == null) {
          adjustedHandlers = new IdentityHashMap<>();
        }

        for (ExceptionHandler element : hs) {
          ExceptionHandler h = element;
          if (!adjustedHandlers.containsKey(h)) {
            adjustedHandlers.put(h, null);
            h.handler = labelDefs[h.handler]; // breaks invariant of ExceptionHandler: immutable!
          }
        }
      }
    }

    if (methodInfo != null) {
      for (int i = 0; i < oldInstructionsToNew.length; i++) {
        if (oldInstructionsToNew[i] != -1) {
          newInstructionsToOld[oldInstructionsToNew[i]] = i;
        }
      }
      methodInfo.update(instructions, handlers, newInstructionsToOld, instructionsToBytecodes);
    }

    return true;
  }

  /**
   * Apply Visitor v to each instruction in the code, for the purpose of patching the code.
   */
  public void visitInstructions(Visitor v) {
    verifyState(DURING_PASS);

    for (int i = 0; i < instructions.length; i++) {
      v.setIndex(this, i);
      instructions[i].visit(v);
    }
  }

  /**
   * A specialized Instruction.Visitor providing convenience methods for inserting patches. In particular it maintains a notion of
   * the "current position" in the code array.
   */
  public static class Visitor extends IInstruction.Visitor {
    private int index;

    private MethodEditor editor;

    /**
     * Set the current editor and instruction index for this visitor.
     */
    final public void setIndex(MethodEditor e, int i) {
      index = i;
      editor = e;
    }

    /**
     * @return the index of the current instruction in the code array
     */
    final public int getIndex() {
      return index;
    }

    /**
     * Insert a patch after the current instruction in the code.
     */
    final public void insertAfter(Patch p) {
      editor.insertAfter(index, p);
    }

    /**
     * Insert a patch before the current instruction in the code.
     */
    final public void insertBefore(Patch p) {
      editor.insertBefore(index, p);
    }

    /**
     * Replace the current instruction in the code with a patch.
     */
    final public void replaceWith(Patch p) {
      editor.replaceWith(index, p);
    }

    /**
     * Add an exception handler to the current instruction.
     * 
     * @param catchClass the JVM type for the exception to be caught (e.g., Ljava.io.IOException;), or null to catch all exceptions
     * @param p the code to handle the exception
     */
    final public void addInstructionExceptionHandler(String catchClass, Patch p) {
      editor.addInstructionExceptionHandler(index, catchClass, p);
    }
  }
}
