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
/**
 * 
 */
package com.ibm.wala.stringAnalysis.translator.repository;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.string.FilteredTransition;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.automaton.string.FilteredTransition.ICondition;
import com.ibm.wala.util.debug.Assertions;

public class StripCSlashes extends Transducer {
  public StripCSlashes(int target) {
    super(target);
  }

  public StripCSlashes() {
    super();
  }
  
  private static final CharSymbol SYM_BSLASH = new CharSymbol('\\');
  private static final ICondition IS_BSLASH = new ICondition() {
    public boolean accept(ISymbol symbol, IMatchContext ctx) {
      // Optimize for CharSymbol
      if (symbol instanceof CharSymbol) {
        char c = ((CharSymbol)symbol).charValue();
        return (c == '\\');
      }
      return symbol.equals(SYM_BSLASH);
    }
  };
  private static final ICondition NOT_BSLASH = new ICondition() {
    public boolean accept(ISymbol symbol, IMatchContext ctx) {
      return !IS_BSLASH.accept(symbol, ctx);
    }
  };
  protected static class IsChar implements ICondition {
    private final char c;
    public IsChar(char c) {
      this.c = c;
    }
    /* (non-Javadoc)
     * @see com.ibm.capa.util.automaton.string.FilteredTransition.ICondition#accept(com.ibm.capa.util.automaton.string.ISymbol, com.ibm.capa.util.automaton.string.IMatchContext)
     */
    public boolean accept(ISymbol symbol, IMatchContext ctx) {
      // Optimize for CharSymbol
      if (symbol instanceof CharSymbol) {
        char d = ((CharSymbol)symbol).charValue();
        return c == d;
      }
      String name =  symbol.getName();
      return ((name.length() == 1) && (name.charAt(0) == c));
    }
  }
  private static final IsChar IS_SMALL_X = new IsChar('x');
  private static final IsChar[] IS_HEX;
  private static final IsChar[] IS_OCTAL;
  static {
    char[] hex = "0123456789abcdefABCDEF".toCharArray();
    IS_HEX = new IsChar[hex.length];
    int i = 0;
    for (char c : hex) {
      IS_HEX[i++] = new IsChar(c);
    }
    IS_OCTAL = new IsChar[8];
    for (i=0; i<8; i++)
      IS_OCTAL[i] = IS_HEX[i];
  }
  private static final ICondition NOT_HEX = new ICondition() {
    public boolean accept(ISymbol symbol, IMatchContext ctx) {
      char d;
      if (symbol instanceof CharSymbol) {
        d = ((CharSymbol)symbol).charValue();
      } else {
        String name =  symbol.getName();
        if (name.length() != 1)
          return false;
        d = name.charAt(0);
      }
      if (d >= '0' && d <= '9')
        return false;
      if (d >= 'a' && d <= 'f')
        return false;
      if (d >= 'A' && d <= 'F')
        return false;
      return true;
    }
  };
  private static final ICondition NOT_OCTAL = new ICondition() {
    public boolean accept(ISymbol symbol, IMatchContext ctx) {
      char d;
      if (symbol instanceof CharSymbol) {
        d = ((CharSymbol)symbol).charValue();
      } else {
        String name =  symbol.getName();
        if (name.length() != 1)
          return false;
        d = name.charAt(0);
      }
      if (d >= '0' && d <= '7')
        return false;
      return true;
    }
  };
  private static final ICondition NOT_SPECIAL = new ICondition() {
    public boolean accept(ISymbol symbol, IMatchContext ctx) {
      char d;
      if (symbol instanceof CharSymbol) {
        d = ((CharSymbol)symbol).charValue();
      } else {
        String name =  symbol.getName();
        if (name.length() != 1)
          return false;
        d = name.charAt(0);
      }
      if (d >= '0' && d <= '7') // Octal digit
        return false;
      switch (d) {
      case 'n':
      case 'r':
      case 'a':
      case 't':
      case 'v':
      case 'b':
      case 'f':
      case '\\':
      case 'x':
        return false;
      }
      return true;
    }
  };
  private static class UnquoteMetaTransition extends FilteredTransition {
    private UnquoteMetaTransition(IState s0, IState s1, ISymbol in, char c, char out) {
      super(s0, s1, in, new CharSymbol[] {new CharSymbol(out)}, null, new IsChar(c));
    }
    private static class LF extends UnquoteMetaTransition {
      private LF(IState s0, IState s1, ISymbol in) {
        super(s0, s1, in, 'n', (char)10);
      }
    }
    private static class CR extends UnquoteMetaTransition {
      private CR(IState s0, IState s1, ISymbol in) {
        super(s0, s1, in, 'r', (char)13);
      }
    }
    private static class ALARM extends UnquoteMetaTransition {
      private ALARM(IState s0, IState s1, ISymbol in) {
        super(s0, s1, in, 'a', (char)7);
      }
    }
    private static class TAB extends UnquoteMetaTransition {
      private TAB(IState s0, IState s1, ISymbol in) {
        super(s0, s1, in, 't', (char)9);
      }
    }
    private static class VTAB extends UnquoteMetaTransition {
      private VTAB(IState s0, IState s1, ISymbol in) {
        super(s0, s1, in, 'v', (char)11);
      }
    }
    private static class BSPACE extends UnquoteMetaTransition {
      private BSPACE(IState s0, IState s1, ISymbol in) {
        super(s0, s1, in, 'b', (char)8);
      }
    }
    private static class FORMFEED extends UnquoteMetaTransition {
      private FORMFEED(IState s0, IState s1, ISymbol in) {
        super(s0, s1, in, 'f', (char)12);
      }
    }
    private static class BSLASH extends UnquoteMetaTransition {
      private BSLASH(IState s0, IState s1, ISymbol in) {
        super(s0, s1, in, '\\', '\\');
      }
    }
    public static ITransition[] makeTransitions(IState s0, IState s1, ISymbol in) {
      return new ITransition[] {
        new ALARM(s0, s1, in),
        new BSLASH(s0, s1, in),
        new BSPACE(s0, s1, in),
        new CR(s0, s1, in),
        new FORMFEED(s0, s1, in),
        new LF(s0, s1, in),
        new TAB(s0, s1, in),
        new VTAB(s0, s1, in),
      };
    }
  }
  
