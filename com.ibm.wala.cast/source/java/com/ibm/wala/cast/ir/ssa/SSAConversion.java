/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ir.ssa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.analysis.LiveAnalysis;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.ssa.IR.SSA2LocalMap;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * @author Julian Dolby
 * 
 * Standard SSA conversion for local value numbers.
 */
public class SSAConversion extends AbstractSSAConversion {

  public static boolean DEBUG = false;

  public static boolean DEBUG_UNDO = false;

  public static boolean DEBUG_NAMES = false;

  public static boolean DUMP = false;

  private final AstIRFactory.AstIR ir;

  private int nextSSAValue;

  private final DebuggingInformation debugInfo;

  private final LexicalInformation lexicalInfo;

  private final SymbolTable symtab;

  private final LiveAnalysis.Result liveness;

  private SSA2LocalMap computedLocalMap;

  private Map<Integer,Integer> assignments = HashMapFactory.make();
  
  //
  // Copy propagation history
  //

  private final Map<Object, CopyPropagationRecord> copyPropagationMap;

  private final ArrayList<CopyPropagationRecord> R[];

  private static class UseRecord {
    final int instructionIndex;

    final int useNumber;

    private UseRecord(int instructionIndex, int useNumber) {
      this.useNumber = useNumber;
      this.instructionIndex = instructionIndex;
    }

    @Override
    public String toString() {
      return "[use " + useNumber + " of " + instructionIndex + "]";
    }
    
