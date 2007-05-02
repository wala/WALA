package com.ibm.wala.core.tests.logic;

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


import java.util.Collection;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.logic.BasicTheory;
import com.ibm.wala.logic.BasicVocabulary;
import com.ibm.wala.logic.BinaryFormula;
import com.ibm.wala.logic.BinaryRelation;
import com.ibm.wala.logic.FunctionTerm;
import com.ibm.wala.logic.IFormula;
import com.ibm.wala.logic.IFunction;
import com.ibm.wala.logic.ILogicConstants;
import com.ibm.wala.logic.IRelation;
import com.ibm.wala.logic.ITheory;
import com.ibm.wala.logic.IntConstant;
import com.ibm.wala.logic.NotFormula;
import com.ibm.wala.logic.QuantifiedFormula;
import com.ibm.wala.logic.RelationFormula;
import com.ibm.wala.logic.Simplifier;
import com.ibm.wala.logic.UnaryFunction;
import com.ibm.wala.logic.UnaryRelation;
import com.ibm.wala.logic.Variable;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * @author Satish Chandra
 *
 */
public class FormulaTest extends WalaTestCase {

  public FormulaTest() {
    super("Formula Test");
  }

  // F1: f(x,y) /\ g(z)
  public void testF1() {
    Variable x = Variable.make(1, null);
    Variable y = Variable.make(2, null);
    Variable z = Variable.make(3, null);
    RelationFormula fxy = RelationFormula.make(BinaryRelation.make("f"), x, y);
    RelationFormula gz = RelationFormula.make(UnaryRelation.make("g"), z);
    IFormula f1 = BinaryFormula.make(ILogicConstants.BinaryConnective.AND, fxy, gz);
    System.err.println(f1);
  }

  // F2: !(t(p) == q)
  public void testF2() {
    Variable p = Variable.make(4, null);
    Variable q = Variable.make(5, null);
    IFormula f2 = NotFormula.make(RelationFormula.makeEquals(FunctionTerm.make(UnaryFunction.make("t"), p), q));
    System.err.println(f2);
  }

  // F3: \exists s. (s > 0)
  public void testF3() {
    Variable s = Variable.make(6, null);
    IFormula f3 = QuantifiedFormula.make(ILogicConstants.Quantifier.EXISTS, s, RelationFormula.make(BinaryRelation.make(">"), s,
        IntConstant.make(0)));
    System.err.println(f3);
  }

  // F4: In F2, substitute p by q
  public void testF4() {
    Variable p = Variable.make(4, null);
    Variable q = Variable.make(5, null);
    IFormula f2 = NotFormula.make(RelationFormula.makeEquals(FunctionTerm.make(UnaryFunction.make("t"), p), q));
    IFormula f4 = Simplifier.substitute(f2, p, q);
    System.err.println(f4);
  }

  public void testTH1() {
    // Build a "theory"

    Variable x = Variable.make(1, null);
    Variable y = Variable.make(2, null);
    Variable z = Variable.make(3, null);
    RelationFormula fxy = RelationFormula.make(BinaryRelation.make("f"), x, y);
    RelationFormula gz = RelationFormula.make(UnaryRelation.make("g"), z);
    IFormula f1 = BinaryFormula.make(ILogicConstants.BinaryConnective.AND, fxy, gz);

    Variable p = Variable.make(4, null);
    Variable q = Variable.make(5, null);
    IFormula f2 = NotFormula.make(RelationFormula.makeEquals(FunctionTerm.make(UnaryFunction.make("t"), p), q));

    Collection<IFormula> sentences = HashSetFactory.make();
    sentences.add(f1);
    sentences.add(f2);

    Collection<IFunction> funcs = HashSetFactory.make();
    funcs.add(UnaryFunction.make("t"));

    Collection<IRelation> rels = HashSetFactory.make();
    rels.add(BinaryRelation.make("f"));
    rels.add(UnaryRelation.make("g"));

    ITheory th1 = BasicTheory.make(BasicVocabulary.make(funcs, rels), sentences);
    System.err.println(th1.toString());
  }
}
