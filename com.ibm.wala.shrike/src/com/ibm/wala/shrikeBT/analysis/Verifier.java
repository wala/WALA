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
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.GotoInstruction;
import com.ibm.wala.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IConversionInstruction;
import com.ibm.wala.shrikeBT.IGetInstruction;
import com.ibm.wala.shrikeBT.IInstanceofInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.ILoadInstruction;
import com.ibm.wala.shrikeBT.IPutInstruction;
import com.ibm.wala.shrikeBT.IShiftInstruction;
import com.ibm.wala.shrikeBT.IStoreInstruction;
import com.ibm.wala.shrikeBT.ITypeTestInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.InvokeDynamicInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.PopInstruction;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
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
 * For full verification you need to provide class hierarchy information using setClassHierarchy. Without this information, we can't
 * compute the exact types of variables at control flow merge points. If you don't provide a hierarchy, or the hierarchy you provide
 * is partial, then the Verifier will be optimistic.
 * 
 * This method can also be used to gather type information for every stack and local variable at every program point. Just call
 * computeTypes() instead of verify() and then retrieve the results with getLocalTypes() and getStackTypes().
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

    @Override
    public void setState(int offset, List<PathElement> path, String[] curStack, String[] curLocals) {
      curIndex = offset;
      curPath = path;
      this.curStack = curStack;
      this.curLocals = curLocals;
    }

    @Override
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

    @Override
    public void visitConstant(ConstantInstruction instruction) {
      // make sure that constants are checked
      instruction.getValue();
    }

    @Override
    public void visitGoto(GotoInstruction instruction) {
    }

    @Override
    public void visitLocalLoad(ILoadInstruction instruction) {
      String t = curLocals[instruction.getVarIndex()];
      if (t == null) {
        ex = new FailureException(curIndex, "Local variable " + instruction.getVarIndex() + " is not defined", curPath);
      }
      if (!isSubtypeOf(t, instruction.getType())) {
        ex = new FailureException(curIndex, "Expected type " + instruction.getType() + " for local " + instruction.getVarIndex()
            + ", got " + t, curPath);
      }
    }

    @Override
    public void visitLocalStore(IStoreInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
    }

    @Override
    public void visitArrayLoad(IArrayLoadInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_int);
      checkArrayStackSubtype(1, instruction.getType());
    }

    @Override
    public void visitArrayStore(IArrayStoreInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
      checkStackSubtype(1, Constants.TYPE_int);
      checkArrayStackSubtype(2, instruction.getType());
    }

    @Override
    public void visitPop(PopInstruction instruction) {
    }

    @Override
    public void visitDup(DupInstruction instruction) {
    }

    @Override
    public void visitBinaryOp(IBinaryOpInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
      checkStackSubtype(1, instruction.getType());
    }

    @Override
    public void visitUnaryOp(IUnaryOpInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
    }

    @Override
    public void visitShift(IShiftInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_int);
      checkStackSubtype(1, instruction.getType());
    }

    @Override
    public void visitConversion(IConversionInstruction instruction) {
      checkStackSubtype(0, instruction.getFromType());
    }

    @Override
    public void visitComparison(IComparisonInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
      checkStackSubtype(1, instruction.getType());
    }

    @Override
    public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
      checkStackSubtype(0, instruction.getType());
      checkStackSubtype(1, instruction.getType());
    }

    @Override
    public void visitSwitch(SwitchInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_int);
    }

    @Override
    public void visitReturn(ReturnInstruction instruction) {
      if (instruction.getType() != Constants.TYPE_void) {
        checkStackSubtype(0, instruction.getType());
        checkStackSubtype(0, Util.getReturnType(signature));
      }
    }

    @Override
    public void visitGet(IGetInstruction instruction) {
      // make sure constant pool entries are dereferenced
      String classType = instruction.getClassType();

      if (!instruction.isStatic()) {
        checkStackSubtype(0, classType);
      }
    }

    @Override
    public void visitPut(IPutInstruction instruction) {
      // make sure constant pool entries are dereferenced
      String classType = instruction.getClassType();
      String type = instruction.getFieldType();

      checkStackSubtype(0, type);
      if (!instruction.isStatic()) {
        checkStackSubtype(1, classType);
      }
    }

    @Override
    public void visitInvoke(IInvokeInstruction instruction) {
      if (instruction instanceof InvokeDynamicInstruction) {
        return;
      }
      
      // make sure constant pool entries are dereferenced
      String classType = instruction.getClassType();
      String signature = instruction.getMethodSignature();

      String thisClass = instruction.getInvocationCode() == IInvokeInstruction.Dispatch.STATIC ? null : classType;            
      String[] params = Util.getParamsTypes(thisClass, signature);

      for (int i = 0; i < params.length; i++) {
        checkStackSubtype(i, params[params.length - 1 - i]);
      }
    }

    @Override
    public void visitNew(NewInstruction instruction) {
      for (int i = 0; i < instruction.getArrayBoundsCount(); i++) {
        checkStackSubtype(i, Constants.TYPE_int);
      }
      // make sure constant is dereferenced
      instruction.getType();
    }

    @Override
    public void visitArrayLength(ArrayLengthInstruction instruction) {
      if (!curStack[0].equals(Constants.TYPE_null) && !Util.isArrayType(curStack[0])) {
        ex = new FailureException(curIndex, "Expected array type at stack 0, got " + curStack[0], curPath);
      }
    }

    @Override
    public void visitThrow(ThrowInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_Throwable);
    }

    @Override
    public void visitMonitor(MonitorInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_Object);
    }

    @Override
    public void visitCheckCast(ITypeTestInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_Object);
      // make sure constant is dereferenced
      instruction.getTypes();
    }

    @Override
    public void visitInstanceof(IInstanceofInstruction instruction) {
      checkStackSubtype(0, Constants.TYPE_Object);
      // make sure constant is dereferenced
      instruction.getType();
    }
  }

  /**
   * Initialize a verifier.
   */
  public Verifier(boolean isConstructor, boolean isStatic, String classType, String signature, IInstruction[] instructions, ExceptionHandler[][] handlers, int[] instToBC, String[][] vars) {
    super(isConstructor, isStatic, classType, signature, instructions, handlers, instToBC, vars);
  }

  /**
   * Initialize a verifier.
   * 
   * @throws NullPointerException if info is null
   */
  public Verifier(MethodData info) throws NullPointerException {
    super(info);
  }

  public Verifier(MethodData info, int[] instToBC, String[][] vars) throws NullPointerException {
    super(info, instToBC, vars);
  }

  /**
   * Try to verify the method. If verification is unsuccessful, we throw an exception.
   * 
   * @throws FailureException the method contains invalid bytecode
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
