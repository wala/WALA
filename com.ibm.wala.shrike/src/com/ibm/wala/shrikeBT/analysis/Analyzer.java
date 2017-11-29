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
package com.ibm.wala.shrikeBT.analysis;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.shrikeBT.ILoadInstruction;
import com.ibm.wala.shrikeBT.IStoreInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.StoreInstruction;
import com.ibm.wala.shrikeBT.SwapInstruction;
import com.ibm.wala.shrikeBT.Util;

/**
 * @author roca
 */
public class Analyzer {

  public static final String thisType = "THIS";

  public static final String topType = "TOP";

  // inputs
  final protected boolean isConstructor;
  
  final protected boolean isStatic;

  final protected String classType;

  final protected String signature;

  final protected IInstruction[] instructions;

  final protected ExceptionHandler[][] handlers;

  protected ClassHierarchyProvider hierarchy;

  // working
  protected int maxStack;

  protected int maxLocals;

  protected String[][] stacks;

  protected String[][] locals;

  protected int[] stackSizes;

  protected BitSet basicBlockStarts;

  protected int[][] backEdges;

  protected int[] instToBC;

  protected String[][] varTypes;
  
  protected final static String[] noStrings = new String[0];

  protected final static int[] noEdges = new int[0];

  public Analyzer(boolean isConstructor, boolean isStatic, String classType, String signature, IInstruction[] instructions, ExceptionHandler[][] handlers, int[] instToBC, String[][] vars) {
    if (instructions == null) {
      throw new IllegalArgumentException("null instructions");
    }
    if (handlers == null) {
      throw new IllegalArgumentException("null handlers");
    }
    for (IInstruction i : instructions) {
      if (i == null) {
        throw new IllegalArgumentException("null instruction is illegal");
      }
    }
    this.classType = classType;
    this.isConstructor = isConstructor;
    this.isStatic = isStatic;
    this.signature = signature;
    this.instructions = instructions;
    this.handlers = handlers;
    this.instToBC = instToBC;
    this.varTypes = vars;
  }

  /**
   * Use class hierarchy information in 'h'. If this method is not called or h provides only partial hierarchy information, the
   * verifier behaves optimistically.
   */
  final public void setClassHierarchy(ClassHierarchyProvider h) {
    this.hierarchy = h;
  }

  private void addBackEdge(int from, int to) {
    int[] oldEdges = backEdges[from];
    if (oldEdges == null) {
      backEdges[from] = new int[] { to };
    } else if (oldEdges[oldEdges.length - 1] < 0) {
      int left = 1;
      int right = oldEdges.length - 1;
      while (true) {
        if (right - left < 2) {
          if (oldEdges[left] < 0) {
            break;
          } else {
            if (oldEdges[right] >= 0)
              throw new Error("Failed binary search");
            left = right;
            break;
          }
        } else {
          int mid = (left + right) / 2;
          if (oldEdges[mid] < 0) {
            right = mid;
          } else {
            left = mid + 1;
          }
        }
      }
      oldEdges[left] = to;
    } else {
      int[] newEdges = new int[oldEdges.length * 2];
      System.arraycopy(oldEdges, 0, newEdges, 0, oldEdges.length);
      newEdges[oldEdges.length] = to;
      for (int i = oldEdges.length + 1; i < newEdges.length; i++) {
        newEdges[i] = -1;
      }
      backEdges[from] = newEdges;
    }
  }

  final public int[][] getBackEdges() {
    if (backEdges != null) {
      return backEdges;
    }

    backEdges = new int[instructions.length][];

    for (int i = 0; i < instructions.length; i++) {
      IInstruction instr = instructions[i];
      int[] targets = instr.getBranchTargets();
      for (int target : targets) {
        addBackEdge(target, i);
      }
      ExceptionHandler[] hs = handlers[i];
      for (ExceptionHandler element : hs) {
        addBackEdge(element.getHandler(), i);
      }
    }

    for (int i = 0; i < backEdges.length; i++) {
      int[] back = backEdges[i];
      if (back == null) {
        backEdges[i] = noEdges;
      } else if (back[back.length - 1] < 0) {
        int j = back.length;
        while (back[j - 1] < 0) {
          j--;
        }
        int[] newBack = new int[j];
        System.arraycopy(back, 0, newBack, 0, newBack.length);
        backEdges[i] = newBack;
      }
    }

    return backEdges;
  }

