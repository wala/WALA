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

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

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
    Assertions.UNREACHABLE("implement me");
    return null;
//    if (s.getKind().equals(IFormula.Kind.QUANTIFIED)) {
//      Collection<IFormula> result = HashSetFactory.make();
//      QuantifiedFormula f = (QuantifiedFormula) s;
//      assert f.getBoundVar() instanceof ConstrainedIntVariable;
//      ConstrainedIntVariable v = (ConstrainedIntVariable) f.getBoundVar();
//      IntPair range = v.getRange();
//      assert range.getX() >= 0;
//      assert range.getY() >= range.getX();
//      for (int i = range.getX(); i <= range.getY(); i++) {
//        result.add(substitute(f.getFormula(), v, IntConstant.make(i)));
//      }
//      return result;
//    } else {
//      return Collections.singleton(s);
//    }
  }

  /**
   * Eliminate quantifiers, by substituting every possible constant value for a
   * quantified variable
   */
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
  public static Collection<IFormula> simplify(Collection<IFormula> s, Collection<? extends IFormula> theory,
      ISemiDecisionProcedure dec) {
    // the following is ad hoc and bogus.
//    boolean changed = true;
//    while (changed) {
//      changed = false;
//      Collection<IFormula> alreadyUsed = HashSetFactory.make();
//      Pair<ITerm, ITerm> substitution = getNextEqualitySubstitution(s, theory, alreadyUsed);
//      while (substitution != null) {
//        Collection<IFormula> temp = HashSetFactory.make();
//        for (IFormula f : s) {
//          if (!defines(f, substitution.fst)) {
//            IFormula f2 = substitute(f, substitution.fst, substitution.snd);
//            temp.add(f2);
//            if (!f.equals(f2)) {
//              changed = true;
//            }
//          } else {
//            temp.add(f);
//          }
//        }
//        s = temp;
//        substitution = getNextEqualitySubstitution(s, theory, alreadyUsed);
//      }
//    }
    return propositionalSimplify(s, theory, dec);
  }

  /**
   * Simplify the set s based on simple propositional logic.
   */
  public static Collection<IFormula> propositionalSimplify(Collection<IFormula> s, Collection<? extends IFormula> t,
      ISemiDecisionProcedure dec) {
    debug1(s, t);
    Collection<ICNFFormula> cs = toCNF(s);
    Collection<ICNFFormula> ct = toCNF(t);
    debug2(cs, ct);
    Collection<IMaxTerm> facts = collectClauses(ct);

    Collection<IFormula> result = HashSetFactory.make();
    for (ICNFFormula f : cs) {
      Collection<? extends IMaxTerm> d = simplifyCNF(f, facts, dec);
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
  /**
   * Simplify the set s based on simple propositional logic.
   */
  public static IFormula propositionalSimplify(IFormula f, Collection<? extends IFormula> t,
      ISemiDecisionProcedure dec) {
    Collection<IFormula> result = propositionalSimplify(Collections.singleton(f), t, dec);
    return result.iterator().next();
  }

  /**
   * Assuming a set of facts holds, simplify a CNF formula
   */
  private static Collection<? extends IMaxTerm> simplifyCNF(ICNFFormula f, Collection<IMaxTerm> facts, ISemiDecisionProcedure dec) {
    Collection<IMaxTerm> result = HashSetFactory.make();
    Collection<IMaxTerm> removedClauses = HashSetFactory.make();
    // for each clause in f ....
    for (IMaxTerm d : collectClauses(Collections.singleton(f))) {
      // otherFacts := facts U live clauses of f - d
      Collection<IMaxTerm> otherFacts = HashSetFactory.make(facts);
      otherFacts.addAll(collectClauses(Collections.singleton(f)));
      otherFacts.remove(d);
      otherFacts.removeAll(removedClauses);

      IMaxTerm checkD = d;
      if (d instanceof Disjunction) {
        checkD = simplifyDisjunction((Disjunction) d, otherFacts, dec);
      }

      if (dec.isContradiction(checkD, otherFacts)) {
        return Collections.singleton(BooleanConstantFormula.FALSE);
      } else if (facts.contains(checkD) || dec.isTautology(checkD, otherFacts)) {
        removedClauses.add(d);
      } else {
        result.add(checkD);
      }
    }
    if (result.isEmpty()) {
      return Collections.singleton(BooleanConstantFormula.TRUE);
    }
    return result;
  }

  private static IMaxTerm simplifyDisjunction(Disjunction d, Collection<IMaxTerm> otherFacts, ISemiDecisionProcedure dec) {
    Collection<IFormula> result = HashSetFactory.make();
    for (IFormula f : d.getClauses()) {
      if (dec.isContradiction(f, otherFacts)) {
        result.add(BooleanConstantFormula.FALSE);
      } else if (dec.isTautology(f, otherFacts)) {
        result.add(BooleanConstantFormula.TRUE);
      } else {
        result.add(f);
      }
    }
    if (result.size() == 1) {
      IFormula f = result.iterator().next();
      assert f instanceof IMaxTerm;
      return (IMaxTerm)f;
    } else {
      return Disjunction.make(result);
    }
  }

  /**
   * Collect all {@link IMaxTerm}s that appear in the formulae in s
   */
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

  static boolean innerStructureMatches(QuantifiedFormula q, IFormula f) {
    IFormula g = innermost(q);
    if (!f.getKind().equals(g.getKind())) {
      return false;
    } else {
      // TODO be less conservative
      return true;
    }
  }

  public static IFormula innermost(QuantifiedFormula q) throws IllegalArgumentException {
    if (q == null) {
      throw new IllegalArgumentException("q == null");
    }
    IFormula g = q.getFormula();
    if (g.getKind().equals(IFormula.Kind.QUANTIFIED)) {
      return innermost((QuantifiedFormula) g);
    } else {
      return g;
    }
  }

//  static AbstractNumberedVariable makeFreshIntVariable(IFormula f, IFormula g) {
//    int max = 0;
//    for (AbstractVariable v : f.getFreeVariables()) {
//      max = Math.max(max, v.getNumber());
//    }
//    for (AbstractVariable v : g.getFreeVariables()) {
//      max = Math.max(max, v.getNumber());
//    }
//    return IntVariable.make(max + 1);
//  }

//  /**
//   * Is f of the form t = rhs?
//   */
//  private static boolean defines(IFormula f, ITerm t) {
//    if (f.getKind().equals(IFormula.Kind.RELATION)) {
//      RelationFormula r = (RelationFormula) f;
//      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
//        return r.getTerms().get(0).equals(t);
//      }
//    }
//    return false;
//  }
//
//  /**
//   * does the structure of some formula f suggest an immediate substitution to
//   * simplify the system, based on theory of equality?
//   * 
//   * @return a pair (p1, p2) meaning "substitute p2 for p1"
//   */
//  private static Pair<ITerm, ITerm> getNextEqualitySubstitution(Collection<IFormula> s, Collection<? extends IFormula> theory,
//      Collection<IFormula> alreadyUsed) {
//    Collection<IFormula> candidates = HashSetFactory.make();
//    candidates.addAll(s);
//    candidates.addAll(theory);
//    for (IFormula f : candidates) {
//      if (!alreadyUsed.contains(f)) {
//        Pair<ITerm, ITerm> substitution = equalitySuggestsSubstitution(f);
//        if (substitution != null) {
//          alreadyUsed.add(f);
//          return substitution;
//        }
//      }
//    }
//    return null;
//  }

//  /**
//   * does the structure of formula f suggest an immediate substitution to
//   * simplify the system, based on theory of equality?
//   * 
//   * @return a pair (p1, p2) meaning "substitute p2 for p1"
//   */
//  private static Pair<ITerm, ITerm> equalitySuggestsSubstitution(IFormula f) {
//    switch (f.getKind()) {
//    case RELATION:
//      // it's not clear at this level that a constant or variable is "simpler" than
//      // e.g. a function term.  so, don't do anything.
//      return null;
////      RelationFormula r = (RelationFormula) f;
////      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
////        ITerm lhs = r.getTerms().get(0);
////        ITerm rhs = r.getTerms().get(1);
////        if (rhs.getKind().equals(ITerm.Kind.CONSTANT) || rhs.getKind().equals(ITerm.Kind.VARIABLE)) {
////          return Pair.make(lhs, rhs);
////        } else {
////          return null;
////        }
////      } else {
////        return null;
////      }
//    case QUANTIFIED:
//      QuantifiedFormula q = (QuantifiedFormula) f;
//      if (q.getQuantifier().equals(Quantifier.FORALL)) {
//        AbstractVariable bound = q.getBoundVar();
//        Wildcard w = freshWildcard(q);
//        IFormula g = substitute(q.getFormula(), bound, w);
//        return equalitySuggestsSubstitution(g);
//      } else {
//        return null;
//      }
//    case BINARY:
//    case CONSTANT:
//    case NEGATION:
//    default:
//      // TODO
//      return null;
//    }
//  }

//  private static Wildcard freshWildcard(QuantifiedFormula q) {
//    int max = 0;
//    for (ITerm t : q.getAllTerms()) {
//      if (t instanceof Wildcard) {
//        Wildcard w = (Wildcard) t;
//        max = Math.max(max, w.getNumber());
//      }
//    }
//    return Wildcard.make(max + 1);
//  }

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
        Map<Wildcard, ITerm> binding = HashMapFactory.make();
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

  public static Collection<AbstractVariable> getFreeVariables(Collection<? extends IFormula> constraints) {
    if (constraints == null) {
      throw new IllegalArgumentException("constraints is null");
    }
    Collection<AbstractVariable> free = HashSetFactory.make();
    for (IFormula f : constraints) {
      free.addAll(f.getFreeVariables());
    }
    return free;
  }

  /**
   * Attempt to distribute the NOT from a NotFormula
   * 
   * @return the original formula if the distribution is unsuccessful
   * @throws IllegalArgumentException
   *             if f == null
   */
  public static IFormula distributeNot(NotFormula f) throws IllegalArgumentException {
    if (f == null) {
      throw new IllegalArgumentException("f == null");
    }
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

  public static IFormula simplify(IFormula f, ISemiDecisionProcedure dec) {
    Collection<Disjunction> emptyTheory = Collections.emptySet();
    Collection<IFormula> single = Collections.singleton(f);
    Collection<IFormula> result = propositionalSimplify(single, emptyTheory, dec);
    assert result.size() == 1;
    return result.iterator().next();
  }

  public static IFormula propositionalSimplify(IFormula f, ISemiDecisionProcedure dec) {
    Collection<IFormula> emptySet = Collections.emptySet();
    Collection<IFormula> singleton = Collections.singleton(f);
    Collection<IFormula> result = propositionalSimplify(singleton, emptySet, dec);
    assert result.size() == 1;
    return result.iterator().next();
  }

  public static IFormula simplify(IFormula f, IFormula t, ISemiDecisionProcedure dec) {
    Collection<IFormula> s = simplify(Collections.singleton(f), Collections.singleton(t), dec);
    assert s.size() == 1;
    return s.iterator().next();
  }

}
