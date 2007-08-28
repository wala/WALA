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
package com.ibm.wala.core.tests.logic;

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

import com.ibm.wala.logic.AdHocSemiDecisionProcedure;
import com.ibm.wala.logic.BinaryFormula;
import com.ibm.wala.logic.BinaryRelation;
import com.ibm.wala.logic.BooleanConstant;
import com.ibm.wala.logic.BooleanConstantFormula;
import com.ibm.wala.logic.IFormula;
import com.ibm.wala.logic.IntVariable;
import com.ibm.wala.logic.NotFormula;
import com.ibm.wala.logic.QuantifiedFormula;
import com.ibm.wala.logic.RelationFormula;
import com.ibm.wala.logic.Simplifier;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * tests of logic simplification
 * 
 * @author sjfink
 */
public class SimplifyTest extends TestCase {

  /**
   * before: v1 = 0 
   * theory: empty 
   * after : v1 = 0
   */
  public void test1() {
    IntVariable v = IntVariable.make(1);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();
    c = Simplifier.propositionalSimplify(c, t, AdHocSemiDecisionProcedure.singleton());
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(f));
  }

  /**
   * before: v1 = 0 
   * theory: v1 = 0 
   * after : true
   */
  public void test2() {
    IntVariable v = IntVariable.make(1);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();
    t.add(f);
    c = Simplifier.propositionalSimplify(c, t, AdHocSemiDecisionProcedure.singleton());
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(BooleanConstantFormula.TRUE));
  }

  /**
   * before: v1 = 0 
   * theory: v1 /= 0 
   * after : false
   */
  public void test3() {
    IntVariable v = IntVariable.make(1);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();
    t.add(NotFormula.make(f));
    c = Simplifier.propositionalSimplify(c, t, AdHocSemiDecisionProcedure.singleton());
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(BooleanConstantFormula.FALSE));
  }

  /**
   * before: v1 = 0 
   * theory: (true = true) <=> (v1 /= 0) 
   * after : false
   */
  public void test4() {
    IntVariable v = IntVariable.make(1);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();

    IFormula lhs = RelationFormula.makeEquals(BooleanConstant.TRUE, BooleanConstant.TRUE);
    IFormula rhs = NotFormula.make(f);
    IFormula axiom = BinaryFormula.biconditional(lhs, rhs);
    t.add(axiom);
    c = Simplifier.propositionalSimplify(c, t, AdHocSemiDecisionProcedure.singleton());
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(BooleanConstantFormula.FALSE));
  }

  // this simplification is disabled; doesn't always make sense.
//  /**
//   * before: v1 = 0 theory: 
//   * FORALL v1. v1 = 0 
//   * after : true
//   */
//  public void test5() {
//    IntVariable v = IntVariable.make(1);
//    IFormula f = RelationFormula.makeEquals(v, 0);
//    System.out.println("before: " + f);
//    assertTrue(f != null);
//
//    Collection<IFormula> c = Collections.singleton(f);
//    Collection<IFormula> t = HashSetFactory.make();
//
//    IFormula axiom = QuantifiedFormula.forall(v, f);
//    t.add(axiom);
//    c = Simplifier.simplify(c, t, AdHocSemiDecisionProcedure.singleton());
//    for (IFormula x : c) {
//      System.out.println("after : " + x);
//    }
//    assertTrue(c.size() == 1);
//    assertTrue(c.iterator().next().equals(BooleanConstantFormula.TRUE));
//  }
//
//  /**
//   * before: foo(v1,v1) 
//   * theory: FORALL v1. foo(v1,v1) 
//   * after : true
 //    *
 //    * Quantifier simplification logic currently disabled