  private String patchType(String t) {
    if (t == thisType) {
      return classType;
    } else if (t != null && t.startsWith("#")) {
      return stripSharp(t);
    } else {
      return t;
    }
  }
  
  final public boolean isSubtypeOf(String t1, String t2) {
    return ClassHierarchy.isSubtypeOf(hierarchy, patchType(t1), patchType(t2)) != ClassHierarchy.NO;
  }

  private static boolean isPrimitive(String type) {
    return type != null && (!type.startsWith("L") && !type.startsWith("["));
  }
  
  final public String findCommonSupertype(String t1, String t2) {
    if (String.valueOf(t1).equals(String.valueOf(t2))) {
      return t1;
    }
    
    if (String.valueOf(t1).equals("L;") || String.valueOf(t2).equals("L;")) {
      if (String.valueOf(t1).equals("L;")) {
        return t2;
      } else {
        return t1;
      }
    }
    
    if (t1 == thisType || t2 == thisType || t1 == topType || t2 == topType) {
      return topType;
    }
    
    if (isPrimitive(t1) != isPrimitive(t2)) {
      return topType;
    }
    
    String x = ClassHierarchy.findCommonSupertype(hierarchy, patchType(t1), patchType(t2));

    return x;
  }

  final public BitSet getBasicBlockStarts() {
    if (basicBlockStarts != null) {
      return basicBlockStarts;
    }

    BitSet r = new BitSet(instructions.length);

    r.set(0);
    for (IInstruction instruction : instructions) {
      int[] targets = instruction.getBranchTargets();

      for (int target : targets) {
        r.set(target);
      }
    }
    for (ExceptionHandler[] hs : handlers) {
      if (hs != null) {
        for (ExceptionHandler element : hs) {
          r.set(element.getHandler());
        }
      }
    }

    basicBlockStarts = r;
    return r;
  }

  final public IInstruction[] getInstructions() {
    return instructions;
  }

  private void getReachableRecursive(int from, BitSet reachable, boolean followHandlers, BitSet mask)
      throws IllegalArgumentException {
    if (from < 0) {
      throw new IllegalArgumentException("from < 0");
    }
    while (true) {
      // stop if we've already visited this instruction or if we've gone outside
      // the mask
      if (reachable.get(from) || (mask != null && !mask.get(from))) {
        return;
      }

      reachable.set(from);

      IInstruction instr = instructions[from];
      int[] targets = instr.getBranchTargets();
      for (int target : targets) {
        getReachableRecursive(target, reachable, followHandlers, mask);
      }

      if (followHandlers) {
        ExceptionHandler[] hs = handlers[from];
        for (ExceptionHandler element : hs) {
          getReachableRecursive(element.getHandler(), reachable, followHandlers, mask);
        }
      }

      if (instr.isFallThrough()) {
        ++from;
        continue;
      }

      break;
    }
  }

  final public BitSet getReachableFrom(int from) {
    return getReachableFrom(from, true, null);
  }

  final public void getReachableFromUpdate(int from, BitSet reachable, boolean followHandlers, BitSet mask) {
    if (reachable == null) {
      throw new IllegalArgumentException("reachable is null");
    }
    reachable.clear();
    getReachableRecursive(from, reachable, followHandlers, mask);
  }

  final public BitSet getReachableFrom(int from, boolean followHandlers, BitSet mask) {
    BitSet reachable = new BitSet();
    getReachableRecursive(from, reachable, followHandlers, mask);
    return reachable;
  }

