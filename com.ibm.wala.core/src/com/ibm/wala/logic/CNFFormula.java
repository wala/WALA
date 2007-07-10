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
import java.util.HashSet;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * A formula in conjunctive normal form.
 * 
 * @author sjfink
 */
public class CNFFormula extends AbstractBinaryFormula {

  private static final boolean DEBUG = false;

  // invariant: size >= 1
  private final Collection<Disjunction> disjunctions;

  private CNFFormula(Collection<Disjunction> clauses) {
    assert !clauses.isEmpty();
    this.disjunctions = clauses;
  }

  public Collection<? extends IConstant> getConstants() {
    Collection<IConstant> result = HashSetFactory.make();
    for (IFormula f : disjunctions) {
      result.addAll(f.getConstants());
    }
    return result;
  }
  
  public Collection<? extends ITerm> getTerms() {
    Collection<ITerm> result = HashSetFactory.make();
    for (IFormula f : disjunctions) {
      result.addAll(f.getTerms());
    }
    return result;
  }

  public Collection<Variable> getFreeVariables() {
    Collection<Variable> result = HashSetFactory.make();
    for (IFormula f : disjunctions) {
      result.addAll(f.getFreeVariables());
    }
    return result;
  }

  public boolean isAtomic() {
    return false;
  }

  public String prettyPrint(ILogicDecorator d) {
    if (disjunctions.size() == 1) {
      return getF1().prettyPrint(d);
    } else {
      StringBuffer result = new StringBuffer();
      result.append(" ( ");
      result.append(getF1().prettyPrint(d));
      result.append(" ) ");
      result.append(d.prettyPrint(getConnective()));
      result.append(" ( ");
      result.append(getF2().prettyPrint(d));
      result.append(" )");
      return result.toString();
    }
  }

  public static CNFFormula make(IFormula f) {
    if (DEBUG) {
      System.err.println("make CNF " + f);
    }
    if (f instanceof CNFFormula) {
      return (CNFFormula) f;
    } else {
      switch (f.getKind()) {
      case RELATION:
      case QUANTIFIED:
      case CONSTANT:
        Disjunction single = Disjunction.make(Collections.singleton(f));
        Collection<Disjunction> clauses = Collections.singleton(single);
        return new CNFFormula(clauses);
      case BINARY:
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
        if (f instanceof AbstractBinaryFormula) {
          AbstractBinaryFormula b = (AbstractBinaryFormula) f;
          assert (b.getConnective().equals(BinaryConnective.AND));
          return new CNFFormula(collectClauses(b));
        } else {
          return CNFFormula.make(f);
        }
      case NEGATION:
      default:
        Assertions.UNREACHABLE(f + " " + f.getKind());
        return null;
      }
    }
  }

  private static Collection<Disjunction> collectClauses(AbstractBinaryFormula b) {
    assert (b.getConnective().equals(BinaryConnective.AND));
    Collection<Disjunction> result = HashSetFactory.make();
    IFormula f1 = b.getF1();
    if (f1 instanceof AbstractBinaryFormula) {
      AbstractBinaryFormula b1 = (AbstractBinaryFormula) f1;
      if (b1.getConnective().equals(BinaryConnective.AND)) {
        result.addAll(collectClauses(b1));
      } else {
        result.add(toDisjunction(b1));
      }
    } else {
      result.add(toDisjunction(f1));
    }

    IFormula f2 = b.getF2();
    if (f2 instanceof AbstractBinaryFormula) {
      AbstractBinaryFormula b2 = (AbstractBinaryFormula) f2;
      if (b2.getConnective().equals(BinaryConnective.AND)) {
        result.addAll(collectClauses(b2));
      } else {
        result.add(toDisjunction(b2));
      }
    } else {
      result.add(toDisjunction(f2));
    }
    return result;
  }

  private static Disjunction toDisjunction(IFormula f) {
    switch (f.getKind()) {
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) f;
      if (b.getConnective().equals(BinaryConnective.OR)) {
        IFormula f1 = b.getF1();
        Disjunction d2 = toDisjunction(b.getF2());
        Collection<IFormula> c = HashSetFactory.make();
        c.add(f1);
        c.addAll(d2.getClauses());
        return Disjunction.make(c);
      } else {
        Assertions.UNREACHABLE(b);
        return null;
      }
    case CONSTANT:
    case QUANTIFIED:
    case RELATION:
      return Disjunction.make(Collections.singleton(f));
    case NEGATION:
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
  private static IFormula distribute(IFormula f) {
    switch (f.getKind()) {
    case BINARY:
      return distribute((AbstractBinaryFormula) f);
    case CONSTANT:
    case QUANTIFIED:
    case RELATION:
      return f;
    case NEGATION:
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
        return x;
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
    default:
      Assertions.UNREACHABLE(f.getKind());
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
    Collection<Disjunction> emptyTheory = Collections.emptySet();

    switch (b.getConnective()) {
    case BICONDITIONAL:
      if (Simplifier.isTautology(f1, emptyTheory)) {
        return f2;
      } else if (Simplifier.isContradiction(f1, emptyTheory)) {
        return BooleanConstantFormula.FALSE;
      } else {
        IFormula not1 = NotFormula.make(f1);
        IFormula not2 = NotFormula.make(f2);
        return BinaryFormula.or(BinaryFormula.and(f1, f2), BinaryFormula.and(not1, not2));
      }
    case AND:
    case OR:
      return BinaryFormula.make(b.getConnective(), f1, f2);
    case IMPLIES:
    default:
      Assertions.UNREACHABLE(b);
      return null;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((disjunctions == null) ? 0 : disjunctions.hashCode());
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
    if (disjunctions == null) {
      if (other.disjunctions != null)
        return false;
    } else if (!disjunctions.equals(other.disjunctions))
      return false;
    return true;
  }

  @Override
  public BinaryConnective getConnective() {
    return BinaryConnective.AND;
  }

  @Override
  public IFormula getF1() {
    return disjunctions.iterator().next();
  }

  @Override
  public IFormula getF2() {
    // if clauses.size() == 1, we fake this by saying we are getF1() AND true
    if (disjunctions.size() == 1) {
      return BooleanConstantFormula.TRUE;
    } else {
      Collection<Disjunction> c = new HashSet<Disjunction>(disjunctions);
      c.remove(getF1());
      return new CNFFormula(c);
    }
  }

  @Override
  public String toString() {
    return prettyPrint(DefaultDecorator.instance());
  }

  public Collection<? extends Disjunction> getDisjunctions() {
    return Collections.unmodifiableCollection(disjunctions);
  }

  public static CNFFormula make(Collection<Disjunction> d) {
    return new CNFFormula(d);
  }

}
