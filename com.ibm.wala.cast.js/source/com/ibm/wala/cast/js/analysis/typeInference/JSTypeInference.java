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

import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.analysis.typeInference.*;
import com.ibm.wala.cast.analysis.typeInference.AstTypeInference;
import com.ibm.wala.cast.js.ssa.*;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.TypeReference;

public class JSTypeInference extends AstTypeInference {

  public JSTypeInference(IR ir, ClassHierarchy cha) {
    super(ir, cha, true);
  }

  protected void initialize() {
    class JSTypeOperatorFactory
	extends AstTypeOperatorFactory
	implements com.ibm.wala.cast.js.ssa.InstructionVisitor 
    {
      public void visitJavaScriptInvoke(JavaScriptInvoke inst) {
	result = new DeclaredTypeOperator(new ConeType(cha.getRootClass(), cha));
      }
      public void visitJavaScriptPropertyRead(JavaScriptPropertyRead inst) {
	result = new DeclaredTypeOperator(new ConeType(cha.getRootClass(), cha));
      }
      public void visitTypeOf(JavaScriptTypeOfInstruction inst) {
	result = new DeclaredTypeOperator(new PointType(cha.lookupClass(JavaScriptTypes.String), cha));
      }

      public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite inst) {
      }
    };

    class JSTypeVarFactory extends TypeVarFactory {

      private TypeAbstraction make(TypeReference typeRef) {
	return new PointType(cha.lookupClass( typeRef ), cha);
      }

      public IVariable makeVariable(int vn) {
	if (ir.getSymbolTable().isStringConstant( vn )) {
	  return new TypeVariable(make(JavaScriptTypes.String), 1331*vn);
	} else if (ir.getSymbolTable().isBooleanConstant( vn )) {
	  return new TypeVariable(make(JavaScriptTypes.Boolean), 4197*vn);
	} else if (ir.getSymbolTable().isNullConstant( vn )) {
	  return new TypeVariable(make(JavaScriptTypes.Null), 4077*vn);
	} else if (ir.getSymbolTable().isNumberConstant( vn )) {
	  return new TypeVariable(make(JavaScriptTypes.Number), 797*vn);
	} else {
	  return super.makeVariable( vn );
	}
      }
    };

    init(ir, new JSTypeVarFactory(), new JSTypeOperatorFactory());
  }

  public TypeAbstraction getConstantType(int valueNumber) {
    SymbolTable st = ir.getSymbolTable();
    if (st.isStringConstant(valueNumber)) {	  
      return new PointType(cha.lookupClass(JavaScriptTypes.String), cha);
    } else if (st.isBooleanConstant( valueNumber )) {
      return new PointType(cha.lookupClass(JavaScriptTypes.Boolean), cha);
    } else if (st.isNullConstant(valueNumber)) {	  
      return new PointType(cha.lookupClass(JavaScriptTypes.Null), cha);
    } else {
      return new PointType(cha.lookupClass(JavaScriptTypes.Number), cha);
    }
  }
}

