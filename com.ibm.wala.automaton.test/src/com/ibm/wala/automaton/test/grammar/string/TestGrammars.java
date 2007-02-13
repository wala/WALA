/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.automaton.test.grammar.string;

import java.util.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;

import junit.framework.TestCase;

public class TestGrammars extends TestCase {
  public void testStringValues1() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new CharSymbol("a")}),
      }
    );
    Set s = Grammars.stringValues(cfg1, new Variable("A"));
    assertEquals(null, s);
  }
  
  public void testStringValues2() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("C")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new CharSymbol("a")}),
      }
    );
    Set s = Grammars.stringValues(cfg1, new Variable("A"));
    assertEquals(AUtil.set(new String[]{"a"}), s);
  }
  
  public void testCreateUnion() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("a")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    IContextFreeGrammar cfg = Grammars.createUnion(cfg1, cfg2);
    
    assertTrue(CFLReachability.containsSome(cfg1, new ISymbol[]{new Symbol("a"), new Symbol("a")}));
    assertFalse(CFLReachability.containsSome(cfg1, new ISymbol[]{new Symbol("b"), new Symbol("b")}));
    assertFalse(CFLReachability.containsSome(cfg1, new ISymbol[]{new Symbol("a"), new Symbol("b")}));
    
    assertFalse(CFLReachability.containsSome(cfg2, new ISymbol[]{new Symbol("a"), new Symbol("a")}));
    assertTrue(CFLReachability.containsSome(cfg2, new ISymbol[]{new Symbol("b"), new Symbol("b")}));
    assertFalse(CFLReachability.containsSome(cfg2, new ISymbol[]{new Symbol("a"), new Symbol("b")}));
    
    assertTrue(CFLReachability.containsSome(cfg, new ISymbol[]{new Symbol("a"), new Symbol("a")}));
    assertTrue(CFLReachability.containsSome(cfg, new ISymbol[]{new Symbol("b"), new Symbol("b")}));
    assertFalse(CFLReachability.containsSome(cfg, new ISymbol[]{new Symbol("a"), new Symbol("b")}));
    assertFalse(CFLReachability.containsSome(cfg, new ISymbol[]{new Symbol("b"), new Symbol("a")}));
  }
  
  public void testCreateConcat() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("a")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    IContextFreeGrammar cfg = Grammars.createConcatenation(cfg1, cfg2);
    
    assertTrue(CFLReachability.containsSome(cfg1, new ISymbol[]{new Symbol("a"), new Symbol("a")}));
    assertFalse(CFLReachability.containsSome(cfg1, new ISymbol[]{new Symbol("b"), new Symbol("b")}));
    assertFalse(CFLReachability.containsSome(cfg1, new ISymbol[]{new Symbol("a"), new Symbol("b")}));
    
    assertFalse(CFLReachability.containsSome(cfg2, new ISymbol[]{new Symbol("a"), new Symbol("a")}));
    assertTrue(CFLReachability.containsSome(cfg2, new ISymbol[]{new Symbol("b"), new Symbol("b")}));
    assertFalse(CFLReachability.containsSome(cfg2, new ISymbol[]{new Symbol("a"), new Symbol("b")}));
    
    assertTrue(CFLReachability.containsSome(cfg, new ISymbol[]{new Symbol("a"), new Symbol("a")}));
    assertTrue(CFLReachability.containsSome(cfg, new ISymbol[]{new Symbol("b"), new Symbol("b")}));
    assertTrue(CFLReachability.containsSome(cfg, new ISymbol[]{new Symbol("a"), new Symbol("b")}));
    assertFalse(CFLReachability.containsSome(cfg, new ISymbol[]{new Symbol("b"), new Symbol("a")}));
  }
  
  public void testEliminateEpsilonRules(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("-")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateEpsilonRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateEpsilonRules2(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("A")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("A")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateEpsilonRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateEpsilonRules3(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("C")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("C")}),
      }
    );
    // TODO: should eliminate the dangling rules.
    /*
     ContextFreeGrammar cfg2 = new ContextFreeGrammar(
     new Variable("A"),
     new IProductionRule[]{
     new ProductionRule(new Variable("A"), new ISymbol[]{}),
     }
     );
     */
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateEpsilonRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateEpsilonRules4(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{}),
      }
    );
    // TODO: should eliminate the dangling rules.
    /*
     ContextFreeGrammar cfg2 = new ContextFreeGrammar(
     new Variable("A"),
     new IProductionRule[]{
     new ProductionRule(new Variable("A"), new ISymbol[]{}),
     }
     );
     */
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateEpsilonRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateUnitRules(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("-")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("-")}),
        
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1")}),
        
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
        
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1")}),
        
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateUnitRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateUnitRules2(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateUnitRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateUnitRules3(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("C")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{new Symbol("0")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{new Symbol("0")}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateUnitRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateUnitRules4(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("A")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("b")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("a")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateUnitRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateUnitRules5(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("A")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateUnitRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateUnitRules6(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("A")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("b")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.eliminateUnitRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testSimplifyRules(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("-")}),
        
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1")}),
        
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
      }
    );
    Grammars.simplifyRules(cfg1, null);
    
    Set strs = AUtil.set(new String[]{"N1","N2","N3","N4"});
    Set comb = AUtil.allOrder(strs);
    boolean result = false;
    for (Iterator i = comb.iterator(); i.hasNext(); ) {
      List l = (List) i.next();
      String n1 = (String) l.get(0);
      String n2 = (String) l.get(1);
      String n3 = (String) l.get(2);
      String n4 = (String) l.get(3);
      ContextFreeGrammar cfg2 = new ContextFreeGrammar(
        new Variable("A"),
        new IProductionRule[]{
          new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable(n1)}),
          new ProductionRule(new Variable(n1), new ISymbol[]{new Symbol("+"), new Variable("A")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable(n2)}),
          new ProductionRule(new Variable(n2), new ISymbol[]{new Symbol("-"), new Variable("A")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("-")}),
          
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Variable(n3)}),
          new ProductionRule(new Variable(n3), new ISymbol[]{new Variable("A"), new Symbol(")")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1")}),
          
          new ProductionRule(new Variable("A"), new ISymbol[]{}),
          
          new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable(n4)}),
          new ProductionRule(new Variable(n4), new ISymbol[]{new Variable("A"), new Symbol(")")}),
          new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
          new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
          new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
        }
      );
      if (cfg2.equals(cfg1)) {
        result = true;
        break;
      }
    }
    assertTrue(result);
  }
  
  public void testSimplifyRules2(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A"), new Variable("B")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol(";")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable("N1")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol(";")}),
        new ProductionRule(new Variable("N1"), new ISymbol[]{new Symbol("+"), new Variable("N2")}),
        new ProductionRule(new Variable("N2"), new ISymbol[]{new Variable("A"), new Variable("B")}),
      }
    );
    assertFalse(cfg1.equals(cfg2));
    Grammars.simplifyRules(cfg1, null);
    assertEquals(cfg2, cfg1);
  }
  
  public void testMoveTerminalsToUnitRules(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable("N1")}),
        new ProductionRule(new Variable("N1"), new ISymbol[]{new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable("N2")}),
        new ProductionRule(new Variable("N2"), new ISymbol[]{new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("-")}),
        
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Variable("N3")}),
        new ProductionRule(new Variable("N3"), new ISymbol[]{new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1")}),
        
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("N4")}),
        new ProductionRule(new Variable("N4"), new ISymbol[]{new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Symbol(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
      }
    );
    Grammars.moveTerminalsToUnitRules(cfg1, null);
    
    Set strs = AUtil.set(new String[]{"N5","N6","N7","N8"});
    Set comb = AUtil.allOrder(strs);
    boolean result = false;
    for (Iterator i = comb.iterator(); i.hasNext(); ) {
      List l = (List) i.next();
      String plus = (String) l.get(0);
      String minus = (String) l.get(1);
      String lparen = (String) l.get(2);
      String rparen = (String) l.get(3);
      
      ContextFreeGrammar cfg2 = new ContextFreeGrammar(
        new Variable("A"),
        new IProductionRule[]{
          new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable("N1")}),
          new ProductionRule(new Variable("N1"), new ISymbol[]{new Variable(plus), new Variable("A")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("+")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable("N2")}),
          new ProductionRule(new Variable("N2"), new ISymbol[]{new Variable(minus), new Variable("A")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("-")}),
          
          new ProductionRule(new Variable("A"), new ISymbol[]{new Variable(lparen), new Variable("N3")}),
          new ProductionRule(new Variable("N3"), new ISymbol[]{new Variable("A"), new Variable(rparen)}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Variable(lparen), new Variable(rparen)}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1")}),
          
          new ProductionRule(new Variable("A"), new ISymbol[]{}),
          
          new ProductionRule(new Variable("B"), new ISymbol[]{new Variable(lparen), new Variable("N4")}),
          new ProductionRule(new Variable("N4"), new ISymbol[]{new Variable("A"), new Variable(rparen)}),
          new ProductionRule(new Variable("B"), new ISymbol[]{new Variable(lparen), new Variable(rparen)}),
          new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
          new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
          
          new ProductionRule(new Variable(plus), new ISymbol[]{new Symbol("+")}),
          new ProductionRule(new Variable(minus), new ISymbol[]{new Symbol("-")}),
          new ProductionRule(new Variable(lparen), new ISymbol[]{new Symbol("(")}),
          new ProductionRule(new Variable(rparen), new ISymbol[]{new Symbol(")")}),
        }
      );
      if (cfg2.equals(cfg1)) {
        result = true;
        break;
      }
    }
    assertTrue(result);
  }
  
  public void testMoveTerminalsToUnitRules2(){
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable("N1"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("N1"), new ISymbol[]{new Symbol("+")}),
      }
    );
    Grammars.moveTerminalsToUnitRules(cfg1, null);
    List l1 = AUtil.sort(cfg1.getRules());
    IProductionRule r11 = (IProductionRule) l1.get(0);
    IProductionRule r12 = (IProductionRule) l1.get(1);
    IProductionRule r13 = (IProductionRule) l1.get(2);
    List l2 = AUtil.sort(cfg2.getRules());
    IProductionRule r21 = (IProductionRule) l2.get(0);
    IProductionRule r22 = (IProductionRule) l2.get(1);
    IProductionRule r23 = (IProductionRule) l2.get(2);
    assertEquals(r11, r21);
    assertEquals(r12, r22);
    assertEquals(r13, r23);
    assertEquals(r21, r11);
    assertEquals(r22, r12);
    assertEquals(r23, r13);
    assertEquals(l1, l2);
    
    assertTrue(cfg2.equals(cfg1));
    assertTrue(cfg1.equals(cfg2));
  }
  
  public void testEliminateUselessRules() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("("), new Variable("A"), new Variable(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{new Variable("D")}),
        new ProductionRule(new Variable("D"), new ISymbol[]{new Symbol("0")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("("), new Variable("A"), new Variable(")")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
      }
    );
    Grammars.eliminateUselessRules(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateDanglingVariables1() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("N")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
      }
    );
    Grammars.eliminateDanglingVariables(cfg1);
    assertEquals(cfg2, cfg1);
  }
  
  public void testEliminateDanglingVariables2() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("N")}),
      }
    );
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("N")}),
      }
    );
    Grammars.eliminateDanglingVariables(cfg1, AUtil.set(new Variable[]{new Variable("N")}));
    assertEquals(cfg2, cfg1);
  }
  
  public void testCollectLinearRules() {
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("a")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("a"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a")}),
      }
    );
    Set el = AUtil.set(new ProductionRule[]{
      new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("a")}),
    });
    Set er = AUtil.set(new ProductionRule[]{
      new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("A")}),
    });
    Set es = AUtil.set(new ProductionRule[]{
      new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("a")}),
      new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("A")}),
    });
    
    Set l = new HashSet();
    Set r = new HashSet();
    Grammars.collectLinearRules(cfg, l, r);
    assertEquals(el, l);
    assertEquals(er, r);
    
    assertEquals(el, Grammars.collectLeftLinearRules(cfg));
    assertEquals(er, Grammars.collectRightLinearRules(cfg));
    assertEquals(es, Grammars.collectLinearRules(cfg));
  }
  
  public void testCollectMutuallyRecursiveVariables() {
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Symbol("+"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Variable("C"), new Symbol("*"), new Variable("C")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{new Variable("N")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{new Variable("D")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
        new ProductionRule(new Variable("N"), new ISymbol[]{new Symbol("0"), new Variable("N")}),
        new ProductionRule(new Variable("N"), new ISymbol[]{}),
        new ProductionRule(new Variable("D"), new ISymbol[]{new Symbol("["), new Variable("N"), new Symbol("]")}),
      }
    );
    Set expected = AUtil.set(new Variable[]{new Variable("A"), new Variable("B"), new Variable("C"), new Variable("N")});
    Set mvars = Grammars.collectMutuallyRecursiveVariables(cfg);
    assertEquals(expected, mvars);
    
    Set expectedEx = AUtil.set(new Variable[]{new Variable("A"), new Variable("B"), new Variable("C")});
    Set mvarsEx =  Grammars.RegularApproximation.collectMutuallyRecursiveVariablesEx(cfg);
    assertEquals(expectedEx, mvarsEx);
  }
  
  /**
   * use the example described in the following literature.
   * M. Mohri: "Regular Approximation of Context-Free Grammars Through Transformation"
   */
  public void testRegularApproximation() {
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("E"),
      new IProductionRule[]{
        new ProductionRule(new Variable("E"), new ISymbol[]{new Variable("E"), new Symbol("+"), new Variable("T")}),
        new ProductionRule(new Variable("E"), new ISymbol[]{new Variable("T")}),
        new ProductionRule(new Variable("T"), new ISymbol[]{new Variable("T"), new Symbol("*"), new Variable("F")}),
        new ProductionRule(new Variable("T"), new ISymbol[]{new Variable("F")}),
        new ProductionRule(new Variable("F"), new ISymbol[]{new Symbol("("), new Variable("E"), new Symbol(")")}),
        new ProductionRule(new Variable("F"), new ISymbol[]{new Symbol("a")}),
      }
    );
    Grammars.RegularApproximation.approximateToRegular(cfg);
    IAutomaton expected = new Automaton(
      new State("s0"),
      new State[]{new State("s1")},
      new Transition[]{
        new Transition(new State("s0"), new State("s0"), new Symbol("(")),
        new Transition(new State("s0"), new State("s1"), new Symbol("a")),
        new Transition(new State("s1"), new State("s1"), new Symbol(")")),
        new Transition(new State("s1"), new State("s0"), new Symbol("*")),
        new Transition(new State("s1"), new State("s0"), new Symbol("+")),
      }
    );
    boolean result = CFLReachability.containsAll(expected, cfg);
    assertTrue(result);
  }
  
  public void testToCFG1() {
    IAutomaton fst = new Automaton(
      new State("s0"),
      new State[]{new State("s1")},
      new Transition[]{
        new Transition(new State("s0"), new State("s0"), new Symbol("(")),
        new Transition(new State("s0"), new State("s1"), new Symbol("a")),
        new Transition(new State("s1"), new State("s1"), new Symbol(")")),
        new Transition(new State("s1"), new State("s0"), new Symbol("*")),
        new Transition(new State("s1"), new State("s0"), new Symbol("+")),
      }
    );
    IContextFreeGrammar cfg = Grammars.toCFG(fst);
    
    boolean result = false;
    
    //System.out.println(Automatons.toGraphviz(fst));
    result = CFLReachability.containsSome(cfg, fst);
    assertTrue(result);
    
    //System.out.println(Automatons.toGraphviz(fst));
    result = CFLReachability.containsAll(fst, cfg);
    assertTrue(result);
    
    //System.out.println(Automatons.toGraphviz(fst));
    result = CFLReachability.containsSome(cfg, fst);
    assertTrue(result);
  }
}