  private void getReachingRecursive(int to, BitSet reaching, BitSet mask) {
    while (true) {
      // stop if we've already visited this instruction or if we've gone outside
      // the mask
      if (reaching.get(to) || (mask != null && !mask.get(to))) {
        return;
      }

      reaching.set(to);

      int[] targets = backEdges[to];
      for (int target : targets) {
        getReachingRecursive(target, reaching, mask);
      }

      if (to > 0 && instructions[to - 1].isFallThrough()) {
        --to;
        continue;
      }

      break;
    }
  }

  private void getReachingBase(int to, BitSet reaching, BitSet mask) {
    int[] targets = backEdges[to];
    for (int target : targets) {
      getReachingRecursive(target, reaching, mask);
    }

    if (to > 0 && instructions[to - 1].isFallThrough()) {
      getReachingRecursive(to - 1, reaching, mask);
    }
  }

  final public void getReachingToUpdate(int to, BitSet reaching, BitSet mask) {
    if (reaching == null) {
      throw new IllegalArgumentException("reaching is null");
    }
    getBackEdges();
    reaching.clear();
    getReachingBase(to, reaching, mask);
  }

  final BitSet getReachingTo(int to, BitSet mask) {
    getBackEdges();
    BitSet reaching = new BitSet();
    getReachingBase(to, reaching, mask);
    return reaching;
  }

  final BitSet getReachingTo(int to) {
    return getReachingTo(to, null);
  }

  private void computeStackSizesAt(int[] stackSizes, int i, int size) throws FailureException {
    while (true) {
      if (stackSizes[i] >= 0) {
        if (size != stackSizes[i]) {
          throw new FailureException(i, "Stack size mismatch", null);
        }
        return;
      }
      stackSizes[i] = size;

      IInstruction instr = instructions[i];
      if (instr instanceof DupInstruction) {
        size += ((DupInstruction) instr).getSize();
      } else if (instr instanceof SwapInstruction) {
      } else {
        size -= instr.getPoppedCount();
        if (instr.getPushedWordSize() > 0) {
          size++;
        }
      }

      int[] targets = instr.getBranchTargets();
      for (int target : targets) {
        computeStackSizesAt(stackSizes, target, size);
      }
      ExceptionHandler[] hs = handlers[i];
      for (ExceptionHandler element : hs) {
        computeStackSizesAt(stackSizes, element.getHandler(), 1);
      }

      if (!instr.isFallThrough()) {
        return;
      }
      i++;
    }
  }

  /**
   * This exception is thrown by verify() when it fails.
   */
  public static final class FailureException extends Exception {

    private static final long serialVersionUID = -7663520961403117526L;

    final private int offset;

    final private String reason;

    private List<PathElement> path;

    FailureException(int offset, String reason, List<PathElement> path) {
      super(reason + " at offset " + offset);
      this.offset = offset;
      this.reason = reason;
      this.path = path;
    }

    /**
     * @return the index of the Instruction which failed to verify
     */
    public int getOffset() {
      return offset;
    }

    /**
     * @return a description of the reason why verification failed
     */
    public String getReason() {
      return reason;
    }

    /**
     * @return a list of PathElements describing how the type that caused the error was propagated from its origin to the point of
     *         the error
     */
    public List<PathElement> getPath() {
      return path;
    }

    void setPath(List<PathElement> path) {
      this.path = path;
    }

    /**
     * Print the path to the given stream, if there is one.
     */
    public void printPath(Writer w) throws IOException {
      if (path != null) {
        for (int i = 0; i < path.size(); i++) {
          PathElement elem = path.get(i);
          String[] stack = elem.stack;
          String[] locals = elem.locals;
          w.write("Offset " + elem.index + ": [");
          for (int j = 0; j < stack.length; j++) {
            if (j > 0) {
              w.write(",");
            }
            w.write(stack[j]);
          }
          w.write("], [");
          for (int j = 0; j < locals.length; j++) {
            if (j > 0) {
              w.write(",");
            }
            w.write(locals[j] == null ? "?" : locals[j]);
          }
          w.write("]\n");
        }
      }
    }
  }

