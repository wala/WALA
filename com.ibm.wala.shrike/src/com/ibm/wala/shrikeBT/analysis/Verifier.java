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

import java.util.BitSet;
import java.util.List;

import com.ibm.wala.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrikeBT.ArrayLoadInstruction;
import com.ibm.wala.shrikeBT.ArrayStoreInstruction;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.CheckCastInstruction;
import com.ibm.wala.shrikeBT.ComparisonInstruction;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.ConversionInstruction;
import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.GetInstruction;
import com.ibm.wala.shrikeBT.GotoInstruction;
import com.ibm.wala.shrikeBT.InstanceofInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.PopInstruction;
import com.ibm.wala.shrikeBT.PutInstruction;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.ShiftInstruction;
import com.ibm.wala.shrikeBT.StoreInstruction;
import com.ibm.wala.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.shrikeBT.Util;

/**
 * This class typechecks intermediate code. It's very easy to use:
 * 
 * <pre>
 * 
 *      MethodData md = ...;
 *      try {
 *          (new Verifier(md)).verify();
 *      } catch (Verifier.FailureException ex) {
 *          System.out.println(&quot;Verification failed at instruction &quot;
 *              + ex.getOffset() + &quot;: &quot; + ex.getReason());
 *      }
 *  
 * </pre>
 * 
 * For full verification you need to provide class hierarchy information using
 * setClassHierarchy. Without this information, we can't compute the exact types
 * of variables at control flow merge points. If you don't provide a hierarchy,
 * or the hierarchy you provide is partial, then the Verifier will be
 * optimistic.
 * 
 * This method can also be used to gather type information for every stack and
 * local variable at every program point. Just call computeTypes() instead of
 * verify() and then retrieve the results with getLocalTypes() and
 * getStackTypes().
 */
public final class Verifier extends Analyzer {
  final class VerifyVisitor extends TypeVisitor {
    private int curIndex;
    private List<PathElement> curPath;
    private FailureException ex;
    private String[] curStack;
    private String[] curLocals;

    VerifyVisitor() {
    }

    public void setState(int offset, List<PathElement> path, String[] curStack, String[] curLocals) {
      curIndex = offset;
      curPath = path;
      this.curStack = curStack;
      this.curLocals = curLocals;
    }

    public boolean shouldContinue() {
      return ex == null;
    }

    void checkError() throws FailureException {
      if (ex != null) {
        throw ex;
      }
    }

    private void checkStackSubtype(int i, String t) {
      if (!isSubtypeOf(curStack[i], Util.getStackType(t))) {
        ex = new FailureException(curIndex, "Expected type " + t + " at stack " + i + ", got " + curStack[i], curPath);
      }
    }

    private void checkArrayStackSubtype(int i, String t) {
      if (!t.equals(Constants.TYPE_byte) || !"[Z".equals(curStack[i])) {
        checkStackSubtype(i, Util.makeArray(t));
      }
    }

    public void visitConstant(ConstantInstruction instruction) {
      // make sure that constants are checked
      instruction.getValue();
    }

    public void visitGoto(GotoInstruction instruction) {
    }

    public void visitLocalLoad(LoadInstruction instruction) {
      String t = curLocals[instruction.getVarIndex()];
      if (t == null) {
        ex = new FailureException(curIndex, "Local variable " + instruction.getVarIndex() + " is not defined", curPath);
      }
      if (!isSubtypeOf(t, instruction.getType())) {
        ex = new FailureException(curIndex, "Expected type " + instruction.getType() + " for local " + instruction.getVarIndex()
            + ", got " + t, curPath);
      }
    }

    public void visitLocalStore(StoreInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
    }

    public void visitArrayLoad(ArrayLoadInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_int);
      checkArrayStackSubtype(1, instruction.getType());
    }

    public void visitArrayStore(ArrayStoreInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
      checkStackSubtype(1, Constants.TYPE_int);
      checkArrayStackSubtype(2, instruction.getType());
    }

    public void visitPop(PopInstruction instruction) {
    }

    public void visitDup(DupInstruction instruction) {
    }

