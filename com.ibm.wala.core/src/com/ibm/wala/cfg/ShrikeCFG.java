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
package com.ibm.wala.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.BytecodeLanguage;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.ArrayIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.NodeWithNumber;
import com.ibm.wala.util.shrike.ShrikeUtil;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 * A graph of basic blocks.
 */
public class ShrikeCFG extends AbstractCFG<IInstruction, ShrikeCFG.BasicBlock> implements BytecodeCFG  {

  private static final boolean DEBUG = false;

  private int[] instruction2Block;

  private final IBytecodeMethod<IInstruction> method;

  /**
   * Cache this here for efficiency
   */
  private final int hashBase;

  /**
   * Set of Shrike {@link ExceptionHandler} objects that cover this method.
   */
  final private Set<ExceptionHandler> exceptionHandlers = HashSetFactory.make(10);

  public static ShrikeCFG make(IBytecodeMethod<IInstruction> m) {
    return new ShrikeCFG(m);
  }
    
  private ShrikeCFG(IBytecodeMethod<IInstruction> method) throws IllegalArgumentException {
    super(method);
    if (method == null) {
      throw new IllegalArgumentException("method cannot be null");
    }
    this.method = method;
    this.hashBase = method.hashCode() * 9967;
    makeBasicBlocks();
    init();
    computeI2BMapping();
    computeEdges();
    
    if (DEBUG) {
      System.err.println(this);
    }
  }
  
  @Override
  public IBytecodeMethod<IInstruction> getMethod() {
    return method;
  }

