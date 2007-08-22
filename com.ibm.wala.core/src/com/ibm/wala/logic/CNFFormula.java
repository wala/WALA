/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
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

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * A formula in conjunctive normal form.
 * 
 * TODO: rename and refactor
 * 
 * @author sjfink
 */
public class CNFFormula extends AbstractBinaryFormula implements ICNFFormula {

  private static final boolean DEBUG = false;

  // invariant: size >= 2
  final Collection<? extends IMaxTerm> maxTerms;

  private CNFFormula(Collection<? extends IMaxTerm> maxTerms) {
    assert maxTerms.size() >= 2;
    this.maxTerms = maxTerms;
  }

  private CNFFormula(IMaxTerm single) {
    this.maxTerms = Collections.singleton(single);
  }

  public Collection<? extends IConstant> getConstants() {
    Collection<IConstant> result = HashSetFactory.make();
    for (IFormula f : maxTerms) {
      result.addAll(f.getConstants());
    }
    return result;
  }

  public Collection<? extends ITerm> getAllTerms() {
    Collection<ITerm> result = HashSetFactory.make();
    for (IFormula f : maxTerms) {
      result.addAll(f.getAllTerms());
    }
    return result;
  }

  public Collection<AbstractVariable> getFreeVariables() {
    Collection<AbstractVariable> result = HashSetFactory.make();
    for (IFormula f : maxTerms) {
      result.addAll(f.getFreeVariables());
    }
    return result;
  }

  public String prettyPrint(ILogicDecorator d) throws IllegalArgumentException {
    if (d == null) {
      throw new IllegalArgumentException("d == null");
    }
    return d.prettyPrint(this);
  }

  public static ICNFFormula make(IFormula f) throws IllegalArgumentException {
    if (f == null) {
      throw new IllegalArgumentException("f == null");
    }
    if (DEBUG) {
      System.err.println("make CNF " + f);
    }
    if (f instanceof CNFFormula) {
      return (ICNFFormula) f;
    } else {
      switch (f.getKind()) {
      case RELATION:
      case QUANTIFIED:
      case CONSTANT:
        return (IMaxTerm) f;
      case BINARY:
      case NEGATION: {
        f = trivialCleanup(f);
        if (DEBUG) {
          System.err.println("after trivial cleanup " + f);
        }

        f = eliminateArrows(f);
        if (DEBUG) {
          System.err.println("after eliminate arrows " + f);
        }

        f = pushNegations(f);
        if (DEBUG) {
          System.err.println("after pushNegations " + f);
        }

        f = distribute(f);
        if (DEBUG) {
          System.err.println("after distribute " + f);
        }

        if (f instanceof AbstractBinaryFormula || f instanceof NotFormula) {
          Collection<IMaxTerm> c = collectMaxTerms(f);
          c.remove(BooleanConstantFormula.TRUE);
          return CNFFormula.make(c);
        } else {
          return CNFFormula.make(f);
        }
      }
      default:
        Assertions.UNREACHABLE(f + " " + f.getKind());
        return null;
      }
    }
  }