  public static final class PathElement {
    final int index;

    final String[] stack;

    final String[] locals;

    PathElement(int index, String[] stack, String[] locals) {
      this.stack = stack.clone();
      this.locals = locals.clone();
      this.index = index;
    }

    /**
     * @return the bytecode offset of the instruction causing a value transfer.
     */
    public int getIndex() {
      return index;
    }

    /**
     * @return the types of the local variabls at the instruction.
     */
    public String[] getLocals() {
      return locals;
    }

    /**
     * @return the types of the working stack at the instruction.
     */
    public String[] getStack() {
      return stack;
    }
  }

  private static String[] cutArray(String[] a, int len) {
    if (len == 0) {
      return noStrings;
    } else {
      String[] r = new String[len];
      System.arraycopy(a, 0, r, 0, len);
      return r;
    }
  }

  private boolean mergeTypes(int i, String[] curStack, int curStackSize, String[] curLocals, int curLocalsSize,
      List<PathElement> path) throws FailureException {
    boolean a = mergeStackTypes(i, curStack, curStackSize, path);
    boolean b = mergeLocalTypes(i, curLocals, curLocalsSize);
    return a||b;
  }
  
  private static boolean longType(String type) {
    return Constants.TYPE_long.equals(type) || Constants.TYPE_double.equals(type);
  }
  
  private boolean mergeStackTypes(int i, String[] curStack, int curStackSize, List<PathElement> path) throws FailureException {
    boolean changed = false;

    if (stacks[i] == null) {
      stacks[i] = cutArray(curStack, curStackSize);
      changed = true;
    } else {
      String[] st = stacks[i];
      if (st.length != curStackSize) {
        throw new FailureException(i, "Stack size mismatch: " + st.length + ", " + curStackSize, path);
      }
      for (int j = 0; j < curStackSize; j++) {
        String t = findCommonSupertype(st[j], curStack[j]);
        if (t != st[j]) {
          if (t == null) {
            throw new FailureException(i, "Stack type mismatch at " + j + " (" + st[j] + " vs " + curStack[j] + ")", path);
          }
          st[j] = t;
          changed = true;
        }
      }
    }

    return changed;
  }

  private boolean mergeLocalTypes(int i, String[] curLocals, int curLocalsSize) {
    boolean changed = false;

    if (locals[i] == null) {
      locals[i] = cutArray(curLocals, curLocalsSize);
      changed = true;
    } else {
      String[] ls = locals[i];
      for (int lj = 0, cj = 0; lj < ls.length; lj++, cj++) {
        String t = findCommonSupertype(ls[lj], curLocals[cj]);
        if (t != ls[lj]) {
          ls[lj] = t;
          changed = true;
        }
      }
    }

    return changed;
  }

  public static String stripSharp(String type) {
    return type.substring(type.lastIndexOf('#')+1);
  }
  
