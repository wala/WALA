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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntPair;

/**
 * Utilities for simplifying logic expressions
 * 
 * @author sjfink
 * 
 */
public class Simplifier {

  public static ITheory eliminateQuantifiers(ITheory t) {
    if (t == null) {
      throw new IllegalArgumentException("t is null");
    }
    Collection<IFormula> sentences = HashSetFactory.make();
    for (IFormula s : t.getSentences()) {
      sentences.addAll(eliminateQuantifiers(s));
    }
    return BasicTheory.make(t.getVocabulary(), sentences);
  }

  private static Collection<? extends IFormula> eliminateQuantifiers(IFormula s) {
    if (s.getKind().equals(IFormula.Kind.QUANTIFIED)) {
      Collection<IFormula> result = HashSetFactory.make();
      QuantifiedFormula f = (QuantifiedFormula) s;
      Variable v = f.getBoundVar();
      IntPair range = v.getRange();
      assert range.getX() >= 0;
      assert range.getY() >= range.getX();
      for (int i = range.getX(); i <= range.getY(); i++) {
        result.add(substitute(f.getFormula(), v, IntConstant.make(i)));
      }
      return result;
    } else {
      return Collections.singleton(s);
    }
  }

  public static Collection<? extends IFormula> eliminateQuantifiers(Collection<? extends IFormula> s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    Collection<IFormula> result = HashSetFactory.make();
    for (IFormula f : s) {
      result.addAll(eliminateQuantifiers(f));
    }
    return result;
  }

  /**
   * this is uglified a little to avoid tail recursion, which leads to stack
   * overflow.
   * 
   * This is inefficient.
   */
  public static Collection<IFormula> simplify(Collection<IFormula> s, ITheory t) {
    boolean changed = true;
    while (changed) {
      changed = false;
      Collection<IFormula> alreadyUsed = HashSetFactory.make();
      Pair<ITerm, ITerm> substitution = getNextSubCandidate(s, t, alreadyUsed);
      while (substitution != null) {
        Collection<IFormula> temp = HashSetFactory.make();
        for (IFormula f : s) {
          if (!defines(f, substitution.fst)) {
            IFormula f2 = substitute(f, substitution.fst, substitution.snd);
            temp.add(f2);
            if (!f.equals(f2)) {
              changed = true;
            }
          } else {
            temp.add(f);
          }
        }
        s = temp;
        substitution = getNextSubCandidate(s, t, alreadyUsed);
      }
    }
    return trivialDecision(s);
  }

  /**
   * Check simple satisfiability rules for each formula in a collection. Replace
   * tautologies with "true" and contradictions with false.
   */
  private static Collection<IFormula> trivialDecision(Collection<IFormula> s) {
    Collection<IFormula> result = HashSetFactory.make();
    for (IFormula f : s) {
      if (isTautology(f)) {
        result.add(BooleanConstantFormula.TRUE);
      } else if (isContradiction(f)) {
        result.add(BooleanConstantFormula.FALSE);
      } else {
        result.add(f);
      }
    }
    return result;
  }

