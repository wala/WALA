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
package com.ibm.wala.cast.js.analysis.typeInference;

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeVariable;
import com.ibm.wala.cast.analysis.typeInference.AstTypeInference;
import com.ibm.wala.cast.js.ssa.JavaScriptCheckReference;
import com.ibm.wala.cast.js.ssa.JavaScriptInstanceOf;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.cast.js.ssa.JavaScriptWithRegion;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.ssa.SetPrototype;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

public class JSTypeInference extends AstTypeInference {

  public JSTypeInference(IR ir, IClassHierarchy cha) {
    super(ir, new PointType(cha.lookupClass(JavaScriptTypes.Boolean)), true);
  }

  @Override
  protected void initialize() {
    class JSTypeOperatorFactory extends AstTypeOperatorFactory implements com.ibm.wala.cast.js.ssa.JSInstructionVisitor {
      @Override
      public void visitJavaScriptInvoke(JavaScriptInvoke inst) {
        result = new DeclaredTypeOperator(new ConeType(cha.getRootClass()));
      }

      @Override
      public void visitJavaScriptPropertyRead(JavaScriptPropertyRead inst) {
        result = new DeclaredTypeOperator(new ConeType(cha.getRootClass()));
      }

      @Override
      public void visitTypeOf(JavaScriptTypeOfInstruction inst) {
        result = new DeclaredTypeOperator(new PointType(cha.lookupClass(JavaScriptTypes.String)));
      }

      @Override
      public void visitJavaScriptInstanceOf(JavaScriptInstanceOf inst) {
        result = new DeclaredTypeOperator(new PointType(cha.lookupClass(JavaScriptTypes.Boolean)));
      }

      @Override
      public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite inst) {
      }

      @Override
      public void visitCheckRef(JavaScriptCheckReference instruction) {
      }

      @Override
      public void visitWithRegion(JavaScriptWithRegion instruction) {
      }

      @Override
      public void visitSetPrototype(SetPrototype instruction) {
      }

      @Override
      public void visitPrototypeLookup(PrototypeLookup instruction) {
        result = new DeclaredTypeOperator(new ConeType(cha.getRootClass()));
      }
    }

    class JSTypeVarFactory extends TypeVarFactory {

      private TypeAbstraction make(TypeReference typeRef) {
        return new PointType(cha.lookupClass(typeRef));
      }

      @Override
      public IVariable makeVariable(int vn) {
        if (ir.getSymbolTable().isStringConstant(vn)) {
          return new TypeVariable(make(JavaScriptTypes.String));
        } else if (ir.getSymbolTable().isBooleanConstant(vn)) {
          return new TypeVariable(make(JavaScriptTypes.Boolean));
        } else if (ir.getSymbolTable().isNullConstant(vn)) {
          return new TypeVariable(make(JavaScriptTypes.Null));
        } else if (ir.getSymbolTable().isNumberConstant(vn)) {
          return new TypeVariable(make(JavaScriptTypes.Number));
        } else {
          return super.makeVariable(vn);
        }
      }
    }

    init(ir, new JSTypeVarFactory(), new JSTypeOperatorFactory());
  }

  @Override
  public TypeAbstraction getConstantType(int valueNumber) {
    SymbolTable st = ir.getSymbolTable();
    if (st.isStringConstant(valueNumber)) {
      return new PointType(cha.lookupClass(JavaScriptTypes.String));
    } else if (st.isBooleanConstant(valueNumber)) {
      return new PointType(cha.lookupClass(JavaScriptTypes.Boolean));
    } else if (st.isNullConstant(valueNumber)) {
      return new PointType(cha.lookupClass(JavaScriptTypes.Null));
    } else {
      return new PointType(cha.lookupClass(JavaScriptTypes.Number));
    }
  }
}