  @Override
  public int hashCode() {
    return 9511 * getMethod().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof ShrikeCFG) && getMethod().equals(((ShrikeCFG) o).getMethod());
  }

  @Override
  public IInstruction[] getInstructions() {
    try {
      return method.getInstructions();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /**
   * Compute a mapping from instruction to basic block. Also, compute the blocks that end with a 'normal' return.
   */
  private void computeI2BMapping() {
    instruction2Block = new int[getInstructions().length];
    for (BasicBlock b : this) {
      for (int j = b.getFirstInstructionIndex(); j <= b.getLastInstructionIndex(); j++) {
        instruction2Block[j] = getNumber(b);
      }
    }
  }

  /**
   * Compute outgoing edges in the control flow graph.
   */
  private void computeEdges() {
    for (BasicBlock b : this) {
      if (b.equals(exit())) {
        continue;
      } else if (b.equals(entry())) {
        BasicBlock bb0 = getBlockForInstruction(0);
        assert bb0 != null;
        addNormalEdge(b, bb0);
      } else {
        b.computeOutgoingEdges();
      }
    }
  }

  private void makeBasicBlocks() {
    ExceptionHandler[][] handlers;
    try {
      handlers = method.getHandlers();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      handlers = null;
    }

    // Compute r so r[i] == true iff instruction i begins a basic block.
    boolean[] r = new boolean[getInstructions().length];
    boolean[] catchers = new boolean[getInstructions().length];
    r[0] = true;
    IInstruction[] instructions = getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      int[] targets = instructions[i].getBranchTargets();

      // if there are any targets, then break the basic block here.
      // also break the basic block after a return
      if (targets.length > 0 || !instructions[i].isFallThrough()) {
        if (i + 1 < instructions.length && !r[i + 1]) {
          r[i + 1] = true;
        }
      }

      for (int j = 0; j < targets.length; j++) {
        if (!r[targets[j]]) {
          r[targets[j]] = true;
        }
      }
      if (instructions[i].isPEI()) {
        ExceptionHandler[] hs = handlers[i];
        // break the basic block here.
        if (i + 1 < instructions.length && !r[i + 1]) {
          r[i + 1] = true;
        }
        if (hs != null && hs.length > 0) {
          for (int j = 0; j < hs.length; j++) {
            exceptionHandlers.add(hs[j]);
            if (!r[hs[j].getHandler()]) {
              // we have not discovered the catch block yet.
              // form a new basic block
              r[hs[j].getHandler()] = true;
            }
            catchers[hs[j].getHandler()] = true;
          }
        }
      }
    }

    BasicBlock entry = new BasicBlock(-1);
    addNode(entry);

    int j = 1;
    for (int i = 0; i < r.length; i++) {
      if (r[i]) {
        BasicBlock b = new BasicBlock(i);
        addNode(b);
        if (catchers[i]) {
          setCatchBlock(j);
        }
        j++;
      }
    }

    BasicBlock exit = new BasicBlock(-1);
    addNode(exit);
  }

  /**
   * Return an instruction's basic block in the CFG given the index of the instruction in the CFG's instruction array.
   */
  @Override
  public BasicBlock getBlockForInstruction(int index) {
    return getNode(instruction2Block[index]);
  }

  public final class BasicBlock extends NodeWithNumber implements IBasicBlock<IInstruction> {

    /**
     * The number of the ShrikeBT instruction that begins this block.
     */
    final private int startIndex;

    public BasicBlock(int startIndex) {
      this.startIndex = startIndex;
    }

    @Override
    public boolean isCatchBlock() {
      return ShrikeCFG.this.isCatchBlock(getNumber());
    }

    private void computeOutgoingEdges() {
      if (DEBUG) {
        System.err.println("Block " + this + ": computeOutgoingEdges()");
      }

      IInstruction last = getInstructions()[getLastInstructionIndex()];
      int[] targets = last.getBranchTargets();
      for (int target : targets) {
        BasicBlock b = getBlockForInstruction(target);
        addNormalEdgeTo(b);
      }
      addExceptionalEdges(last);
      if (last.isFallThrough()) {
        BasicBlock next = getNode(getNumber() + 1);
        addNormalEdgeTo(next);
      }
      if (last instanceof ReturnInstruction) {
        // link each return instruction to the exit block.
        BasicBlock exit = exit();
        addNormalEdgeTo(exit);
      }
    }

    /**
     * Add any exceptional edges generated by the last instruction in a basic block.
     * 
     * @param last the last instruction in a basic block.
     */
    protected void addExceptionalEdges(IInstruction last) {
      IClassHierarchy cha = getMethod().getClassHierarchy();
      if (last.isPEI()) {
        Collection<TypeReference> exceptionTypes = null;
        boolean goToAllHandlers = false;

        ExceptionHandler[] hs = getExceptionHandlers();
        if (last instanceof ThrowInstruction) {
          // this class does not have the type information needed
          // to determine what the athrow throws. So, add an
          // edge to all reachable handlers. Better information can
          // be obtained later with SSA type propagation.
          // TODO: consider pruning to only the exception types that
          // this method either catches or allocates, since these are
          // the only types that can flow to an athrow.
          goToAllHandlers = true;
        } else {
          if (hs != null && hs.length > 0) {
            IClassLoader loader = getMethod().getDeclaringClass().getClassLoader();
            BytecodeLanguage l = (BytecodeLanguage) loader.getLanguage();
            exceptionTypes = l.getImplicitExceptionTypes(last);
            if (last instanceof IInvokeInstruction) {
              IInvokeInstruction call = (IInvokeInstruction) last;
              exceptionTypes = HashSetFactory.make(exceptionTypes);
              MethodReference target = MethodReference.findOrCreate(l, loader.getReference(), call.getClassType(), call
                  .getMethodName(), call.getMethodSignature());
              try {
                exceptionTypes.addAll(l.inferInvokeExceptions(target, cha));
              } catch (InvalidClassFileException e) {
                e.printStackTrace();
                Assertions.UNREACHABLE();
              }
              IMethod mTarget = cha.resolveMethod(target);
              if (mTarget == null) {
                goToAllHandlers = true;
              }
            }
          }
        }

        if (hs != null && hs.length > 0) {
          // found a handler for this PEI

          // create a mutable copy
          if (!goToAllHandlers) {
            exceptionTypes = HashSetFactory.make(exceptionTypes);
          }

          // this var gets set to false if goToAllHandlers is true but some enclosing exception handler catches all
          // exceptions.  in such a case, we need not add an exceptional edge to the method exit
          boolean needEdgeToExitForAllHandlers = true;
          for (ExceptionHandler element : hs) {
            if (DEBUG) {
              System.err.println(" handler " + element);
            }
            BasicBlock b = getBlockForInstruction(element.getHandler());
            if (DEBUG) {
              System.err.println(" target " + b);
            }
            if (goToAllHandlers) {
              // add an edge to the catch block.
              if (DEBUG) {
                System.err.println(" gotoAllHandlers " + b);
              }
              addExceptionalEdgeTo(b);
              // if the handler catches all exceptions, we don't need to add an edge to the exit or any other handlers
              if (element.getCatchClass() == null) {
                needEdgeToExitForAllHandlers = false;
                break;
              }
            } else {
              TypeReference caughtException = null;
              if (element.getCatchClass() != null) {
                ClassLoaderReference loader = ShrikeCFG.this.getMethod().getDeclaringClass().getReference().getClassLoader();
                caughtException = ShrikeUtil.makeTypeReference(loader, element.getCatchClass());
                if (DEBUG) {
                  System.err.println(" caughtException " + caughtException);
                }
                IClass caughtClass = cha.lookupClass(caughtException);
                if (caughtClass == null) {
                  // conservatively add the edge, and raise a warning
                  addExceptionalEdgeTo(b);
                  Warnings.add(FailedExceptionResolutionWarning.create(caughtException));
                  // null out caughtException, to avoid attempting to process it
                  caughtException = null;
                }
              } else {
                if (DEBUG) {
                  System.err.println(" catchClass() == null");
                }
                // hs[j].getCatchClass() == null.
                // this means that the handler catches all exceptions.
                // add the edge and null out all types
                if (!exceptionTypes.isEmpty()) {
                  addExceptionalEdgeTo(b);
                  exceptionTypes.clear();
                  assert caughtException == null;
                }
              }
              if (caughtException != null) {
                IClass caughtClass = cha.lookupClass(caughtException);
                // the set "caught" should be the set of exceptions that MUST
                // have been caught by the handlers in scope
                ArrayList<TypeReference> caught = new ArrayList<>(exceptionTypes.size());
                // check if we should add an edge to the catch block.
                for (TypeReference t : exceptionTypes) {
                  if (t != null) {
                    IClass klass = cha.lookupClass(t);
                    if (klass == null) {
                      Warnings.add(FailedExceptionResolutionWarning.create(caughtException));
                      // conservatively add an edge
                      addExceptionalEdgeTo(b);
                    } else {
                      boolean subtype1 = cha.isSubclassOf(klass, caughtClass);
                      if (subtype1 || cha.isSubclassOf(caughtClass, klass)) {
                        // add the edge and null out the type from the array
                        addExceptionalEdgeTo(b);
                        if (subtype1) {
                          caught.add(t);
                        }
                      }
                    }
                  }
                }
                exceptionTypes.removeAll(caught);
              }
            }
          }
          // if needed, add an edge to the exit block.
          if ((exceptionTypes == null && needEdgeToExitForAllHandlers) || (exceptionTypes != null && !exceptionTypes.isEmpty())) {
            BasicBlock exit = exit();
            addExceptionalEdgeTo(exit);
          }
        } else {
          // found no handler for this PEI ... link to the exit block.
          BasicBlock exit = exit();
          addExceptionalEdgeTo(exit);
        }
      }
    }

    private ExceptionHandler[] getExceptionHandlers() {
      ExceptionHandler[][] handlers;
      try {
        handlers = method.getHandlers();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
        handlers = null;
      }
      ExceptionHandler[] hs = handlers[getLastInstructionIndex()];
      return hs;
    }

    private void addNormalEdgeTo(BasicBlock b) {
      addNormalEdge(this, b);
    }

    private void addExceptionalEdgeTo(BasicBlock b) {
      addExceptionalEdge(this, b);
    }

    @Override
    public int getLastInstructionIndex() {
      if (this == entry() || this == exit()) {
        // these are the special end blocks
        return -2;
      }
      if (getNumber() == (getMaxNumber() - 1)) {
        // this is the last non-exit block
        return getInstructions().length - 1;
      } else {
        int i = 1;
        BasicBlock next;
        do {
          next = getNode(getNumber() + i);
        } while (next == null);
        return next.getFirstInstructionIndex() - 1;
      }
    }

    @Override
    public int getFirstInstructionIndex() {
      return startIndex;
    }

    @Override
    public String toString() {
      return "BB[Shrike]" + getNumber() + " - " + method.getDeclaringClass().getReference().getName() + "." + method.getName();
    }

    /*
     * @see com.ibm.wala.cfg.BasicBlock#isExitBlock()
     */
    @Override
    public boolean isExitBlock() {
      return this == ShrikeCFG.this.exit();
    }

    /*
     * @see com.ibm.wala.cfg.BasicBlock#isEntryBlock()
     */
    @Override
    public boolean isEntryBlock() {
      return this == ShrikeCFG.this.entry();
    }

    /*
     * @see com.ibm.wala.cfg.BasicBlock#getMethod()
     */
    @Override
    public IMethod getMethod() {
      return ShrikeCFG.this.getMethod();
    }

    @Override
    public int hashCode() {
      return hashBase + getNumber();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof BasicBlock) && ((BasicBlock) o).getMethod().equals(getMethod())
          && ((BasicBlock) o).getNumber() == getNumber();
    }

    /*
     * @see com.ibm.wala.cfg.BasicBlock#getNumber()
     */
    @Override
    public int getNumber() {
      return getGraphNodeId();
    }

    @Override
    public Iterator<IInstruction> iterator() {
      return new ArrayIterator<>(getInstructions(), getFirstInstructionIndex(), getLastInstructionIndex());
    }
  }

  @Override
  public String toString() {
    StringBuffer s = new StringBuffer("");
    for (BasicBlock bb : this) {
      s.append("BB").append(getNumber(bb)).append("\n");
      for (int j = bb.getFirstInstructionIndex(); j <= bb.getLastInstructionIndex(); j++) {
        s.append("  ").append(j).append("  ").append(getInstructions()[j]).append("\n");
      }

      Iterator<BasicBlock> succNodes = getSuccNodes(bb);
      while (succNodes.hasNext()) {
        s.append("    -> BB").append(getNumber(succNodes.next())).append("\n");
      }
    }
    return s.toString();
  }

  @Override
  public Set<ExceptionHandler> getExceptionHandlers() {
    return exceptionHandlers;
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getProgramCounter(int)
   */
  @Override
  public int getProgramCounter(int index) {
    try {
      return method.getBytecodeIndex(index);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return -1;
    }
  }

  /**
   * A warning when we fail to resolve the type of an exception
   */
  private static class FailedExceptionResolutionWarning extends Warning {

    final TypeReference T;

    FailedExceptionResolutionWarning(TypeReference T) {
      super(Warning.MODERATE);
      this.T = T;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + T;
    }

    public static FailedExceptionResolutionWarning create(TypeReference T) {
      return new FailedExceptionResolutionWarning(T);
    }
  }
}
