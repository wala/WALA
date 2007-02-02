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

import java.util.Set;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;

import junit.framework.TestCase;

public class TestCFLReachability extends TestCase {

  public void testReachabilitySimple(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
      }
    ); 
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),

        new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")}),
        new CFLReachability.AnalysisTransition(new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
      }
    );
    IAutomaton result = CFLReachability.analyze(fst, cfg, new CFLReachability.SimpleTransitionFactory());
    assertEquals(fst2, result);
    assertTrue(CFLReachability.isReachable(fst,cfg));
  }

  public void testReachabilitySimple2(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
      }
    ); 
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),

        new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")}),
        new CFLReachability.AnalysisTransition(new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
        new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("0"), new Variable("B")}),
      }
    ); 
    assertEquals(fst2, CFLReachability.analyze(fst, cfg, new CFLReachability.SimpleTransitionFactory()));
  }

  public void testReachabilityWithAnalysisTransition(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
      }
    );
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),

        new CFLReachability.AnalysisTransition(
          new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")}),
          new CFLReachability.AnalysisTransition(
            new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
      }
    );
    IAutomaton result = CFLReachability.analyze(fst, cfg, new CFLReachability.SimpleTransitionFactory());
    assertEquals(fst2, result);
    assertTrue(CFLReachability.isReachable(fst,cfg));
  }

  public void testReachabilityWithProductionRule(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
      }
    );
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),

        new CFLReachability.ProductionRuleTransition(
          new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")},
          new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")})),
          new CFLReachability.ProductionRuleTransition(
            new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Symbol("2")},
            new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")})),
      }
    );
    IAutomaton result = CFLReachability.analyze(fst, cfg, new CFLReachability.ProductionRuleTransitionFactory());
    assertEquals(fst2, result);
    assertTrue(CFLReachability.isReachable(fst,cfg));
  }

  public void testReachabilityWithVariable(){
    FilteredTransition.ICondition cond = new FilteredTransition.ICondition(){
      public boolean accept(ISymbol symbol, IMatchContext ctx) {
        if (symbol instanceof IVariable) {
          return false;
        }
        if (symbol.getName().charAt(0) == (char)'a') {
          return true;
        }
        return false;
      }
    };
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new FilteredTransition(new State("s1"), new State("s2"), new Variable("A"), new ISymbol[]{}, null, cond),
        new Transition(new State("s2"), new State("s3"), new Symbol("b")),
      }
    );
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new FilteredTransition(new State("s1"), new State("s2"), new Variable("A"), new ISymbol[]{}, null, cond),
        new Transition(new State("s2"), new State("s3"), new Symbol("b")),

        new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("A"), new ISymbol[]{}),
      }
    );
    IAutomaton result = CFLReachability.analyze(fst, cfg, new CFLReachability.SimpleTransitionFactory());
    assertEquals(fst2, result);
  }

  public void testReachabilityWithFilteredTransition1() {
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s1")},
      new Transition[]{
        new FilteredTransition(new State("s1"), new State("s1"), new Variable("x"), new FilteredTransition.ICondition(){
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            if (symbol.getName().equals("z")) {
              return false;
            }
            else {
              return true;
            }
          }
        }),
      });
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new ProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Symbol("a")}),
      });
    ContextFreeGrammar cfg2 = new ContextFreeGrammar(
      new Variable("A"),
      new ProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Symbol("z"), new Symbol("a")}),
      });
    assertTrue(CFLReachability.containsAll(fst, cfg1));
    assertFalse(CFLReachability.containsAll(fst, cfg2));
    assertTrue(CFLReachability.containsSome(cfg1, fst));
    assertFalse(CFLReachability.containsSome(cfg2, fst));
  }

  public void testReachability2(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
      }
    ); 
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),

        new CFLReachability.ProductionRuleTransition(
          new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Symbol("2")},
          new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")})),
          new CFLReachability.ProductionRuleTransition(
            new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Symbol("0"), new Variable("B")},
            new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")})),
            new CFLReachability.ProductionRuleTransition(
              new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Symbol("0"), new Symbol("2")},
              new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")})),
              new CFLReachability.ProductionRuleTransition(
                new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")},
                new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")})),
                new CFLReachability.ProductionRuleTransition(
                  new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("0"), new Symbol("2")},
                  new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")})),
                  new CFLReachability.ProductionRuleTransition(
                    new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("0"), new Variable("B")},
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")})),
      }
    ); 
    IAutomaton result = CFLReachability.analyze(fst, cfg, new CFLReachability.ProductionRuleTransitionFactory());
    assertEquals(fst2, result);
  }


  public void testReachabilityRecursion(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s3"), new State("s1"), new Symbol("c"), new Symbol[]{new Symbol("3")}),
        new Transition(new State("s3"), new State("s3"), new Symbol("c"), new Symbol[]{new Symbol("3")}),
      }
    ); 
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b"), new Variable("C")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{new Symbol("c"), new Variable("A")}),
        new ProductionRule(new Variable("C"), new ISymbol[]{new Symbol("c")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s3"), new State("s1"), new Symbol("c"), new Symbol[]{new Symbol("3")}),
        new Transition(new State("s3"), new State("s3"), new Symbol("c"), new Symbol[]{new Symbol("3")}),

        new CFLReachability.AnalysisTransition(new State("s1"), new State("s1"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
        new CFLReachability.AnalysisTransition(new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),

        new CFLReachability.AnalysisTransition(new State("s2"), new State("s1"), new Variable("B"), new Symbol[]{new Symbol("2"), new Variable("C")}),
        new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2"), new Variable("C")}),

        new CFLReachability.AnalysisTransition(new State("s3"), new State("s1"), new Variable("C"), new Symbol[]{new Symbol("3")}),
        new CFLReachability.AnalysisTransition(new State("s3"), new State("s1"), new Variable("C"), new Symbol[]{new Symbol("3"), new Variable("A")}),
        new CFLReachability.AnalysisTransition(new State("s3"), new State("s3"), new Variable("C"), new Symbol[]{new Symbol("3")}),
        new CFLReachability.AnalysisTransition(new State("s3"), new State("s3"), new Variable("C"), new Symbol[]{new Symbol("3"), new Variable("A")}),
      }
    ); 
    assertEquals(fst2, CFLReachability.analyze(fst, cfg, new CFLReachability.SimpleTransitionFactory()));
  }

  public void testReachabilityTraceable(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
      }
    ); 
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),

        new CFLReachability.AnalysisTransition(new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
        new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")}),
        new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("0"), new Variable("B")}),

        /*
                    new ProductionRuleTransition(
                            new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")},
                            new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")})),
                    new ProductionRuleTransition(
                            new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")},
                            new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")})),
                    new ProductionRuleTransition(
                            new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("0"), new Variable("B")},
                            new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")})),
         */
      }
    );
    Set expected = AUtil.set(new ITransition[]{
      new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
      new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
      new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
      new CFLReachability.AnalysisTransition(new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
      new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")}),
      new CFLReachability.AnalysisTransition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("0"), new Variable("B")}),
    });
    CFLReachability.ITransitionFactory factory = new CFLReachability.SimpleTransitionFactory();
    CFLReachability.TraceableTransitionFactory traceableFactory = new CFLReachability.TraceableTransitionFactory(factory);
    IAutomaton result = CFLReachability.analyze(fst, cfg, traceableFactory);
    assertEquals(fst2, result);
    Set s1 = CFLReachability.getCorrespondingTransitions(result, cfg);
    Set s2 = traceableFactory.getCorrespondingTransitions();
    Set c1 = CFLReachability.selectConnectedTransitions(
      s1,
      new State("s1"), new State[]{new State("s3")}, new Variable("A"));
    Set c2 = CFLReachability.selectConnectedTransitions(
      s2,
      new State("s1"), new State[]{new State("s3")}, new Variable("A"));
    assertTrue(c1.equals(c2));
    assertEquals(expected, c1);
  }

  public void testGetCorrespondingTransitions(){
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),

        new Transition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
      }
    );
    Set expected = AUtil.set(new CFLReachability.CorrespondingPath[]{
      new CFLReachability.CorrespondingPath(
        new Transition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")})
      ),
      new CFLReachability.CorrespondingPath(
        new Transition(new State("s1"), new State("s3"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), new Variable("B"), new Symbol[]{new Symbol("2")})
      ),
    });
    Set transitions = CFLReachability.getCorrespondingTransitions(fst, cfg);
    assertEquals(expected, transitions);
  }

  public void testReachabilityWithEpsilonTransition(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s4")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s3"), Transition.EpsilonSymbol),
        new Transition(new State("s3"), new State("s4"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s3"), new State("s3"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
      }
    ); 
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    Automaton fst2 = new Automaton(
      new State("s1"),
      new State[]{new State("s4")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
        new Transition(new State("s2"), new State("s4"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s3"), new State("s4"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
        new Transition(new State("s2"), new State("s3"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
        new Transition(new State("s3"), new State("s3"), new Symbol("0"), new Symbol[]{new Symbol("0")}),

        new CFLReachability.AnalysisTransition(new State("s1"), new State("s4"), new Variable("A"), new Symbol[]{new Symbol("1"), new Variable("B")}),
        new CFLReachability.AnalysisTransition(new State("s2"), new State("s4"), new Variable("B"), new Symbol[]{new Symbol("2")}),
        new CFLReachability.AnalysisTransition(new State("s3"), new State("s4"), new Variable("B"), new Symbol[]{new Symbol("2")}),
      }
    );
    IAutomaton result = CFLReachability.analyze(fst, cfg, new CFLReachability.SimpleTransitionFactory());
    assertEquals(fst2, result);
    assertTrue(CFLReachability.isReachable(fst,cfg));
    //assertTrue(CFLReachability.isReachable(fst,cfg, new State("s1"), new State("s4")));
    //assertFalse(CFLReachability.isReachable(fst,cfg, new State("s1"), new State("s2")));
    //assertFalse(CFLReachability.isReachable(fst,cfg, new State("s2"), new State("s4")));
  }

  public void testAutomatonContainsAllCFL(){
    Automaton fst = new Automaton(
      new State("s1"),
      new State[]{new State("s3")},
      new Transition[]{
        new Transition(new State("s1"), new State("s2"), new Symbol("a")),
        new Transition(new State("s2"), new State("s3"), new Symbol("b")),
        new Transition(new State("s2"), new State("s2"), new Symbol("0")),
      }
    );
    ContextFreeGrammar cfg = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
      }
    );
    assertTrue(CFLReachability.containsAll(fst, cfg));
  }
}