    public void visitBinaryOp(BinaryOpInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
      checkStackSubtype(1, instruction.getType());
    }

    public void visitUnaryOp(UnaryOpInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
    }

    public void visitShift(ShiftInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_int);
      checkStackSubtype(1, instruction.getType());
    }

    public void visitConversion(ConversionInstruction instruction) {
      checkStackSubtype(0, instruction.getFromType());
    }

    public void visitComparison(ComparisonInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
      checkStackSubtype(1, instruction.getType());
    }

    public void visitConditionalBranch(ConditionalBranchInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
      checkStackSubtype(1, instruction.getType());
    }

    public void visitSwitch(SwitchInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_int);
    }

    public void visitReturn(ReturnInstruction instruction) {
      if (instruction.getType() != Constants.TYPE_void) {
        checkStackSubtype(0, instruction.getType());
        checkStackSubtype(0, Util.getReturnType(signature));
      }
    }

    public void visitGet(GetInstruction instruction) {
      // make sure constant pool entries are dereferenced
      String classType = instruction.getClassType();

      if (!instruction.isStatic()) {
        checkStackSubtype(0, classType);
      }
    }

    public void visitPut(PutInstruction instruction) {
      // make sure constant pool entries are dereferenced
      String classType = instruction.getClassType();
      String type = instruction.getFieldType();

      checkStackSubtype(0, type);
      if (!instruction.isStatic()) {
        checkStackSubtype(1, classType);
      }
    }

    public void visitInvoke(InvokeInstruction instruction) {
      // make sure constant pool entries are dereferenced
      String classType = instruction.getClassType();
      String signature = instruction.getMethodSignature();

      String thisClass = instruction.getInvocationMode() == Constants.OP_invokestatic ? null : classType;
      String[] params = Util.getParamsTypes(thisClass, signature);

      for (int i = 0; i < params.length; i++) {
        checkStackSubtype(i, params[params.length - 1 - i]);
      }
    }

    public void visitNew(NewInstruction instruction) {
      for (int i = 0; i < instruction.getArrayBoundsCount(); i++) {
        checkStackSubtype(i, Constants.TYPE_int);
      }
      // make sure constant is dereferenced
      instruction.getType();
    }

    public void visitArrayLength(ArrayLengthInstruction instruction) {
      if (!curStack[0].equals(Constants.TYPE_null) && !Util.isArrayType(curStack[0])) {
        ex = new FailureException(curIndex, "Expected array type at stack 0, got " + curStack[0], curPath);
      }
    }

    public void visitThrow(ThrowInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_Throwable);
    }

    public void visitMonitor(MonitorInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_Object);
    }

    public void visitCheckCast(CheckCastInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_Object);
      // make sure constant is dereferenced
      instruction.getType();
    }

    public void visitInstanceof(InstanceofInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_Object);
      // make sure constant is dereferenced
      instruction.getType();
    }
  }

  /**
   * Initialize a verifier.
   */
  public Verifier(boolean isStatic, String classType, String signature, Instruction[] instructions, ExceptionHandler[][] handlers) {
    super(isStatic, classType, signature, instructions, handlers);
  }

  /**
   * Initialize a verifier.
   * @throws NullPointerException  if info is null
   */
  public Verifier(MethodData info) throws NullPointerException {
    super(info);
  }

  /**
   * Try to verify the method. If verification is unsuccessful, we throw an
   * exception.
   * 
   * @throws FailureException
   *           the method contains invalid bytecode
   */
  public void verify() throws FailureException {
    VerifyVisitor v = new VerifyVisitor();
    computeTypes(v, getBasicBlockStarts(), true);
    v.checkError();
  }

  public void verifyCollectAll() throws FailureException {
    VerifyVisitor v = new VerifyVisitor();
    BitSet all = new BitSet(instructions.length);
    all.set(0, instructions.length);
    computeTypes(v, all, true);
    v.checkError();
  }

  public void computeTypes() throws FailureException {
    computeTypes(null, getBasicBlockStarts(), false);
  }
}