/*
 * Copyright (c) 2009 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

public interface AstInstructionFactory extends SSAInstructionFactory {

  AssignInstruction AssignInstruction(int iindex, int result, int val);

  AstAssertInstruction AssertInstruction(int iindex, int value, boolean fromSpecification);

  AstEchoInstruction EchoInstruction(int iindex, int[] rvals);

  AstGlobalRead GlobalRead(int iindex, int lhs, FieldReference global);

  AstGlobalWrite GlobalWrite(int iindex, FieldReference global, int rhs);

  AstIsDefinedInstruction IsDefinedInstruction(
      int iindex, int lval, int rval, int fieldVal, FieldReference fieldRef);

  AstIsDefinedInstruction IsDefinedInstruction(
      int iindex, int lval, int rval, FieldReference fieldRef);

  AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval, int fieldVal);

  AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval);

  AstLexicalRead LexicalRead(int iindex, Access[] accesses);

  AstLexicalRead LexicalRead(int iindex, Access access);

  AstLexicalRead LexicalRead(
      int iindex, int lhs, String definer, String globalName, TypeReference type);

  AstLexicalWrite LexicalWrite(int iindex, Access[] accesses);

  AstLexicalWrite LexicalWrite(int iindex, Access access);

  AstLexicalWrite LexicalWrite(
      int iindex, String definer, String globalName, TypeReference type, int rhs);

  EachElementGetInstruction EachElementGetInstruction(
      int iindex, int lValue, int objectRef, int previousProp);

  EachElementHasNextInstruction EachElementHasNextInstruction(
      int iindex, int lValue, int objectRef, int previousProp);

  AstPropertyRead PropertyRead(int iindex, int result, int objectRef, int memberRef);

  AstPropertyWrite PropertyWrite(int iindex, int objectRef, int memberRef, int value);

  AstYieldInstruction YieldInstruction(int iindex, int[] rvals);
}
