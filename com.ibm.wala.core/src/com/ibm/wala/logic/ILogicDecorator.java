/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.logic.ILogicConstants.Quantifier;

public interface ILogicDecorator {

  String prettyPrint(BinaryConnective b);

  String prettyPrint(BooleanConstant c);

  String prettyPrint(AbstractNumberedVariable v);

  String prettyPrint(Quantifier quantifier);

  String prettyPrint(IConstant constant);

  String prettyPrint(FunctionTerm term);

  String prettyPrint(RelationFormula formula);
  
  String prettyPrint(IRelation r);

  String prettyPrint(AbstractBinaryFormula binaryFormula);
  
  String prettyPrint(NotFormula notFormula);

}