  private static Collection<IMaxTerm> collectMaxTerms(IFormula f) {
    switch (f.getKind()) {
    case CONSTANT:
    case QUANTIFIED:
    case RELATION:
      return Collections.singleton((IMaxTerm) f);
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) f;
      if (b.getConnective().equals(BinaryConnective.AND)) {
        Collection<IMaxTerm> result = HashSetFactory.make();
        result.addAll(collectMaxTerms(b.getF1()));
        result.addAll(collectMaxTerms(b.getF2()));
        return result;
      } else if (b.getConnective().equals(BinaryConnective.OR)) {
        return Collections.singleton(orToMaxTerm(b));
      } else {
        Assertions.UNREACHABLE();
        return null;
      }
    case NEGATION:
      NotFormula n = (NotFormula) f;
      IMaxTerm t = NotFormulaMaxTerm.make(n.getFormula());
      return Collections.singleton(t);
    default:
      Assertions.UNREACHABLE(f);
      return null;
    }
  }

  private static IMaxTerm orToMaxTerm(AbstractBinaryFormula b) {
    assert b.getConnective().equals(BinaryConnective.OR);
    Collection<IMaxTerm> clauses = HashSetFactory.make();
    clauses.addAll(collectMaxTerms(b.getF1()));
    clauses.addAll(collectMaxTerms(b.getF2()));
    if (clauses.size() == 1) {
      return clauses.iterator().next();
    } else {
      return Disjunction.make(clauses);
    }
  }

  private static IFormula trivialCleanup(IFormula f) {
    if (AdHocSemiDecisionProcedure.singleton().isTautology(f)) {
      return BooleanConstantFormula.TRUE;
    } else if (AdHocSemiDecisionProcedure.singleton().isContradiction(f)) {
      return BooleanConstantFormula.FALSE;
    } else {
      switch (f.getKind()) {
      case BINARY:
        AbstractBinaryFormula b = (AbstractBinaryFormula) f;
        return BinaryFormula.make(b.getConnective(), trivialCleanup(b.getF1()), trivialCleanup(b.getF2()));
      case RELATION:
      case CONSTANT:
      case NEGATION:
      case QUANTIFIED:
      default:
        return f;
      }
    }
  }

  /**
   * move all conjunctions outside disjunctions using distributive laws.
   * 
   * (a AND b) OR c -> (a OR c) AND (b OR c)
   * 
   * a OR (b AND c) -> (a OR b) AND (a OR c)
   */
  private static IFormula distribute(IFormula f) {
    switch (f.getKind()) {
    case BINARY:
      return distribute((AbstractBinaryFormula) f);
    case CONSTANT:
    case QUANTIFIED:
    case RELATION:
    case NEGATION:
      return f;
    default:
      Assertions.UNREACHABLE(f.getKind());
      return null;
    }
  }

  /**
   * move all conjunctions outside disjunctions using distributive laws.
   * 
   * (a AND b) OR c -> (a OR c) AND (b OR c)
   * 
   * a OR (b AND c) -> (a OR b) AND (a OR c)
   */
  private static IFormula distribute(AbstractBinaryFormula b) {
    IFormula f1 = b.getF1();
    IFormula f2 = b.getF2();
    f1 = distribute(f1);
    f2 = distribute(f2);
    switch (b.getConnective()) {
    case OR:
      if (f1 instanceof AbstractBinaryFormula && ((AbstractBinaryFormula) f1).getConnective().equals(BinaryConnective.AND)) {
        AbstractBinaryFormula c1 = (AbstractBinaryFormula) f1;
        IFormula af = c1.getF1();
        IFormula bf = c1.getF2();
        IFormula x = BinaryFormula.and(BinaryFormula.or(af, f2), BinaryFormula.or(bf, f2));
        // distribute again; case 2 may apply
        return distribute(x);
      }
      if (f2 instanceof AbstractBinaryFormula && ((AbstractBinaryFormula) f2).getConnective().equals(BinaryConnective.AND)) {
        AbstractBinaryFormula c2 = (AbstractBinaryFormula) f2;
        IFormula bf = c2.getF1();
        IFormula cf = c2.getF2();
        IFormula x = BinaryFormula.and(BinaryFormula.or(f1, bf), BinaryFormula.or(f1, cf));
        // distribute again; case 1 may apply now
        return distribute(x);
      }
      return BinaryFormula.make(b.getConnective(), f1, f2);
    case AND:
      return BinaryFormula.make(b.getConnective(), f1, f2);
    case BICONDITIONAL:
    case IMPLIES:
    default:
      Assertions.UNREACHABLE(b.getConnective());
      return null;
    }
  }

  /**
   * push NOT operators into the formula using double complement and De Morgan's
   * laws.
   */
  private static IFormula pushNegations(IFormula f) {
    switch (f.getKind()) {
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) f;
      IFormula f1 = b.getF1();
      IFormula f2 = b.getF2();
      f1 = pushNegations(f1);
      f2 = pushNegations(f2);
      return BinaryFormula.make(b.getConnective(), f1, f2);
    case CONSTANT:
    case RELATION:
    case QUANTIFIED:
      return f;
    case NEGATION:
      NotFormula n = (NotFormula) f;
      return Simplifier.distributeNot(n);
    default:
      return null;
    }
  }

  /**
   * eliminate all IMPLIES and BICONDITIONALS
   */
  private static IFormula eliminateArrows(IFormula f) {
    switch (f.getKind()) {
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) f;
      return eliminateArrows(b);
    case CONSTANT:
    case RELATION:
    case QUANTIFIED:
      return f;
    case NEGATION:
      NotFormula n = (NotFormula) f;
      return NotFormula.make(eliminateArrows(n.getFormula()));
    default:
      Assertions.UNREACHABLE(f.getKind());
      return null;
    }
  }

  /**
   * eliminate all IMPLIES and BICONDITIONALS
   */
  private static IFormula eliminateArrows(AbstractBinaryFormula b) {
    IFormula f1 = b.getF1();
    IFormula f2 = b.getF2();
    f1 = eliminateArrows(f1);
    f2 = eliminateArrows(f2);

    switch (b.getConnective()) {
    case BICONDITIONAL:
      if (AdHocSemiDecisionProcedure.singleton().isTautology(f1)) {
        return f2;
      } else if (AdHocSemiDecisionProcedure.singleton().isContradiction(f1)) {
        return BooleanConstantFormula.TRUE;
      } else {
        IFormula not1 = NotFormula.make(f1);
        IFormula not2 = NotFormula.make(f2);
        return BinaryFormula.or(BinaryFormula.and(f1, f2), BinaryFormula.and(not1, not2));
      }
    case AND:
    case OR:
      return BinaryFormula.make(b.getConnective(), f1, f2);
    case IMPLIES:
      return BinaryFormula.or(NotFormula.make(f1), f2);
    default:
      Assertions.UNREACHABLE(b);
      return null;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((maxTerms == null) ? 0 : maxTerms.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final CNFFormula other = (CNFFormula) obj;
    if (maxTerms == null) {
      if (other.maxTerms != null)
        return false;
    } else if (!maxTerms.equals(other.maxTerms))
      return false;
    return true;
  }

  @Override
  public BinaryConnective getConnective() {
    return BinaryConnective.AND;
  }

  @Override
  public IFormula getF1() {
    return maxTerms.iterator().next();
  }

  @Override
  public IFormula getF2() {
    Collection<? extends IMaxTerm> c = HashSetFactory.make(maxTerms);
    c.remove(getF1());
    return CNFFormula.make(c);
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("CNF\n");
    int i = 1;
    for (IMaxTerm t : getMaxTerms()) {
      result.append(" (" + i + ") " + t.prettyPrint(DefaultDecorator.instance()) + "\n");
      i++;
    }
    return result.toString();
  }

  public Collection<IMaxTerm> getMaxTerms() {
    return Collections.unmodifiableCollection(maxTerms);
  }

  public static ICNFFormula make(Collection<? extends IMaxTerm> d) {
    Collection<IMaxTerm> c = HashSetFactory.make();
    for (IMaxTerm x : d) {
      c.add(normalize(x));
    }
    c.remove(BooleanConstantFormula.TRUE);
    if (c.size() == 0) {
      return BooleanConstantFormula.TRUE;
    } else if (c.size() == 1) {
      return c.iterator().next();
    } else {
      return new CNFFormula(c);
    }
  }
  
  // TODO: move this to Simplifier?
  public static IMaxTerm normalize(IMaxTerm f) throws IllegalArgumentException {
    if (f == null) {
      throw new IllegalArgumentException("f == null");
    }
    switch (f.getKind()) {
    case RELATION:
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.GE )|| r.getRelation().equals(BinaryRelation.GT)) {
        BinaryRelation swap = BinaryRelation.swap(r.getRelation());
        return RelationFormula.make(swap, r.getTerms().get(1), r.getTerms().get(0));
      }
      return f;
    default:
      return f;
    }
  }

  
  
}
