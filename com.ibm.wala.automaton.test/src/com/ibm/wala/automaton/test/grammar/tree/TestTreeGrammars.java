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

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.grammar.tree.*;
import com.ibm.wala.automaton.string.DeepSymbolCopier;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.test.tree.TreeJunitBase;
import com.ibm.wala.automaton.tree.*;

public class TestTreeGrammars extends TreeJunitBase {
  public void testTreeGrammarsNormalize1() {
    ITreeGrammar tg = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BT("c")),
        BV(2), BT("b", BV(2), BinaryTree.LEAF),
      }
    );
    ITreeGrammar ex = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BV("N2")),
        BV(2), BT("b", BV(2), BV("N1")),
        BV("N1"), BinaryTree.LEAF,
        BV("N2"), BT("c", BV("N1"), BV("N1")),
      }
    );
    TreeGrammars.normalize(tg);
    assertEquals(ex, tg);
  }

  public void testTreeGrammarsNormalize2() {
    ITreeGrammar tg = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BT("c")),
        BV(2), BT("b", BV(2), BinaryTree.LEAF),
        BV(2), BV(2),
      }
    );
    ITreeGrammar ex = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BV("N2")),
        BV(2), BT("b", BV(2), BV("N1")),
        BV("N1"), BinaryTree.LEAF,
        BV("N2"), BT("c", BV("N1"), BV("N1")),
      }
    );
    TreeGrammars.normalize(tg);
    assertEquals(ex, tg);
  }

  public void testTreeGrammarAppend1() {
    ITreeGrammar tg = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BT("c")),
        BV(2), BT("b", BV(2), BinaryTree.LEAF),
        BV(2), BT("c"),
        BV(2), BV(2),
      }
    );
    ITreeGrammar ex = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BT("c", null, BT("z"))),
        BV(2), BT("b", BV(2), BinaryTree.LEAF),
        BV(2), BT("c"),
        BV(2), BV(2),
      }
    );
    TreeGrammars.append(tg, BT("z"));
    assertContains(ex, tg);
    assertContains(tg, ex);
  }
  
  public void testTreeGrammarAppend2() {
    ITreeGrammar tg = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), null),
        BV(2), BT("b", BV(2), BinaryTree.LEAF),
        BV(2), BT("c"),
        BV(2), BV(2),
      }
    );
    ITreeGrammar ex = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BT("z")),
        BV(2), BT("b", BV(2), BinaryTree.LEAF),
        BV(2), BT("c"),
        BV(2), BV(2),
      }
    );
    TreeGrammars.append(tg, BT("z"));
    assertContains(ex, tg);
    assertContains(tg, ex);
  }
  
  public void testTreeGrammarAppendChild1() {
    ITreeGrammar tg = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BT("c")),
        BV(2), BT("b", BV(2), BinaryTree.LEAF),
        BV(2), BT("c"),
        BV(2), BV(2),
      }
    );
    ITreeGrammar ex = TG(
      BV(1),
      new Object[]{
        BV(1), BT("a", BV(2), BT("c")),
        BV(2), BT("b", BV(3), BT("z")),
        BV(2), BT("c", null, BT("z")),
        BV(2), BT("b", BV(3), BT("z")),
        BV(3), BT("b", BV(3), BinaryTree.LEAF),
        BV(3), BT("c"),
      }
    );
    TreeGrammars.appendChild(tg, BT("z"));
    assertContains(ex, tg);
    assertContains(tg, ex);
  }
}
