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

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;

/**
 * Ad-hoc decision logic.
 * 
 * @author sjfink
 */
public class AdHocSemiDecisionProcedure extends AbstractSemiDecisionProcedure {

  private final static AdHocSemiDecisionProcedure INSTANCE = new AdHocSemiDecisionProcedure();

  public static AdHocSemiDecisionProcedure singleton() {
    return INSTANCE;
  }

  protected AdHocSemiDecisionProcedure() {
  }

  /*
   * @see com.ibm.wala.logic.ISemiDecisionProcedure#isContradiction(com.ibm.wala.logic.IFormula,
   *      java.util.Collection)
   */
  public boolean isContradiction(IFormula f, Collection<IMaxTerm> facts) throws IllegalArgumentException {
    return contradiction(f, facts);
  }

  /*
   * @see com.ibm.wala.logic.ISemiDecisionProcedure#isTautology(com.ibm.wala.logic.IFormula,
   *      java.util.Collection)
   */
  public boolean isTautology(IFormula f, Collection<IMaxTerm> facts) throws IllegalArgumentException {
    return tautology(f, facts);
  }

  // some ad-hoc formula normalization
  // 1) change >, >= to <, <=
  // TODO: do normalization in a principled manner
  static IFormula normalize(IFormula f) throws IllegalArgumentException {
    if (f == null) {
      throw new IllegalArgumentException("f == null");
    }
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
   * TODO .. fix this. Does axiom imply f?
   */
  private static boolean implies(IFormula axiom, IFormula f) {
    if (AdHocSemiDecisionProcedure.normalize(axiom).equals(AdHocSemiDecisionProcedure.normalize(f))) {
      return true;
    }
    // This is way too slow. give up on it.
    // if (axiom.getKind().equals(IFormula.Kind.QUANTIFIED)) {
    // QuantifiedFormula q = (QuantifiedFormula) axiom;
    //  
    // if (!Simplifier.innerStructureMatches(q, f)) {
    // return false;
    // }
    //  
    // if (q.getQuantifier().equals(Quantifier.FORALL)) {
    // AbstractVariable bound = q.getBoundVar();
    // IFormula body = q.getFormula();
    // // this could be inefficient. find a better algorithm.
    // for (ITerm t : f.getAllTerms()) {
    // if (q.getFreeVariables().contains(t)) {
    // AbstractVariable fresh = Simplifier.makeFreshIntVariable(q, f);
    // IFormula testBody = Simplifier.substitute(body, bound, fresh);
    // IFormula testF = Simplifier.substitute(f, t, fresh);
    // if (implies(testBody, testF)) {
    // return true;
    // }
    // } else {
    // IFormula testBody = Simplifier.substitute(body, bound, t);
    // if (implies(testBody, f)) {
    // return true;
    // }
    // }
    // }
    // }
    // }
    return false;
  }

  private static boolean contradicts(IMaxTerm axiom, IFormula f) {
    IFormula notF = NotFormula.make(f);
    return AdHocSemiDecisionProcedure.implies(axiom, notF);
  }

  /**
   * @param facts
   *            formulae that can be treated as axioms
   * @return true if we can easily prove f is a contradiction
   * @throws IllegalArgumentException
   *             if facts == null
   */
  private static boolean contradiction(IFormula f, Collection<IMaxTerm> facts) throws IllegalArgumentException {
    if (facts == null) {
      throw new IllegalArgumentException("facts == null");
    }
    for (IMaxTerm d : facts) {
      if (AdHocSemiDecisionProcedure.contradicts(d, f)) {
        return true;
      }
    }
    switch (f.getKind()) {
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) f;
      if (b.getConnective().equals(BinaryConnective.AND)) {
        if (contradiction(b.getF1(), facts) || contradiction(b.getF2(), facts)) {
          return true;
        }
        IFormula not1 = NotFormula.make(b.getF1());
        if (AdHocSemiDecisionProcedure.implies(b.getF2(), not1)) {
          return true;
        }
        IFormula not2 = NotFormula.make(b.getF2());
        if (AdHocSemiDecisionProcedure.implies(b.getF1(), not2)) {
          return true;
        }
      } else if (b.getConnective().equals(BinaryConnective.OR)) {
        if (contradiction(b.getF1(), facts) && contradiction(b.getF2(), facts)) {
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
      } else if (r.getRelation().equals(BinaryRelation.LT)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs.getKind().equals(ITerm.Kind.CONSTANT) && rhs.getKind().equals(ITerm.Kind.CONSTANT)) {
          if (lhs instanceof IntConstant && rhs instanceof IntConstant) {
            IntConstant c1 = (IntConstant) lhs;
            IntConstant c2 = (IntConstant) rhs;
            return c1.getVal() >= c2.getVal();
          }
        }
      } else if (r.getRelation().equals(BinaryRelation.LE)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs.getKind().equals(ITerm.Kind.CONSTANT) && rhs.getKind().equals(ITerm.Kind.CONSTANT)) {
          if (lhs instanceof IntConstant && rhs instanceof IntConstant) {
            IntConstant c1 = (IntConstant) lhs;
            IntConstant c2 = (IntConstant) rhs;
            return c1.getVal() > c2.getVal();
          }
        }
      }
      break;
    }
    return false;
  }

  /**
   * @param facts
   *            formulae that can be treated as axioms
   * @return true if we can easily prove f is a tautology
   * @throws IllegalArgumentException
   *             if facts == null
   */
  private static boolean tautology(IFormula f, Collection<IMaxTerm> facts) throws IllegalArgumentException {
    if (facts == null) {
      throw new IllegalArgumentException("facts == null");
    }
    for (IMaxTerm d : facts) {
      if (AdHocSemiDecisionProcedure.implies(d, f)) {
        return true;
      }
    }
    switch (f.getKind()) {
    case BINARY:
      AbstractBinaryFormula b = (AbstractBinaryFormula) f;
      if (b.getConnective().equals(BinaryConnective.AND)) {
        if (tautology(b.getF1(), facts) && tautology(b.getF2(), facts)) {
          return true;
        }
      }
      if (b.getConnective().equals(BinaryConnective.OR)) {
        if (tautology(b.getF1(), facts) || tautology(b.getF2(), facts)) {
          return true;
        } else if (b.getF1().equals(NotFormula.make(b.getF2()))) {
          return true;
        }
      }
      break;
    case CONSTANT:
      return f.equals(BooleanConstantFormula.TRUE);
    case NEGATION:
      NotFormula n = (NotFormula) f;
      return AdHocSemiDecisionProcedure.singleton().isContradiction(n.getFormula());
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
      } else if (r.getRelation().equals(BinaryRelation.LE)) {
        ITerm lhs = r.getTerms().get(0);
        ITerm rhs = r.getTerms().get(1);
        if (lhs instanceof IntConstant && rhs instanceof IntConstant) {
          IntConstant x = (IntConstant) lhs;
          IntConstant y = (IntConstant) rhs;
          return x.getVal() <= y.getVal();
        }
      }
      break;
    }
    return false;
  }
}
