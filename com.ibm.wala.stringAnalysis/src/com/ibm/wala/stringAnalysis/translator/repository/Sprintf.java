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
package com.ibm.wala.stringAnalysis.translator.repository;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.string.FilteredTransition;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Sprintf extends Transducer {

//  private int argsVarIndex;

  public Sprintf(int target, int args1, int args2) {
    super(target);
//    this.argsVarIndex = args1;
  }

  public Sprintf(int args1, int args2) {
//    this.argsVarIndex = args1;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    String[] args = getStrArgs();

    final CharSymbol percentChar = new CharSymbol("%");
    final CharSymbol plusChar = new CharSymbol("+");
    final CharSymbol dChar = new CharSymbol("d");
    final CharSymbol[] typeSpecifier = (CharSymbol[]) new StringSymbol(
        "dsfoxXbeuc").toCharSymbols().toArray(new CharSymbol[0]);

    Set transitions = new HashSet();
    IState state = new State("s0");
    IState initState = state;
    Set finalStates = new HashSet();
    int stateIndex = 1;
    for (int i = 0; i < args.length; i++) {
      IState s1 = new State("s" + (stateIndex++));
      IState s2 = new State("s" + (stateIndex++));
      IState s3 = new State("s" + (stateIndex++));
      IState s4 = new State("s" + (stateIndex++));
      IState s5 = new State("s" + (stateIndex++));
      finalStates.add(s1);
      finalStates.add(s2);
      finalStates.add(s3);
      finalStates.add(s4);
      finalStates.add(s5);
      final String arg = args[i];
      ITransition t0 = new FilteredTransition(state, state, v,
          new ISymbol[] { v }, null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = !symbol.equals(percentChar);
              if (r)
                System.err.println("(!symbol.equals(percentChar): accept "
                    + symbol + ")");
              return r;
            }
          });
      transitions.add(t0);
      ITransition t1 = new FilteredTransition(state, s1, v, new ISymbol[] {},
          null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = symbol.equals(percentChar);
              if (r)
                System.err.println("(symbol.equals(percentChar) " + symbol
                    + ")");
              return r;
            }
          });
      transitions.add(t1);
      ITransition t2 = new FilteredTransition(s1, s2, v, new ISymbol[] {},
          null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = symbol.equals(plusChar);
              if (r)
                System.err.println("(symbol.equals(plusChar) " + symbol + ")");
              return r;
            }
          });
      transitions.add(t2);
      ITransition t3 = new FilteredTransition(s2, s4, v, new ISymbol[] { v },
          new FilteredTransition.IFilter() {
            public List invoke(ISymbol symbol, List outputs) {
              if (symbol.equals(dChar)) {
                return formatSignSpecifier(arg);
              }
              if (symbol.equals(percentChar)) {
                return outputs;
              }
              outputs.clear();
              return outputs;
            }
          }, null);
      transitions.add(t3);
      ITransition t4 = new FilteredTransition(s1, s3, v, new ISymbol[] { v },
          new FilteredTransition.IFilter() {
            public List invoke(ISymbol symbol, List outputs) {
//              List newOutputs = new ArrayList();
              if (symbol.equals(typeSpecifier[0])) {
                return formatStandardInteger(arg);
              }
              if (symbol.equals(typeSpecifier[1])) {
                return formatString(arg);
              }
              if (symbol.equals(typeSpecifier[2])) {
                return formatFloatingPoint(arg);
              }
              if (symbol.equals(typeSpecifier[3])) {
                return formatOctal(arg);
              }
              if (symbol.equals(typeSpecifier[4])) {
                return formatHexadecimalLowerCase(arg);
              }
              if (symbol.equals(typeSpecifier[5])) {
                return formatHexadecimalUpperCase(arg);
              }
              if (symbol.equals(typeSpecifier[6])) {
                return formatBinary(arg);
              }
              if (symbol.equals(typeSpecifier[7])) {
                return formatScientificNotation(arg);
              }
              if (symbol.equals(typeSpecifier[8])) {
                return formatUnsignedInteger(arg);
              }
              if (symbol.equals(typeSpecifier[9])) {
                return printAsciiCharacter(arg);
              }
              return outputs;
            }
          }, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = (!symbol.equals(percentChar))
                  && (!symbol.equals(plusChar));
              if (r)
                System.err
                    .println("(!symbol.equals(percentChar)) && (!symbol.equals(plusChar): accept "
                        + symbol + ")");
              return r;
            }
          });
      transitions.add(t4);
      ITransition t5 = new FilteredTransition(s1, state, v,
          new ISymbol[] { v }, null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = symbol.equals(percentChar);
              if (r)
                System.err.println("(symbol.equals(percentChar): accept " + symbol
                    + ")");
              return r;
            }
          });
      transitions.add(t5);
      ITransition t6 = new Transition(s3, s5, Transition.EpsilonSymbol);
      ITransition t7 = new Transition(s4, s5, Transition.EpsilonSymbol);
      transitions.add(t6);
      transitions.add(t7);
      state = s5;
    }

    ITransition t = new Transition(state, state, v, new ISymbol[] { v });
    transitions.add(t);
    finalStates.add(state);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  private String[] getStrArgs() {
    int argsLen = params.size() - 1;
    String[] args = new String[argsLen];
    Variable[] argVars = new Variable[argsLen];
    for (int i = 0; i < argsLen; i++) {
      argVars[i] = (Variable) params.get(i + 1);
    }
    Set rules = (Set) grammar.getRules();
    for (int i = 0; i < argsLen; i++) {
      Iterator rulesIte = rules.iterator();
      while (rulesIte.hasNext()) {
        ProductionRule rule = (ProductionRule) rulesIte.next();
        List right = new ArrayList();
        if (rule.getLeft().equals(argVars[i])) {
          right = rule.getRight();
        }
        if (!right.isEmpty()) {
          Iterator rightIte = right.iterator();
          StringBuffer sb = new StringBuffer();
          while (rightIte.hasNext()) {
            CharSymbol charSym = (CharSymbol) rightIte.next();
            sb.append(charSym.charValue());
          }
          args[i] = sb.toString();
          break;
        }
      }
    }
    return args;
  }

  protected List printAsciiCharacter(String arg) {
    int argInt = 0;
    if (isNumber(arg)) {
      argInt = Integer.parseInt(arg);
    }
    Charset asciiCharset = Charset.forName("US-ASCII");
    CharsetDecoder decoder = asciiCharset.newDecoder();
    if (!(0 <= argInt && argInt <= 127)) {
      return new ArrayList();
    }
    byte b1 = (byte) argInt;
    byte[] b = { b1 };
    ByteBuffer asciiBytes = ByteBuffer.wrap(b);
    CharBuffer cBuf = null;
    try {
      cBuf = decoder.decode(asciiBytes);
    }
    catch (CharacterCodingException e) {
      e.printStackTrace();
    }
    StringSymbol strSym = new StringSymbol(cBuf.toString());
    return strSym.toCharSymbols();
  }

  protected List formatSignSpecifier(String arg) {
    int argInt = 0;
    if (isNumber(arg)) {
      argInt = Integer.parseInt(arg);
    }
    StringSymbol strSym = new StringSymbol(String.valueOf(argInt));
    if (argInt >= 0) {
      strSym = new StringSymbol("+" + String.valueOf((argInt)));
    }
    return strSym.toCharSymbols();
  }

  protected List formatUnsignedInteger(String arg) {
    int argInt = 0;
    if (isNumber(arg)) {
      argInt = Integer.parseInt(arg);
    }
    double argDouble = argInt;
    if (argInt < 0) {
      argDouble = argInt + Math.pow(2, 32);
      System.out.println(argDouble);
    }
    DecimalFormat df = new DecimalFormat("0");
    StringSymbol strSym = new StringSymbol(df.format(argDouble));
    return strSym.toCharSymbols();
  }

  protected List formatScientificNotation(String arg) {
    if (!isNumber(arg)) {
      arg = "0";
    }
    DecimalFormat df = new DecimalFormat("0.00000E0");
    String argStr = df.format(Double.parseDouble(arg));
    int ePos = argStr.indexOf("E");
    int minusPos = argStr.indexOf("-");
    if (minusPos < 0) {
      StringBuffer sb = new StringBuffer(argStr);
      sb.replace(ePos, ePos + 1, "e");
      sb.insert(ePos + 1, "+");
      argStr = sb.toString();
    }
    StringSymbol strSym = new StringSymbol(argStr);
    return strSym.toCharSymbols();
  }

  protected List formatBinary(String arg) {
    if (!isNumber(arg)) {
      arg = "0";
    }
    StringSymbol strSym = new StringSymbol(Integer.toBinaryString(Integer
        .parseInt(arg)));
    return strSym.toCharSymbols();
  }

  protected List formatHexadecimalUpperCase(String arg) {
    if (!isNumber(arg)) {
      arg = "0";
    }
    String argStr = Integer.toHexString(Integer.parseInt(arg));
    StringSymbol strSym = new StringSymbol(argStr.toUpperCase());
    return strSym.toCharSymbols();
  }

  protected List formatHexadecimalLowerCase(String arg) {
    if (!isNumber(arg)) {
      arg = "0";
    }
    StringSymbol strSym = new StringSymbol(Integer.toHexString(Integer
        .parseInt(arg)));
    return strSym.toCharSymbols();
  }

  protected List formatOctal(String arg) {
    if (!isNumber(arg)) {
      arg = "0";
    }
    StringSymbol strSym = new StringSymbol(Integer.toOctalString(Integer
        .parseInt(arg)));
    return strSym.toCharSymbols();
  }

  protected List formatFloatingPoint(String arg) {
    if (!isNumber(arg)) {
      arg = "0";
    }
    DecimalFormat df = new DecimalFormat("0.000000");
    StringSymbol strSym = new StringSymbol(df.format(Double.parseDouble(arg)));
    return strSym.toCharSymbols();
  }

  protected List formatString(String arg) {
    StringSymbol strSym = new StringSymbol(arg);
    return strSym.toCharSymbols();
  }

  protected List formatStandardInteger(String arg) {
    if (!isNumber(arg)) {
      arg = "0";
    }
    BigDecimal bigDecimal = new BigDecimal(arg);
    StringSymbol strSym = new StringSymbol(String
        .valueOf(bigDecimal.intValue()));
    return strSym.toCharSymbols();
  }

  private boolean isNumber(String arg) {
    char[] charArray = arg.toCharArray();
    for (int i = 0; i < charArray.length; i++) {
      if (Character.isDigit(charArray[i])) {
        return true;
      }
    }
    return false;
  }
}