  protected int hexToInt(char c) {
    if (c >= '0' && c <= '9')
      return (int)(c - '0');
    if (c >= 'a' && c <= 'f')
      return (int)(c - 'a' + 10);
    if (c >= 'A' && c <= 'F')
      return (int)(c - 'A' + 10);
    Assertions.UNREACHABLE("Not a hex digit: " + c);
    return Integer.MIN_VALUE;
  }
  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Set<ITransition> transitions = new HashSet<ITransition>();
    IState s0 = new State("s0");
    IState initState = s0;
    Set<IState> finalStates = new HashSet<IState>();
    finalStates.add(s0);

    /* Step 1. Add Transitions for normal cases */
    // Transition for non-backslash characters
    // s0 -([^\])-> s0
    ITransition t = new FilteredTransition(s0, s0, v, new ISymbol[] { v }, null, NOT_BSLASH);
    transitions.add(t);

    // Transition for backslash at the end of input
    // s0 -(\$END-OF-STRING)-> sX
    IState sX = new State("sX");
    t = new FilteredTransition(s0, sX, v, new ISymbol[] { v }, null, IS_BSLASH);
    transitions.add(t);
    finalStates.add(sX);
    
    /* Step 2. Add Transitions for striping backslashes */
    // Transition for backslash not at the end of input
    // s0 -(\)-> s1
    IState s1 = new State("s1");
    t = new FilteredTransition(s0, s1, v, new ISymbol[] {}, null, IS_BSLASH);
    transitions.add(t);

    /* Step 2-1. Add Transitions for non-meta characters */
    // Transition for non-meta characters
    // s1 -(.)-> s0, output the input
    t = new FilteredTransition(s1, s0, v, new ISymbol[] { v }, null, NOT_SPECIAL);
    transitions.add(t);
    
