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

import com.ibm.wala.logic.BinaryFormula;
import com.ibm.wala.logic.BooleanConstant;
import com.ibm.wala.logic.BooleanConstantFormula;
import com.ibm.wala.logic.IFormula;
import com.ibm.wala.logic.NotFormula;
import com.ibm.wala.logic.QuantifiedFormula;
import com.ibm.wala.logic.RelationFormula;
import com.ibm.wala.logic.Simplifier;
import com.ibm.wala.logic.Variable;
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
    Variable v = Variable.make(1, null);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();
    c = Simplifier.propositionalSimplify(c, t);
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
    Variable v = Variable.make(1, null);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();
    t.add(f);
    c = Simplifier.propositionalSimplify(c, t);
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
    Variable v = Variable.make(1, null);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();
    t.add(NotFormula.make(f));
    c = Simplifier.propositionalSimplify(c, t);
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
    Variable v = Variable.make(1, null);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();

    IFormula lhs = RelationFormula.makeEquals(BooleanConstant.TRUE, BooleanConstant.TRUE);
    IFormula rhs = NotFormula.make(f);
    IFormula axiom = BinaryFormula.biconditional(lhs, rhs);
    t.add(axiom);
    c = Simplifier.propositionalSimplify(c, t);
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(BooleanConstantFormula.FALSE));
  }
  
  /**
   * before: v1 = 0 
   * theory: FORALL v1. v1 = 0
   * after : true
   */
  public void test5() {
    Variable v = Variable.make(1, null);
    IFormula f = RelationFormula.makeEquals(v, 0);
    System.out.println("before: " + f);
    assertTrue(f != null);

    Collection<IFormula> c = Collections.singleton(f);
    Collection<IFormula> t = HashSetFactory.make();

    IFormula axiom = QuantifiedFormula.forall(v, f);
    t.add(axiom);
    c = Simplifier.simplify(c, t);
    for (IFormula x : c) {
      System.out.println("after : " + x);
    }
    assertTrue(c.size() == 1);
    assertTrue(c.iterator().next().equals(BooleanConstantFormula.TRUE));
  }

}