    @Override
    public int hashCode() {
      return useNumber * instructionIndex;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof UseRecord) && instructionIndex == ((UseRecord) o).instructionIndex
          && useNumber == ((UseRecord) o).useNumber;
    }
  }

  private class PhiUseRecord {
    final int BBnumber;

    final int phiNumber;

    final int useNumber;

    private PhiUseRecord(int BBnumber, int phiNumber, int useNumber) {
      this.BBnumber = BBnumber;
      this.phiNumber = phiNumber;
      this.useNumber = useNumber;
    }

    @Override
    public String toString() {
      return "[use " + useNumber + " of " + phiNumber + " of block " + BBnumber + "]";
    }
    
    @Override
    public int hashCode() {
      return phiNumber * BBnumber * useNumber;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof PhiUseRecord) && BBnumber == ((PhiUseRecord) o).BBnumber && phiNumber == ((PhiUseRecord) o).phiNumber
          && useNumber == ((PhiUseRecord) o).useNumber;
    }
  }

  private class CopyPropagationRecord {
    final int rhs;

    final int instructionIndex;

    final Set<Object> renamedUses = HashSetFactory.make(2);

    private final Set<CopyPropagationRecord> childRecords = HashSetFactory.make(1);

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer("<vn " + rhs + " at " + instructionIndex);
      for (CopyPropagationRecord c : childRecords) {
        sb.append("\n " + c.toString());
      }
      sb.append(">");
      return sb.toString();
    }
    
    @Override
    public int hashCode() {
      return instructionIndex;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof CopyPropagationRecord) && instructionIndex == ((CopyPropagationRecord) o).instructionIndex;
    }

    private CopyPropagationRecord(int instructionIndex, int rhs) {
      if (DEBUG_UNDO)
        System.err.println(("new copy record for instruction #" + instructionIndex + ", rhs value is " + rhs));
      this.rhs = rhs;
      this.instructionIndex = instructionIndex;
    }

    private void addChild(CopyPropagationRecord rec) {
      if (DEBUG_UNDO)
        System.err.println(("(" + rec.instructionIndex + "," + rec.rhs + ") is a child of (" + instructionIndex + "," + rhs + ")"));
      childRecords.add(rec);
    }

    private void addUse(int instructionIndex, int use) {
      if (DEBUG_UNDO)
        System.err.println(("propagated use of (" + this.instructionIndex + "," + this.rhs + ") at use #" + use + " of instruction #"
        + instructionIndex));
      UseRecord rec = new UseRecord(instructionIndex, use);
      copyPropagationMap.put(rec, this);
      renamedUses.add(rec);
    }

    private void addUse(int BB, int phiNumber, int use) {
      PhiUseRecord rec = new PhiUseRecord(BB, phiNumber, use);
      copyPropagationMap.put(rec, this);
      renamedUses.add(rec);
    }

    private SSAInstruction undo(SSAInstruction inst, int use, int val) {
      int c = getNumberOfUses(inst);
      int[] newUses = new int[c];
      for (int i = 0; i < c; i++) {
        if (i == use)
          newUses[i] = val;
        else
          newUses[i] = getUse(inst, i);
      }

      return inst.copyForSSA(CFG.getMethod().getDeclaringClass().getClassLoader().getInstructionFactory(), null, newUses);
    }

    private void undo(int rhs) {
      int lhs = symtab.newSymbol();

      instructions[instructionIndex] = new AssignInstruction(instructionIndex, lhs, rhs);

      if (DEBUG_UNDO)
        System.err.println(("recreating assignment at " + instructionIndex + " as " + lhs + " = " + rhs));

      for (Object x : renamedUses) {
        if (x instanceof UseRecord) {
          UseRecord use = (UseRecord) x;
          int idx = use.instructionIndex;
          SSAInstruction inst = instructions[idx];

          if (DEBUG_UNDO)
            System.err.println(("Changing use #" + use.useNumber + " of inst #" + idx + " to val " + lhs));

          if (use.useNumber >= 0) {
            instructions[idx] = undo(inst, use.useNumber, lhs);
          } else {
            lexicalInfo.getExposedUses(idx)[-use.useNumber - 1] = lhs;
          }
          copyPropagationMap.remove(use);
        } else {
          PhiUseRecord use = (PhiUseRecord) x;
          int bb = use.BBnumber;
          int phi = use.phiNumber;
          SSACFG.BasicBlock BB = CFG.getNode(bb);
          BB.addPhiForLocal(phi, (SSAPhiInstruction) undo(BB.getPhiForLocal(phi), use.useNumber, lhs));
          copyPropagationMap.remove(use);
        }
      }

      for (CopyPropagationRecord copyPropagationRecord : childRecords) {
        copyPropagationRecord.undo(lhs);
      }
    }

    public void undo() {
      undo(this.rhs);
      copyPropagationMap.remove(new UseRecord(instructionIndex, rhs));
    }
  }

  public static void undoCopyPropagation(AstIRFactory.AstIR ir, int instruction, int use) {
    SSAInformation info = (SSAInformation) ir.getLocalMap();
    info.undoCopyPropagation(instruction, use);
  }

  public static void copyUse(AstIRFactory.AstIR ir, int fromInst, int fromUse, int toInst, int toUse) {
    SSAInformation info = (SSAInformation) ir.getLocalMap();
    info.copyUse(fromInst, fromUse, toInst, toUse);
  }

  //
  // SSA2LocalMap implementation for SSAConversion
  //
  private class SSAInformation implements com.ibm.wala.ssa.IR.SSA2LocalMap {
    private final String[][] computedNames = new String[valueMap.length][]; 
    
    @Override
    public String[] getLocalNames(int pc, int vn) {
      
      if (computedNames[vn] != null) {
        return computedNames[vn];
      }
      
      int v = skip(vn) || vn >= valueMap.length ? vn : valueMap[vn];
      String[][] namesData = debugInfo.getSourceNamesForValues();
      String[] vNames = namesData[v];
      Set<String> x = HashSetFactory.make();
      x.addAll(Arrays.asList(vNames));
 
      MutableIntSet vals = IntSetUtil.make();
      while (assignments.containsKey(v) && !vals.contains(v)) {
        vals.add(v);
        v = assignments.get(v);
        vNames = namesData[v];
        x.addAll(Arrays.asList(vNames));        
      }

      return computedNames[vn] = x.toArray(new String[x.size()]);
    }

    private void undoCopyPropagation(int instructionIndex, int useNumber) {

      if (DEBUG_UNDO)
        System.err.println(("undoing for use #" + useNumber + " of inst #" + instructionIndex));

      UseRecord use = new UseRecord(instructionIndex, useNumber);
      if (copyPropagationMap.containsKey(use)) {
        copyPropagationMap.get(use).undo();
      }
    }

    private void copyUse(int fromInst, int fromUse, int toInst, int toUse) {
      UseRecord use = new UseRecord(fromInst, fromUse);
      if (copyPropagationMap.containsKey(use)) {
        copyPropagationMap.get(use).addUse(toInst, toUse);
      }
    }

    private Map<Object, CopyPropagationRecord> getCopyHistory() {
      return copyPropagationMap;
    }
    
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer( super.toString() );
      
      for(Map.Entry<Object, CopyPropagationRecord> x : copyPropagationMap.entrySet()) {
        sb.append(x.getKey().toString() + " --> " + x.getValue().toString() + "\n");
      }
      
      return sb.toString();
    }
  }

  private CopyPropagationRecord topR(int v) {
    if (R[v] != null && !R[v].isEmpty()) {
      CopyPropagationRecord rec = peek(R[v]);
      if (top(v) == rec.rhs) {
        return rec;
      }
    }

    return null;
  }

  private static <T> void push(ArrayList<T> stack, T elt) {
    stack.add(elt);
  }
  
  private static <T> T peek(ArrayList<T> stack) {
    return stack.get(stack.size()-1); 
  }

  //
  // implementation of AbstractSSAConversion hooks
  //

  @Override
  protected int getNumberOfDefs(SSAInstruction inst) {
    return inst.getNumberOfDefs();
  }

  @Override
  protected int getDef(SSAInstruction inst, int index) {
    return inst.getDef(index);
  }

  @Override
  protected int getNumberOfUses(SSAInstruction inst) {
    return inst.getNumberOfUses();
  }

  @Override
  protected int getUse(SSAInstruction inst, int index) {
    return inst.getUse(index);
  }

  @Override
  protected boolean isAssignInstruction(SSAInstruction inst) {
    return inst instanceof AssignInstruction;
  }

  @Override
  protected int getMaxValueNumber() {
    return symtab.getMaxValueNumber();
  }

  @Override
  protected boolean skip(int vn) {
    return false;
  }

  @Override
  protected boolean isLive(SSACFG.BasicBlock Y, int V) {
    return (liveness.isLiveEntry(Y, V));
  }

  private void addPhi(SSACFG.BasicBlock BB, SSAPhiInstruction phi) {
    BB.addPhiForLocal(phiCounts[BB.getGraphNodeId()], phi);
  }

  @Override
  protected void placeNewPhiAt(int value, SSACFG.BasicBlock Y) {
    int[] params = new int[CFG.getPredNodeCount(Y)];
    for (int i = 0; i < params.length; i++)
      params[i] = value;

    SSAPhiInstruction phi = new SSAPhiInstruction(SSAInstruction.NO_INDEX, value, params);

    if (DEBUG)
      System.err.println(("Placing " + phi + " at " + Y));

    addPhi(Y, phi);
  }

  @Override
  protected SSAPhiInstruction getPhi(SSACFG.BasicBlock B, int index) {
    return B.getPhiForLocal(index);
  }

  @Override
  protected void setPhi(SSACFG.BasicBlock B, int index, SSAPhiInstruction inst) {
    B.addPhiForLocal(index, inst);
  }

  @Override
  protected SSAPhiInstruction repairPhiDefs(SSAPhiInstruction phi, int[] newDefs) {
    return (SSAPhiInstruction) phi.copyForSSA(CFG.getMethod().getDeclaringClass().getClassLoader().getInstructionFactory(), newDefs, null);
  }

  @Override
  protected void repairPhiUse(SSACFG.BasicBlock BB, int phiIndex, int rvalIndex, int newRval) {
    SSAPhiInstruction phi = getPhi(BB, phiIndex);

    int[] newUses = new int[getNumberOfUses(phi)];
    for (int v = 0; v < newUses.length; v++) {
      int oldUse = getUse(phi, v);
      int newUse = (v == rvalIndex) ? newRval : oldUse;
      newUses[v] = newUse;

      if (v == rvalIndex && topR(oldUse) != null) {
        topR(oldUse).addUse(BB.getGraphNodeId(), phiIndex, v);
      }
    }

    phi.setValues(newUses);
  }

  @Override
  protected void pushAssignment(SSAInstruction inst, int index, int newRhs) {
    int lhs = getDef(inst, 0);
    int rhs = getUse(inst, 0);

    assignments.put(rhs, lhs);
    
    CopyPropagationRecord rec = new CopyPropagationRecord(index, newRhs);
    push(R[lhs], rec);
    if (topR(rhs) != null) {
      topR(rhs).addChild(rec);
    }
  }

  @Override
  protected void repairInstructionUses(SSAInstruction inst, int index, int[] newUses) {
    for (int j = 0; j < getNumberOfUses(inst); j++) {
      if (topR(getUse(inst, j)) != null) {
        topR(getUse(inst, j)).addUse(index, j);
      }
    }

    int[] lexicalUses = lexicalInfo.getExposedUses(index);
    if (lexicalUses != null) {
      for (int j = 0; j < lexicalUses.length; j++) {
        int lexicalUse = lexicalUses[j];
        if (lexicalUse != -1 && !skip(lexicalUse)) {
          if (S.length <= lexicalUse ||  S[lexicalUse].isEmpty()) {
            lexicalUses[j] = -1;
          } else {
            int newUse = top(lexicalUse);

            lexicalUses[j] = newUse;

            if (topR(lexicalUse) != null) {
              topR(lexicalUse).addUse(index, -j - 1);
            }
          }
        }
      }
    }
  }

  @Override
  protected void repairInstructionDefs(SSAInstruction inst, int index, int[] newDefs, int[] newUses) {
    instructions[index] = inst.copyForSSA(CFG.getMethod().getDeclaringClass().getClassLoader().getInstructionFactory(), newDefs, newUses);
  }

  @Override
  protected void popAssignment(SSAInstruction inst, int index) {
    instructions[index] = null;
  }

  @Override
  protected boolean isConstant(int valueNumber) {
    return symtab.isConstant(valueNumber);
  }

  @Override
  protected boolean skipRepair(SSAInstruction inst, int index) {
    if (!super.skipRepair(inst, index)) {
      return false;
    }

    if (index == -1)
      return true;

    int[] lexicalUses = lexicalInfo.getExposedUses(index);
    if (lexicalUses != null) {
      for (int j = 0; j < lexicalUses.length; j++) {
        if (!skip(lexicalUses[j])) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * @param ir
   * @param options
   */
  @SuppressWarnings("unchecked")
  private SSAConversion(AstMethod M, AstIRFactory.AstIR ir, SSAOptions options) {
    super(ir, options);
    HashMap<Object, CopyPropagationRecord> m = HashMapFactory.make();
    this.copyPropagationMap = (ir.getLocalMap() instanceof SSAInformation) ? ((SSAInformation) ir.getLocalMap()).getCopyHistory()
        : m;

    this.ir = ir;
    this.debugInfo = M.debugInfo();
    this.lexicalInfo = ir.lexicalInfo();
    this.symtab = ir.getSymbolTable();
    this.R = new ArrayList[ir.getSymbolTable().getMaxValueNumber() + 1];

    for (int i = 0; i < CFG.getNumberOfNodes(); i++) {
      SSACFG.BasicBlock bb = CFG.getNode(i);
      if (bb.hasPhi()) {
        phiCounts[i] = IteratorUtil.count(bb.iteratePhis());
      }
    }

    this.nextSSAValue = ir.getNumberOfParameters() + 1;

    int[] exitLive = lexicalInfo.getExitExposedUses();
    BitVector v = new BitVector();
    if (exitLive != null) {
      for (int element : exitLive) {
        if (element > -1) {
          v.set(element);
        }
      }
    }
    this.liveness = LiveAnalysis.perform(CFG, symtab, v);

    if (DEBUG) {
      System.err.println(liveness);
    }
  }

  @Override
  protected int getNextNewValueNumber() {
    while (symtab.isConstant(nextSSAValue) || skip(nextSSAValue))
      ++nextSSAValue;
    symtab.ensureSymbol(nextSSAValue);
    int v = nextSSAValue++;
    return v;
  }

  @Override
  protected void initializeVariables() {
    for (int V = 1; V <= getMaxValueNumber(); V++) {
      if (!skip(V)) {
        R[V] = new ArrayList<>();
      }
    }

    int[] params = symtab.getParameterValueNumbers();
    for (int i = 0; i < params.length; i++) {
      if (!skip(params[i])) {
        S[params[i]].push(params[i]);
        valueMap[params[i]] = params[i];
      }
    }

  }

  @Override
  protected void repairExit() {
    int[] exitLives = lexicalInfo.getExitExposedUses();
    if (exitLives != null) {
      for (int i = 0; i < exitLives.length; i++) {
        if (exitLives[i] != -1 && !skip(exitLives[i])) {
          assert !S[exitLives[i]].isEmpty();
          exitLives[i] = top(exitLives[i]);
        }
      }
    }
  }

  //
  // Global control.
  //

  @Override
  protected void fail(int v) {
    System.err.println("during SSA conversion of the following IR:");
    System.err.println(ir);
    super.fail(v);
  }

  public SSA2LocalMap getComputedLocalMap() {
    return computedLocalMap;
  }

  @Override
  public void perform() {
    super.perform();

    if (DUMP) {
      System.err.println(ir);
      if (lexicalInfo != null) {
        for (int i = 0; i < instructions.length; i++) {
          int[] lexicalUses = lexicalInfo.getExposedUses(i);
          if (lexicalUses != null) {
            System.err.print(("extra uses for " + instructions[i] + ": "));
            for (int lexicalUse : lexicalUses) {
              System.err.print((new Integer(lexicalUse).toString() + " "));
            }
            System.err.println("");
          }
        }
      }
    }

    computedLocalMap = new SSAInformation();
  }

  private static IntSet valuesToConvert(AstIRFactory.AstIR ir) {
    SSAInstruction[] insts = ir.getInstructions();
    MutableIntSet foundOne = new BitVectorIntSet();
    MutableIntSet foundTwo = new BitVectorIntSet();
    for (SSAInstruction inst : insts) {
      if (inst != null) {
        for (int j = 0; j < inst.getNumberOfDefs(); j++) {
          int def = inst.getDef(j);
          if (def != -1) {
            if (foundOne.contains(def) || ir.getSymbolTable().isConstant(def) || def <= ir.getNumberOfParameters()
                || inst instanceof AssignInstruction) {
              foundTwo.add(def);
            } else {
              foundOne.add(def);
            }
          }
        }
      }
    }

    return foundTwo;
  }

  public static SSA2LocalMap convert(AstMethod M, AstIRFactory.AstIR ir, SSAOptions options) {
    return convert(M, ir, options, valuesToConvert(ir));
  }

  public static SSA2LocalMap convert(AstMethod M, final AstIRFactory.AstIR ir, SSAOptions options, final IntSet values) {
    try {
      if (DEBUG) {
        System.err.println(("starting conversion for " + values));
        System.err.println(ir);
      }
      if (DEBUG_UNDO)
        System.err.println((">>> starting " + ir.getMethod()));
      SSAConversion ssa = new SSAConversion(M, ir, options) {
        final int limit = ir.getSymbolTable().getMaxValueNumber();

        @Override
        protected boolean skip(int i) {
          return (i >= 0) && (i <= limit) && (!values.contains(i));
        }
      };
      ssa.perform();
      if (DEBUG_UNDO)
        System.err.println(("<<< done " + ir.getMethod()));
      return ssa.getComputedLocalMap();
    } catch (RuntimeException e) {
//      System.err.println(("exception " + e + " while converting:"));
//      System.err.println(ir);
      throw e;
    } catch (Error e) {
//      System.err.println(("error " + e + " while converting:"));
//      System.err.println(ir);
      throw e;
    }
  }

}