    /* Step 2-2. Add Transitions for convertiong meta-characters */
    // Transtions for meta-characters
    // s1 -(n)-> s0, output '\n';
    // s1 -(r)-> s0, output '\r';
    // ...
    ITransition[] metaCharTrans = UnquoteMetaTransition.makeTransitions(s1, s0, v);
    for (ITransition tr : metaCharTrans) {
      transitions.add(tr);
    }

    /* Step 2-3. Add Transitions for convertiong hex values */
    // Transition for small 'x' after '\'
    // s1 -(x)-> sx
    IState sx = new State("sx");
    t = new FilteredTransition(s1, sx, v, new ISymbol[]{}, null, IS_SMALL_X);
    transitions.add(t);

    // Transitions for hex characters after "\x"
    // sx -(0)-> sx0
    // ...
    // sx -(F)-> sxF
    for (IsChar cond: IS_HEX) {
      char c = cond.c;
      IState sxc = new State("sx" + c);
      t = new FilteredTransition(sx, sxc, v, new ISymbol[]{}, null, cond);
      transitions.add(t);
      // Transitions for another hex
      // sxc -(0)-> s0; output hex-value of the two characters
      // ..
      // sxc -(F)-> s0; output hex-value of the two characters
      for (IsChar cond2: IS_HEX) {
        int val = hexToInt(c)*16 + hexToInt(cond2.c);
        ISymbol out = new CharSymbol((char)val);
        t = new FilteredTransition(sxc, s0, v, new ISymbol[] {out}, null, cond2);
        transitions.add(t);
      }
      
      // Transitions for non-hex characters
      // sxc -(.)-> s0; output hex-value of c and the input
      int val = hexToInt(c);
      ISymbol hexOut = new CharSymbol((char)val);
      t = new FilteredTransition(sxc, s0, v, new ISymbol[] { hexOut, v }, null, NOT_HEX);
      transitions.add(t);
    }
    /* End of tep 2-3 */
    
    /* Step 2-4. Add Transitions for convertiong octal values */
    // Transitions for octal digits after '\'
    // s1 -(0)-> sO0
    // ...
    // s1 -(7)-> sO7
    for (IsChar cond: IS_OCTAL) {
      char c = cond.c;
      String nm = "sO" + c;
      IState sOc = new State(nm);
      t = new FilteredTransition(s1, sOc, v, new ISymbol[]{}, null, cond);
      transitions.add(t);
      
      // Transitions for sequence of two octal digits
      // sOc -(0)-> sOc0
      // ...
      // sOc -(7)-> sOc7
      for (IsChar cond2: IS_OCTAL) {
        char c2 = cond2.c;
        IState sOc_c2 = new State(nm + c2);
        t = new FilteredTransition(sOc, sOc_c2, v, new ISymbol[]{}, null, cond2);
        transitions.add(t);
        // Transitions for sequence of three octal digits
        // sOc_c2 -(0)-> s0; output octal value of the three-character sequence
        // ...
        // sOc_c2 -(7)-> s0; output octal value of the three-character sequence
        for (IsChar cond3: IS_OCTAL) {
          char c3 = cond3.c;
          int val = (c - '0')*8*8 + (c2 - '0')*8 + (c3 - '0');
          ISymbol out = new CharSymbol((char)val);
          t = new FilteredTransition(sOc_c2, s0, v, new ISymbol[]{out}, null, cond3);
          transitions.add(t);
        }
        // Transition for non-octal characters after two octal digits
        // sOc_c2 -(.)-> s0; output octal value of the two-character sequence and the input
        int val = (c - '0')*8 + (c2 - '0');
        ISymbol out = new CharSymbol((char)val);
        t = new FilteredTransition(sOc_c2, s0, v, new ISymbol[]{out, v}, null, NOT_OCTAL);
        transitions.add(t);
      }
      // Transition for non-octal characters after one octal digits
      // sOc -(.)-> s0; output octal value of the digit and the input
      int val = (c - '0');
      ISymbol out = new CharSymbol((char)val);
      t = new FilteredTransition(sOc, s0, v, new ISymbol[]{out, v}, null, NOT_OCTAL);
      transitions.add(t);
    }
    /* End of Step 2-4 */

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}