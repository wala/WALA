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
package com.ibm.wala.automaton.test.grammar.tree;

import com.ibm.wala.automaton.grammar.tree.ITreeGrammar;
import com.ibm.wala.automaton.regex.string.StringPatternSymbol;
import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.test.tree.TreeJunitBase;

public class TestRTLComparator extends TreeJunitBase {
  /*
       g = {
         $A -> ();
         $A -> a((),$A);
         $A -> a((),$B);
         $B -> ();
         $B -> b((),$B);
       }
       h = {
         $AB -> ();
         $AB -> a((),$AB);
         $AB -> b((),$AB);
       }
   */
  public void testRTLComparator1() {
    ITreeGrammar g = TG(
      BV("A"),
      new Object[]{
        BV("A"), BT("a", null, BV("A")),
        BV("A"), BT("a", null, BV("B")),
        BV("B"), null,
        BV("B"), BT("b", null, BV("B")),
      }
    );
    ITreeGrammar h = TG(
      BV("AB"),
      new Object[]{
        BV("AB"), null,
        BV("AB"), BT("a", null, BV("AB")),
        BV("AB"), BT("b", null, BV("AB")),
      }
    );
    assertContains(h, g);
    assertNotContains(g, h);
  }

  /*
    g = {
      $A -> ();
      $A -> a((),$A);
      $A -> a((),$B);
      $B -> ();
      $B -> c((),$B);
    }

    h = {
      $AB -> ();
      $AB -> a((),$AB);
      $AB -> b((),$AB);
    }
   */
  public void testRTLComparator2() {
    ITreeGrammar g = TG(
      BV("A"),
      new Object[]{
        BV("A"), null,
        BV("A"), BT("a", null, BV("A")),
        BV("A"), BT("a", null, BV("B")),
        BV("B"), null,
        BV("B"), BT("c", null, BV("B")),
      }
    );
    ITreeGrammar h = TG(
      BV("AB"),
      new Object[]{
        BV("AB"), null,
        BV("AB"), BT("a", null, BV("AB")),
        BV("AB"), BT("b", null, BV("AB")),
      }
    );
    assertNotContains(g, h);
    assertNotContains(h, g);
  }

  /*
    Grammar g3 = {
      $G -> a((),());
    }

    Grammar h3 = {
      $G -> a((),());
    }
   */
  public void testRTLComparator3() {
    ITreeGrammar g = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("a"),
      }
    );
    ITreeGrammar h = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("a"),
      }
    );
    assertContains(g, h);
    assertContains(h, g);
  }

  /*
    Grammar f4 = {
      $G -> l(a((),()),());
    }

    Grammar h4 = {
      $G -> l(b((),()),());
    }
   */
  public void testRTLComparator4() {
    ITreeGrammar g = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("l", BT("a"), null),
      }
    );
    ITreeGrammar h = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("l", BT("a"), null),
      }
    );
    assertContains(g, h);
    assertContains(h, g);
  }

  /*
    Grammar g5 = {
      $G -> b(b((),()),());
    }

    Grammar h5 = {
      $G -> a($G,());
      $G -> b($G,());
      $G -> ();
    }
   */
  public void testRTLComparator5() {
    ITreeGrammar g = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("b", BT("b"), null),
      }
    );
    ITreeGrammar h = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("a", BV("G"), null),
        BV("G"), BT("b", BV("G"), null),
        BV("G"), null,
      }
    );
    assertNotContains(g, h);
    assertContains(h, g);
  }

  /*
    Grammar g6 = {
      $G -> a(b((),c((),())), ());
    }

    Grammar h6 = {
      $G -> a($H, ());
      $H -> b((),c((),()));
      $H -> b((),());
    }
   */
  public void testRTLComparator6() {
    ITreeGrammar g = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("a", BT("b", null, BT("c")), null),
      }
    );
    ITreeGrammar h = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("a", BV("H"), null),
        BV("H"), BT("b", null, BT("c")),
        BV("H"), BT("b"),
      }
    );
    assertNotContains(g, h);
    assertContains(h, g);
  }

  /*
   * g = {
   *   $A -> a($C,$B)
   *   $B -> b((),())
   *   $C -> c((),())
   * }
   * 
   * h = {
   *   $A -> a($C,$B)
   *   $B -> $BB
   *   $C -> $CC
   *   $BB -> b((),())
   *   $CC -> c((),())
   * }
   */
  public void testRTLComparator7() {
    ITreeGrammar g = TG(
      BV("A"),
      new Object[]{
        BV("A"), BT("a", BV("B"), BV("C")),
        BV("B"), BT("b"),
        BV("C"), BT("c"),
      }
    );
    ITreeGrammar h = TG(
      BV("A"),
      new Object[]{
        BV("A"), BT("a", BV("B"), BV("C")),
        BV("B"), BV("BB"),
        BV("C"), BV("CC"),
        BV("BB"), BT("b"),
        BV("CC"), BT("c"),
      }
    );
    assertContains(g, h);
    assertContains(h, g);
  }

  /*
   * g = {
   *   $G -> ();
   * }
   * 
   * h = {
   *   $G -> a[];
   * }
   */
  public void testRTLComparator8() {
    ITreeGrammar g = TG(
      BV("G"),
      new Object[]{
        BV("G"), null,
      }
    );
    ITreeGrammar h = TG(
      BV("G"),
      new Object[]{
        BV("G"), BT("a"),
      }
    );
    assertNotContains(g, h);
    assertNotContains(h, g);
  }
  /*
   * g = {
   *   $A -> "a|A"($B, $C)
   *   $B -> "b|B"((),())
   *   $C -> "c|C"((),())
   * }
   * h = {
   *   $A -> a($B, $C)
   *   $B -> b((),())
   *   $C -> c((),())
   * }
   */
  public void testRTLComparatorWithPattern1() {
    StringPatternSymbol a = new StringPatternSymbol("a|A");
    StringPatternSymbol b = new StringPatternSymbol("b|B");
    StringPatternSymbol c = new StringPatternSymbol("c|C");
    ITreeGrammar g = TG(
      BV("A"),
      new Object[]{
        BV("A"), BT(a, BV("B"), BV("C")),
        BV("B"), BT(b),
        BV("C"), BT(c),
      }
    );
    ITreeGrammar h = TG(
      BV("A"),
      new Object[]{
        BV("A"), BT(new CharSymbol("a"), BV("B"), BV("C")),
        BV("B"), BT(new CharSymbol("b")),
        BV("C"), BT(new CharSymbol("c")),
      }
    );
    assertContains(g, h);
    assertNotContains(h, g);
  }
}