  /**
   * A PathElement describes a point where a value is moved from one location to another.
   */
  private void computeTypes(int i, TypeVisitor visitor, BitSet makeTypesAt, List<PathElement> path) throws FailureException {
    final String[] curStack = new String[maxStack];
    final String[] curLocals = new String[maxLocals];

    while (true) {
      if (path != null) {
        path.add(new PathElement(i, stacks[i], locals[i]));
      }

      int curStackSize = stacks[i].length;
      System.arraycopy(stacks[i], 0, curStack, 0, curStackSize);

      final int[] curLocalsSize = { locals[i].length };
      System.arraycopy(locals[i], 0, curLocals, 0, curLocalsSize[0]);

      IInstruction.Visitor localsUpdate = new IInstruction.Visitor() {
        
        @Override
        public void visitInvoke(IInvokeInstruction instruction) {
          if (instruction.getInvocationCode() == Dispatch.SPECIAL) {
            if (instruction.getMethodName().equals("<init>")) {
              int sz = Util.getParamsTypes(instruction.getClassType(), instruction.getMethodSignature()).length;
                
              if (isConstructor) {
                if (thisType.equals(curStack[ sz-1 ])) {
                  for(int i = 0; i < curLocals.length; i++) {
                    if (thisType.equals(curLocals[i])) {
                      curLocals[i] = classType;
                    }
                  }
                  for(int i = 0; i < curStack.length; i++) {
                    if (thisType.equals(curStack[i])) {
                      curStack[i] = classType;
                    }
                  }
                }
              }
              if (curStack.length > sz && curStack[sz] != null && curStack[sz].startsWith("#")) {
                curStack[sz] = stripSharp(curStack[sz]);
              }
            }
          }
        }

        @Override
        public void visitLocalLoad(ILoadInstruction instruction) {
          String t = curLocals[instruction.getVarIndex()];
          curStack[0] = t;
        }

        @Override
        public void visitLocalStore(IStoreInstruction instruction) {
          int index = instruction.getVarIndex();
          String t = curStack[0];
          curLocals[index] = t;
          if (longType(t) && curLocals.length > index+1) {
            curLocals[index+1] = null;
          }
          if (index >= curLocalsSize[0]) {
            curLocalsSize[0] = index + (longType(t) && curLocals.length > index+1? 2: 1);
          }
        }
      };

      boolean restart = false;
      while (true) {
        IInstruction instr = instructions[i];
        int popped = instr.getPoppedCount();

        if (curStackSize < popped) {
          throw new FailureException(i, "Stack underflow", path);
        }

        if (visitor != null) {
          visitor.setState(i, path, curStack, curLocals);
          instr.visit(visitor);
          if (!visitor.shouldContinue()) {
            return;
          }
        }

        if (instr instanceof DupInstruction) {
          DupInstruction d = (DupInstruction) instr;
          int size = d.getSize();

          System.arraycopy(curStack, popped, curStack, popped + size, curStackSize - popped);
          System.arraycopy(curStack, 0, curStack, popped, size);
          curStackSize += size;
        } else if (instr instanceof SwapInstruction) {
          String s = curStack[0];
          curStack[0] = curStack[1];
          curStack[1] = s;
        } else {
          String pushed = instr.getPushedType(curStack);
          if (instr instanceof NewInstruction && ! pushed.startsWith("[")) {
            pushed = "#" + instToBC[i] + "#" + pushed;
          }
          if (pushed != null) {
            System.arraycopy(curStack, popped, curStack, 1, curStackSize - popped);
            curStack[0] = Util.getStackType(pushed);
            instr.visit(localsUpdate); // visit localLoad after pushing
            curStackSize -= popped - 1;
          } else {
            instr.visit(localsUpdate); // visit localStore before popping
            System.arraycopy(curStack, popped, curStack, 0, curStackSize - popped);
            curStackSize -= popped;
            
            if (varTypes != null) {
            if (instr instanceof IStoreInstruction) {
              int local = ((IStoreInstruction)instr).getVarIndex();
              if (Constants.TYPE_null.equals(curLocals[local])) {
                for(int idx : new int[]{i, i+1}) {
                  int bc = instToBC[idx];
                  if (bc == 667 && local == 16) {
                    System.err.println("got here");
                  }
                  if (bc != -1 && varTypes != null && varTypes.length > bc && varTypes[bc] != null) {
                    if (varTypes[bc].length > local && varTypes[bc][local] != null) {
                      String declaredType = varTypes[bc][local];
                      curLocals[local] = declaredType;
                    }
                  }
                }
              }
            }
            }
          }
        }

        ExceptionHandler[] handler = handlers[i];
        for (ExceptionHandler element : handler) {
          int target = element.getHandler();
          String cls = element.getCatchClass();
          if (cls == null) {
            cls = "Ljava/lang/Throwable;";
          }
          String[] catchStack = new String[]{ cls };
          if (mergeTypes(target, catchStack, 1, curLocals, curLocalsSize[0], path)) { 
            computeTypes(target, visitor, makeTypesAt, path);            
          }
        }
        
        int[] targets = instr.getBranchTargets();
        for (int target : targets) {
          if (mergeTypes(target, curStack, curStackSize, curLocals, curLocalsSize[0], path)) {
            computeTypes(target, visitor, makeTypesAt, path);
          }
        }

        if (!instr.isFallThrough()) {
          break;
        } else {
          i++;
          if (makeTypesAt.get(i)) {
            if (mergeTypes(i, curStack, curStackSize, curLocals, curLocalsSize[0], path)) {
              restart = true;
              break;
            }
            if (path != null) {
              path.remove(path.size() - 1);
            }
            return;
          }
        }
      }
      if (!restart) {
        break;
      }
    }
    if (path != null) {
      path.remove(path.size() - 1);
    }
  }