//   */
//  public void test6() {
//    IntVariable v = IntVariable.make(1);
//    BinaryRelation foo = BinaryRelation.make("foo");
//    IFormula f = RelationFormula.make(foo, v, v);
//    System.out.println("before: " + f);
//    assertTrue(f != null);
//
//    Collection<IFormula> c = Collections.singleton(f);
//    Collection<IFormula> t = HashSetFactory.make();
//
//    IFormula axiom = QuantifiedFormula.forall(v, f);
//    t.add(axiom);
//    c = Simplifier.simplify(c, t, AdHocSemiDecisionProcedure.singleton());
//    for (IFormula x : c) {
//      System.out.println("after : " + x);
//    }
//    assertTrue(c.size() == 1);
//    assertTrue(c.iterator().next().equals(BooleanConstantFormula.TRUE));
//  }

  /**
   * before: foo(v1,v2) 
   * theory: FORALL v1. foo(v1,v1) 
   * after : foo(v1,v2)
   */
  public void test7() {
    IntVariable v1 = IntVariable.make(1);
    IntVariable v2 = IntVariable.make(2);
    BinaryRelation foo = BinaryRelation.make("foo");
    IFormula f = RelationFormula.make(foo, v1, v2);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();

    IFormula g = RelationFormula.make(foo, v1, v1);
    IFormula axiom = QuantifiedFormula.forall(v1, g);
    t.add(axiom);
    c = Simplifier.simplify(c, t, AdHocSemiDecisionProcedure.singleton());
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(f));
  }
  
  /**
   * before: foo(v1,v2) 
   * theory: FORALL v2. FORALL v1. foo(v1,v1) 
   * after : foo(v1,v2)
   */
  public void test8() {
    IntVariable v1 = IntVariable.make(1);
    IntVariable v2 = IntVariable.make(2);
    BinaryRelation foo = BinaryRelation.make("foo");
    IFormula f = RelationFormula.make(foo, v1, v2);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();

    IFormula g = RelationFormula.make(foo, v1, v1);
    IFormula axiom = QuantifiedFormula.forall(v1, g);
    axiom = QuantifiedFormula.forall(v2, axiom);
    t.add(axiom);
    c = Simplifier.simplify(c, t, AdHocSemiDecisionProcedure.singleton());
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(f));
  }
  
  /**
   * before: foo(v1,v2) 
   * theory: FORALL v1. FORALL v2. foo(v1,v1) 
   * after : foo(v1,v2)
   */
  public void test9() {
    IntVariable v1 = IntVariable.make(1);
    IntVariable v2 = IntVariable.make(2);
    BinaryRelation foo = BinaryRelation.make("foo");
    IFormula f = RelationFormula.make(foo, v1, v2);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();

    IFormula g = RelationFormula.make(foo, v1, v1);
    IFormula axiom = QuantifiedFormula.forall(v2, g);
    axiom = QuantifiedFormula.forall(v1, axiom);
    t.add(axiom);
    c = Simplifier.simplify(c, t, AdHocSemiDecisionProcedure.singleton());
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(f));
  }
  
//  /**
//   * before: foo(0) 
//   * theory: FORALL v1. foo(v1) 
//   * after : true
//  Quantifier simplification logic currently disabled
//   */
//  public void test10() {
//    UnaryRelation foo = UnaryRelation.make("foo");
//    IFormula f = RelationFormula.make(foo, IntConstant.make(0));
//    System.out.println("before: " + f);
//    assertTrue(f != null);
//
//    Collection<IFormula> c = Collections.singleton(f);
//    Collection<IFormula> t = HashSetFactory.make();
//    IntVariable v1 = IntVariable.make(1);
//    IFormula g = RelationFormula.make(foo, v1);
//    IFormula axiom = QuantifiedFormula.forall(v1, g);
//    t.add(axiom);
//    c = Simplifier.simplify(c, t, AdHocSemiDecisionProcedure.singleton());
//    for (IFormula x : c) {
//      System.out.println("after : " + x);
//    }
//    assertTrue(c.size() == 1);
//    assertTrue(c.iterator().next().equals(BooleanConstantFormula.TRUE));
//  }
  
  // this simplification is disabled. doesn't always make sense.
//  /**
//   * before: foo(3,4) == 4
//   * theory: FORALL v1. FORALL v2. foo(v1, v2) == v2 
//   * after : true
//   */
//  public void test11() {
//    BinaryFunction foo = BinaryFunction.make("foo");
//    IFormula f = RelationFormula.makeEquals(FunctionTerm.make(foo,IntConstant.make(3), IntConstant.make(4)), IntConstant.make(4));
//    System.out.println("before: " + f);
//    assertTrue(f != null);
//
//    Collection<IFormula> c = Collections.singleton(f);
//    Collection<IFormula> t = HashSetFactory.make();
//    IntVariable v1 = IntVariable.make(1);
//    IntVariable v2 = IntVariable.make(2);
//    IFormula g = RelationFormula.makeEquals(FunctionTerm.make(foo,v1, v2), v2);
//    IFormula axiom = QuantifiedFormula.forall(v1, v2, g);
//    t.add(axiom);
//    c = Simplifier.simplify(c, t, AdHocSemiDecisionProcedure.singleton());
//    for (IFormula x : c) {
//      System.out.println("after : " + x);
//    }
//    assertTrue(c.size() == 1);
//    assertTrue(c.iterator().next().equals(BooleanConstantFormula.TRUE));
//  }
}
