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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.logic.ILogicConstants.Quantifier;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntPair;

/**
 * Utilities for simplifying logic expressions
 * 
 * @author sjfink
 */
public class Simplifier {

  private final static boolean DEBUG = false;

  /**
   * Eliminate quantifiers, by substituting every possible constant value for a
   * quantified variable
   */
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

  /**
   * Eliminate quantifiers, by substituting every possible constant value for a
   * quantified variable
   */
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
  public static Collection<IFormula> simplify(Collection<IFormula> s, Collection<? extends IFormula> theory) {
    boolean changed = true;
    while (changed) {
      changed = false;
      Collection<IFormula> alreadyUsed = HashSetFactory.make();
      Pair<ITerm, ITerm> substitution = getNextEqualitySubstitution(s, theory, alreadyUsed);
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
        substitution = getNextEqualitySubstitution(s, theory, alreadyUsed);
      }
    }
    return propositionalSimplify(s, theory);
  }

  /**
   * Simplify the set s based on simple propositional logic.
   */
  public static Collection<IFormula> propositionalSimplify(Collection<IFormula> s, Collection<? extends IFormula> t) {
    debug1(s, t);
    Collection<ICNFFormula> cs = toCNF(s);
    Collection<ICNFFormula> ct = toCNF(t);
    debug2(cs, ct);
    Collection<IMaxTerm> facts = collectClauses(ct);

    Collection<IFormula> result = HashSetFactory.make();
    for (ICNFFormula f : cs) {
      Collection<? extends IMaxTerm> d = simplifyCNF(f, facts);
      result.add(CNFFormula.make(d));
    }

    if (DEBUG) {
      System.err.println("--result--");
      for (IFormula f : result) {
        System.err.println(f);
      }
    }
    return result;
  }

  private static Collection<? extends IMaxTerm> simplifyCNF(ICNFFormula f, Collection<IMaxTerm> facts) {
    Collection<IMaxTerm> result = HashSetFactory.make();
    for (IMaxTerm d : collectClauses(Collections.singleton(f))) {
      Collection<IMaxTerm> otherFacts = HashSetFactory.make(facts);
      otherFacts.addAll(collectClauses(Collections.singleton(f)));
      otherFacts.remove(d);
      if (isContradiction(d, otherFacts)) {
        return Collections.singleton(BooleanConstantFormula.FALSE);
      } else if (isTautology(d, facts)) {
        // do nothing.
      } else {
        result.add(d);
      }
    }
    if (result.isEmpty()) {
      return Collections.singleton(BooleanConstantFormula.TRUE);
    }
    return result;
  }

  private static Collection<IMaxTerm> collectClauses(Collection<ICNFFormula> s) {
    Collection<IMaxTerm> result = HashSetFactory.make();
    for (ICNFFormula f : s) {
      if (f instanceof CNFFormula) {
        result.addAll(f.getMaxTerms());
      } else {
        result.add((IMaxTerm) f);
      }
    }
    return result;
  }

  private static void debug2(Collection<ICNFFormula> cs, Collection<ICNFFormula> ct) {
    if (DEBUG) {
      System.err.println("--cs--");
      for (IFormula f : cs) {
        System.err.println(f);
      }
      System.err.println("--ct--");
      for (IFormula f : ct) {
        System.err.println(f);
      }
    }
  }

  private static void debug1(Collection<IFormula> s, Collection<? extends IFormula> t) {
    if (DEBUG) {
      System.err.println("--s--");
      for (IFormula f : s) {
        System.err.println(f);
      }
      System.err.println("--t--");
      for (IFormula f : t) {
        System.err.println(f);
      }
    }
  }

  private static Collection<ICNFFormula> toCNF(Collection<? extends IFormula> s) {
    Collection<ICNFFormula> result = HashSetFactory.make();
    for (IFormula f : s) {
      result.add(CNFFormula.make(f));
    }
    return result;
  }

  /**
   * @param facts
   *            formulae that can be treated as axioms
   * @return true if we can easily prove f is a contradiction
   */
  public static boolean isContradiction(IFormula f, Collection<IMaxTerm> facts) {
    for (IMaxTerm d : facts) {
      if (contradicts(d, f)) {
        return true;
      }
    }
    switch (f.getKind()) {
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) f;
      if (b.getConnective().equals(BinaryConnective.AND)) {
        if (isContradiction(b.getF1(), facts) || isContradiction(b.getF2(), facts)) {
          return true;
        }
        IFormula not1 = NotFormula.make(b.getF1());
        if (implies(b.getF2(), not1)) {
          return true;
        }
        IFormula not2 = NotFormula.make(b.getF2());
        if (implies(b.getF1(), not2)) {
          return true;
        }
      } else if (b.getConnective().equals(BinaryConnective.OR)) {
        if (isContradiction(b.getF1(), facts) && isContradiction(b.getF2(), facts)) {
          return true;
        }
      }
      break;
    case CONSTANT:
      BooleanConstantFormula bc = (BooleanConstantFormula) f;
      return bc.equals(BooleanConstantFormula.FALSE);
    case QUANTIFIED:
      return false;
    case RELATION:
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs.getKind().equals(ITerm.Kind.CONSTANT) && rhs.getKind().equals(ITerm.Kind.CONSTANT)) {
          if (!lhs.equals(rhs)) {
            return true;
          }
        }
      } else if (r.getRelation().equals(BinaryRelation.NE)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs.getKind().equals(ITerm.Kind.CONSTANT) && rhs.getKind().equals(ITerm.Kind.CONSTANT)) {
          if (lhs.equals(rhs)) {
            return true;
          }
        }
      }
      break;
    }
    return false;
  }

  private static boolean contradicts(IMaxTerm axiom, IFormula f) {
    IFormula notF = NotFormula.make(f);
    return implies(axiom, notF);
  }

  private static boolean implies(IFormula axiom, IFormula f) {

    if (normalize(axiom).equals(normalize(f))) {
      return true;
    }
    if (axiom.getKind().equals(IFormula.Kind.QUANTIFIED)) {
      QuantifiedFormula q = (QuantifiedFormula) axiom;

      if (!innerStructureMatches(q, f)) {
        return false;
      }

      if (q.getQuantifier().equals(Quantifier.FORALL)) {
        Variable bound = q.getBoundVar();
        IFormula body = q.getFormula();
        // this could be inefficient. find a better algorithm.
        for (ITerm t : f.getAllTerms()) {
          if (q.getFreeVariables().contains(t)) {
            Variable fresh = makeFresh(q, f);
            IFormula testBody = substitute(body, bound, fresh);
            IFormula testF = substitute(f, t, fresh);
            if (implies(testBody, testF)) {
              return true;
            }
          } else {
            IFormula testBody = substitute(body, bound, t);
            if (implies(testBody, f)) {
              return true;
            }
          }
        }
      }
    }
    // TODO
    // if (f instanceof Disjunction) {
    // Disjunction d = (Disjunction) f;
    // Collection<? extends IFormula> dc = d.getClauses();
    // if (sameValue(c, dc)) {
    // return true;
    // }
    // }
    return false;
  }

  // private static boolean sameValue(Collection<?> a, Collection<?> b) {
  // if (a.size() != b.size()) {
  // return false;
  // }
  // for (Object x : a) {
  // if (!b.contains(x)) {
  // return false;
  // }
  // }
  // return true;
  // }

  private static boolean innerStructureMatches(QuantifiedFormula q, IFormula f) {
    IFormula g = innermost(q);
    if (!f.getKind().equals(g.getKind())) {
      return false;
    } else {
      // TODO be less conservative
      return true;
    }
    // switch (g.getKind()) {
    // case BINARY:
    // return true;
    // case CONSTANT:
    // return g.equals(f);
    // case NEGATION:
    // return true;
    // case RELATION:
    // RelationFormula r1 = (RelationFormula)g;
    // RelationFormula r2 = (RelationFormula)f;
    // return r1.getRelation().equals(r2.getRelation());
    // case QUANTIFIED:
    // default:
    // Assertions.UNREACHABLE();
  }

  public static IFormula innermost(QuantifiedFormula q) {
    IFormula g = q.getFormula();
    if (g.getKind().equals(IFormula.Kind.QUANTIFIED)) {
      return innermost((QuantifiedFormula) g);
    } else {
      return g;
    }
  }

  private static Variable makeFresh(IFormula f, IFormula g) {
    int max = 0;
    for (Variable v : f.getFreeVariables()) {
      max = Math.max(max, v.getNumber());
    }
    for (Variable v : g.getFreeVariables()) {
      max = Math.max(max, v.getNumber());
    }
    return Variable.make(max + 1, null);
  }

  // some ad-hoc formula normalization
  // 1) change >, >= to <, <=
  // TODO: do normalization in a principled manner
  public static IFormula normalize(IFormula f) {
    switch (f.getKind()) {
    case RELATION:
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.GE) || r.getRelation().equals(BinaryRelation.GT)) {
        BinaryRelation swap = BinaryRelation.swap(r.getRelation());
        return RelationFormula.make(swap, r.getTerms().get(1), r.getTerms().get(0));
      }
      return f;
    default:
      return f;
    }
  }

  /**
   * @param facts
   *            formulae that can be treated as axioms
   * @return true if we can easily prove f is a tautology
   */
  public static boolean isTautology(IFormula f, Collection<IMaxTerm> facts) {
    for (IMaxTerm d : facts) {
      if (implies(d, f)) {
        return true;
      }
    }
    switch (f.getKind()) {
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) f;
      if (b.getConnective().equals(BinaryConnective.AND)) {
        if (isTautology(b.getF1(), facts) && isTautology(b.getF2(), facts)) {
          return true;
        }
      }
      if (b.getConnective().equals(BinaryConnective.OR)) {
        if (isTautology(b.getF1(), facts) || isTautology(b.getF2(), facts)) {
          return true;
        }
      }
      break;
    case CONSTANT:
      return f.equals(BooleanConstantFormula.TRUE);
    case NEGATION:
      NotFormula n = (NotFormula) f;
      return isContradiction(n.getFormula());
    case QUANTIFIED:
      return false;
    case RELATION:
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs.getKind().equals(ITerm.Kind.CONSTANT) && rhs.getKind().equals(ITerm.Kind.CONSTANT)) {
          if (lhs.equals(rhs)) {
            return true;
          }
        }
      } else if (r.getRelation().equals(BinaryRelation.NE)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs.getKind().equals(ITerm.Kind.CONSTANT) && rhs.getKind().equals(ITerm.Kind.CONSTANT)) {
          if (!lhs.equals(rhs)) {
            return true;
          }
        }
      } else if (r.getRelation().equals(BinaryRelation.GE)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs instanceof IntConstant && rhs instanceof IntConstant) {
          IntConstant x = (IntConstant) lhs;
          IntConstant y = (IntConstant) rhs;
          return x.getVal() >= y.getVal();
        }
      } else if (r.getRelation().equals(BinaryRelation.LT)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs instanceof IntConstant && rhs instanceof IntConstant) {
          IntConstant x = (IntConstant) lhs;
          IntConstant y = (IntConstant) rhs;
          return x.getVal() < y.getVal();
        }
      }
      break;
    }
    return false;
  }

  /**
   * Is f of the form t = rhs?
   */
  private static boolean defines(IFormula f, ITerm t) {
    if (f.getKind().equals(IFormula.Kind.RELATION)) {
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
        return r.getTerms().get(0).equals(t);
      }
    }
    return false;
  }

  /**
   * does the structure of some formula f suggest an immediate substitution to
   * simplify the system, based on theory of equality?
   * 
   * @return a pair (p1, p2) meaning "substitute p2 for p1"
   */
  private static Pair<ITerm, ITerm> getNextEqualitySubstitution(Collection<IFormula> s, Collection<? extends IFormula> theory,
      Collection<IFormula> alreadyUsed) {
    Collection<IFormula> candidates = HashSetFactory.make();
    candidates.addAll(s);
    candidates.addAll(theory);
    for (IFormula f : candidates) {
      if (!alreadyUsed.contains(f)) {
        Pair<ITerm, ITerm> substitution = equalitySuggestsSubstitution(f);
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
   * simplify the system, based on theory of equality?
   * 
   * @return a pair (p1, p2) meaning "substitute p2 for p1"
   */
  private static Pair<ITerm, ITerm> equalitySuggestsSubstitution(IFormula f) {
    switch (f.getKind()) {
    case RELATION:
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
    case QUANTIFIED:
      QuantifiedFormula q = (QuantifiedFormula) f;
      if (q.getQuantifier().equals(Quantifier.FORALL)) {
        Variable bound = q.getBoundVar();
        Wildcard w = freshWildcard(q);
        IFormula g = substitute(q.getFormula(), bound, w);
        return equalitySuggestsSubstitution(g);
      } else {
        return null;
      }
    case BINARY:
    case CONSTANT:
    case NEGATION:
    default:
      // TODO
      return null;
    }
  }

  private static Wildcard freshWildcard(QuantifiedFormula q) {
    int max = 0;
    for (ITerm t : q.getAllTerms()) {
      if (t instanceof Wildcard) {
        Wildcard w = (Wildcard) t;
        max = Math.max(max, w.getNumber());
      }
    }
    return Wildcard.make(max + 1);
  }


  /**
   * in formula f, substitute the term t2 for all free occurrences of t1
   * 
   * @throws IllegalArgumentException
   *             if formula is null
   */
  public static IFormula substitute(IFormula formula, ITerm t1, ITerm t2) {
    if (formula == null) {
      throw new IllegalArgumentException("formula is null");
    }
    switch (formula.getKind()) {
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) formula;
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
        Map<Wildcard,ITerm> binding = HashMapFactory.make();
        terms.add(substitute(t, t1, t2, binding));
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
   * in term t, substitute t2 for free occurrences of t1
   */
  private static ITerm substitute(ITerm t, ITerm t1, ITerm t2, Map<Wildcard, ITerm> binding) {
    assert t != null;
    assert t1 != null;
    assert t2 != null;
    if (termsMatch(t, t1, binding)) {
      ITerm result = bindingOf(t2, binding);
      assert result != null;
      return result;
    }
    switch (t.getKind()) {
    case CONSTANT:
      return t;
    case FUNCTION:
      FunctionTerm f = (FunctionTerm) t;
      List<ITerm> terms = new LinkedList<ITerm>();
      for (ITerm p : f.getParameters()) {
        terms.add(substitute(p, t1, t2, binding));
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

  private static ITerm bindingOf(ITerm t, Map<Wildcard, ITerm> binding) {
    assert t != null;
    switch (t.getKind()) {
    case CONSTANT:
      if (t instanceof Wildcard) {
        ITerm result = binding.get(t);
        if (result == null) {
          return t;
        } else {
          return result;
        }
      } else {
        return t;
      }
    case VARIABLE:
      return t;
    case FUNCTION:
      FunctionTerm ft = (FunctionTerm) t;
      List<ITerm> terms = new ArrayList<ITerm>();
      for (ITerm p : ft.getParameters()) {
        terms.add(bindingOf(p, binding));
      }
      return FunctionTerm.make(ft.getFunction(), terms);
    default:
      Assertions.UNREACHABLE(t);
      return null;
    }
  }

  /**
   * Does the term t1 match the pattern t2? Note that this deals with wildcards.
   * Records bindings from Wildcards to Terms in the binding map ... modified as
   * a side effect.
   */
  private static boolean termsMatch(ITerm t1, ITerm t2, Map<Wildcard, ITerm> binding) {
    if (t1.equals(t2)) {
      return true;
    }
    if (t2 instanceof Wildcard) {
      Wildcard w = (Wildcard) t2;
      ITerm b = binding.get(w);
      if (b != null) {
        return b.equals(t1);
      } else {
        binding.put(w, t1);
        return true;
      }
    }

    switch (t1.getKind()) {
    case CONSTANT:
    case VARIABLE:
      return false;
    case FUNCTION:
      if (t2 instanceof FunctionTerm) {
        FunctionTerm f1 = (FunctionTerm) t1;
        FunctionTerm f2 = (FunctionTerm) t2;
        if (f1.getFunction().equals(f2.getFunction())) {
          for (int i = 0; i < f1.getParameters().size(); i++) {
            ITerm x = f1.getParameters().get(i);
            ITerm y = f2.getParameters().get(i);
            if (!termsMatch(x, y, binding)) {
              return false;
            }
          }
          return true;
        }
      }
      return false;
    default:
      Assertions.UNREACHABLE();
      return false;
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

  /**
   * Attempt to distribute the NOT from a NotFormula
   * 
   * @return the original formula if the distribution is unsuccessful
   */
  public static IFormula distributeNot(NotFormula f) {
    IFormula f1 = f.getFormula();
    if (f1 instanceof RelationFormula) {
      RelationFormula r = (RelationFormula) f1;
      BinaryRelation negate = BinaryRelation.negate(r.getRelation());
      if (negate == null) {
        return f;
      } else {
        return RelationFormula.make(negate, r.getTerms());
      }
    } else {
      return f;
    }
  }

  public static boolean isTautology(IFormula f) {
    Collection<IMaxTerm> emptyTheory = Collections.emptySet();
    return isTautology(f, emptyTheory);
  }

  public static boolean isContradiction(IFormula f) {
    Collection<IMaxTerm> emptyTheory = Collections.emptySet();
    return isContradiction(f, emptyTheory);
  }

  public static IFormula simplify(IFormula f) {
    Collection<Disjunction> emptyTheory = Collections.emptySet();
    Collection<IFormula> single = Collections.singleton(f);
    Collection<IFormula> result = propositionalSimplify(single, emptyTheory);
    assert result.size() == 1;
    return result.iterator().next();
  }

  public static IFormula propositionalSimplify(IFormula f) {
    Collection<IFormula> emptySet = Collections.emptySet();
    Collection<IFormula> singleton = Collections.singleton(f);
    Collection<IFormula> result = propositionalSimplify(singleton, emptySet);
    assert result.size() == 1;
    return result.iterator().next();
  }

}