  public int[] getStackSizes() throws FailureException {
    if (stackSizes != null) {
      return stackSizes;
    }

    stackSizes = new int[instructions.length];

    for (int i = 0; i < stackSizes.length; i++) {
      stackSizes[i] = -1;
    }
    computeStackSizesAt(stackSizes, 0, 0);

    return stackSizes;
  }

  private void computeMaxLocals() {
    maxLocals = locals[0].length;
    for (IInstruction instr : instructions) {
      if (instr instanceof LoadInstruction) {
        maxLocals = Math.max(maxLocals, ((LoadInstruction) instr).getVarIndex() + 1);
      } else if (instr instanceof StoreInstruction) {
        maxLocals = Math.max(maxLocals, ((StoreInstruction) instr).getVarIndex() + 1);
      }
    }
  }

  final protected void initTypeInfo() throws FailureException {
    stacks = new String[instructions.length][];
    locals = new String[instructions.length][];

    String thisType;
    if (isConstructor) {
      thisType = Analyzer.thisType;
    } else {
      thisType = classType;
    }
    
    stacks[0] = noStrings;
    locals[0] = Util.getParamsTypesInLocals(isStatic ? null : thisType, signature);
    if (isConstructor) {
      for(int i = 0; i < locals[0].length; i++) {
        if (classType.equals(locals[0][i])) {
          locals[0][i] = thisType;
        }
      }
    }
    int[] stackSizes = getStackSizes();
    maxStack = 0;
    for (int stackSize : stackSizes) {
      maxStack = Math.max(maxStack, stackSize);
    }
    computeMaxLocals();
  }

  /**
   * Verify the method and compute types at every program point.
   * 
   * @throws FailureException the method contains invalid bytecode
   */
  final public void computeTypes(TypeVisitor v, BitSet makeTypesAt, boolean wantPath) throws FailureException {
    initTypeInfo();
    computeTypes(0, v, makeTypesAt, wantPath ? new ArrayList<PathElement>() : null);
  }

  public abstract class TypeVisitor extends IInstruction.Visitor {
    public abstract void setState(int index, List<PathElement> path, String[] curStack, String[] curLocals);

    public abstract boolean shouldContinue();
  }

  /**
   * @return an array indexed by instruction index; each entry is an array of Strings giving the types of the locals at that
   *         instruction.
   */
  final public String[][] getLocalTypes() {
    return locals;
  }

  /**
   * @return an array indexed by instruction index; each entry is an array of Strings giving the types of the stack elements at that
   *         instruction. The top of the stack is the last element of the array.
   */
  final public String[][] getStackTypes() {
    return stacks;
  }

  protected Analyzer(MethodData info) {
    this(info.getName().equals("<init>"), info.getIsStatic(), info.getClassType(), info.getSignature(), info.getInstructions(), info.getHandlers(), info.getInstructionsToBytecodes(), null);
  }

  protected Analyzer(MethodData info, int[] instToBC, String[][] vars) {
    this(info.getName().equals("<init>"), info.getIsStatic(), info.getClassType(), info.getSignature(), info.getInstructions(), info.getHandlers(), instToBC, vars);
  }

  public static Analyzer createAnalyzer(MethodData info) {
    if (info == null) {
      throw new IllegalArgumentException("info is null");
    }
    return new Analyzer(info);
  }

}