  /**
   * @return true if we can easily prove f is a contradiction
   */
  private static boolean isContradiction(IFormula f) {
    if (f.getKind().equals(IFormula.Kind.RELATION)) {
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs.getKind().equals(ITerm.Kind.CONSTANT) && rhs.getKind().equals(ITerm.Kind.CONSTANT)) {
          if (!lhs.equals(rhs)) {
            return true;
          }
        }
      }
    } else if (f.getKind().equals(IFormula.Kind.BINARY)) { 
      BinaryFormula b = (BinaryFormula)f;
      if (b.getConnective().equals(BinaryConnective.AND)) {
        if (isContradiction(b.getF1()) || isContradiction(b.getF2())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @return true if we can easily prove f is a tautology
   */
  private static boolean isTautology(IFormula f) {
    if (f.getKind().equals(IFormula.Kind.RELATION)) {
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs.getKind().equals(ITerm.Kind.CONSTANT) && rhs.getKind().equals(ITerm.Kind.CONSTANT)) {
          if (lhs.equals(rhs)) {
            return true;
          }
        }
      }
    } else if (f.getKind().equals(IFormula.Kind.BINARY)) { 
      BinaryFormula b = (BinaryFormula)f;
      if (b.getConnective().equals(BinaryConnective.AND)) {
        if (isTautology(b.getF1()) && isTautology(b.getF2())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean defines(IFormula f, ITerm t) {
    if (f.getKind().equals(IFormula.Kind.RELATION)) {
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
        return r.getTerms().get(0).equals(t);
      }
    }
    return false;
  }

  private static Pair<ITerm, ITerm> getNextSubCandidate(Collection<IFormula> s, ITheory t, Collection<IFormula> alreadyUsed) {
    Collection<IFormula> candidates = HashSetFactory.make();
    candidates.addAll(s);
    candidates.addAll(t.getSentences());
    for (IFormula f : candidates) {
      if (!alreadyUsed.contains(f)) {
        Pair<ITerm, ITerm> substitution = suggestsSubstitution(f);
        if (substitution != null) {
          alreadyUsed.add(f);
          return substitution;
        }
      }
    }
    return null;
  }

  /**
   * does the structure of formula f suggest an immediate substitution to
   * simplify it?
   * 
   * @return a pair (p1, p2) meaning "substitute p2 for p1"
   */
  private static Pair<ITerm, ITerm> suggestsSubstitution(IFormula f) {
    if (f.getKind().equals(IFormula.Kind.RELATION)) {
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (rhs.getKind().equals(ITerm.Kind.CONSTANT) || rhs.getKind().equals(ITerm.Kind.VARIABLE)) {
          return Pair.make(lhs, rhs);
        } else {
          return null;
        }
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * in formula f, substitute the term t2 for all free occurences of t1
   * 
   * @throws IllegalArgumentException
   *           if formula is null
   */
  public static IFormula substitute(IFormula formula, ITerm t1, ITerm t2) {
    if (formula == null) {
      throw new IllegalArgumentException("formula is null");
    }
    switch (formula.getKind()) {
    case BINARY:
      BinaryFormula b = (BinaryFormula) formula;
      return BinaryFormula.make(b.getConnective(), substitute(b.getF1(), t1, t2), substitute(b.getF2(), t1, t2));
    case NEGATION:
      NotFormula n = (NotFormula) formula;
      return NotFormula.make(substitute(n.getFormula(), t1, t2));
    case QUANTIFIED:
      QuantifiedFormula q = (QuantifiedFormula) formula;
      if (q.getBoundVar().equals(t1)) {
        return q;
      } else {
        return QuantifiedFormula.make(q.getQuantifier(), q.getBoundVar(), substitute(q.getFormula(), t1, t2));
      }
    case RELATION:
      RelationFormula r = (RelationFormula) formula;
      List<ITerm> terms = new LinkedList<ITerm>();
      for (ITerm t : r.getTerms()) {
        terms.add(substitute(t, t1, t2));
      }
      return RelationFormula.make(r.getRelation(), terms);
    case CONSTANT:
      return formula;
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /**
   * in term t, substitute t2 for free occurences of t1
   */
  private static ITerm substitute(ITerm t, ITerm t1, ITerm t2) {
    if (t.equals(t1)) {
      return t2;
    }
    switch (t.getKind()) {
    case CONSTANT:
      return t;
    case FUNCTION:
      FunctionTerm f = (FunctionTerm) t;
      List<ITerm> terms = new LinkedList<ITerm>();
      for (ITerm p : f.getParameters()) {
        terms.add(substitute(p, t1, t2));
      }
      return FunctionTerm.make(f.getFunction(), terms);
    case VARIABLE:
      if (t1.equals(t)) {
        return t2;
      } else {
        return t;
      }
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public static Collection<Variable> getFreeVariables(Collection<? extends IFormula> constraints) {
    if (constraints == null) {
      throw new IllegalArgumentException("constraints is null");
    }
    Collection<Variable> free = HashSetFactory.make();
    for (IFormula f : constraints) {
      free.addAll(f.getFreeVariables());
    }
    return free;
  }

}
