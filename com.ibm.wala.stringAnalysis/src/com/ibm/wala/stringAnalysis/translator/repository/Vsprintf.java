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

public class Vsprintf extends Transducer {

  private int arrayVarIndex;

  private final CharSymbol percentChar = new CharSymbol("%");

  private final CharSymbol plusChar = new CharSymbol("+");

  private final CharSymbol dChar = new CharSymbol("d");

  private final CharSymbol[] typeSpecifier = (CharSymbol[]) new StringSymbol(
      "dsfoxXbeuc").toCharSymbols().toArray(new CharSymbol[0]);

  public Vsprintf(int target, int array) {
    super(target);
    this.arrayVarIndex = array;
  }

  public Vsprintf(int array) {
    this.arrayVarIndex = array;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    List arrayVarList = getArrayVarList();
    String[] args = getArrayValues(arrayVarList);

    Set transitions = new HashSet();
    IState state = new State("s0");
    IState initState = state;
    Set finalStates = new HashSet();
    int stateIndex = 1;
    int argsLen = args.length;
    for (int i = 0; i < argsLen; i++) {
      IState s1 = new State("s" + (stateIndex++));
      IState s2 = new State("s" + (stateIndex++));
      IState s3 = new State("s" + (stateIndex++));
      finalStates.add(s1);
      finalStates.add(s2);
      finalStates.add(s3);
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
      for (int j = 0; j < argsLen; j++) {
        IState nextState = new State("ss" + (i * argsLen + j));
        ITransition t3 = generateFormatSpecifierTransition(s2, nextState,
          args[j]);
        transitions.add(t3);
        ITransition t4 = generateFormatTransition(s1, nextState, args[j]);
        transitions.add(t4);
        ITransition t5 = new Transition(nextState, s3, Transition.EpsilonSymbol);
        transitions.add(t5);
        finalStates.add(nextState);
      }
      ITransition t6 = new FilteredTransition(s1, state, v,
          new ISymbol[] { v }, null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = symbol.equals(percentChar);
              if (r)
                System.err.println("(symbol.equals(percentChar) " + symbol
                    + ")");
              return r;
            }
          });
      transitions.add(t6);
      state = s3;
    }

    ITransition t = new Transition(state, state, v, new ISymbol[] { v });
    transitions.add(t);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  private ITransition generateFormatTransition(IState s1, IState nextState,
                                               final String arg) {
    Variable v = new Variable("v");
    ITransition t = new FilteredTransition(s1, nextState, v,
        new ISymbol[] { v }, new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
            List newOutputs = new ArrayList();
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
            return newOutputs;
          }
        }, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = (!symbol.equals(percentChar))
                && (!symbol.equals(plusChar));
            if (r)
              System.err
                  .println("(!symbol.equals(percentChar) && (!symbol.equals(plucChar)");
            return r;
          }
        });
    return t;
  }

  private ITransition generateFormatSpecifierTransition(IState s2,
                                                        IState nextState,
                                                        final String arg) {
    Variable v = new Variable("v");
    ITransition t = new FilteredTransition(s2, nextState, v,
        new ISymbol[] { v }, new FilteredTransition.IFilter() {
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
    return t;
  }

  private String[] getArrayValues(List arrayVarList) {
    String[] args = new String[arrayVarList.size()];
    Set rules = (Set) grammar.getRules();
    Iterator arrayVarIte = arrayVarList.iterator();
    int index = 0;
    while (arrayVarIte.hasNext()) {
      Variable arrayVar = (Variable) arrayVarIte.next();
      Iterator rulesIte = rules.iterator();
      while (rulesIte.hasNext()) {
        ProductionRule rule = (ProductionRule) rulesIte.next();
        if (rule.getLeft().equals(arrayVar)) {
          List right = rule.getRight();
          args[index] = list2Str(right);
          index++;
          break;
        }
      }
    }
    return args;
  }

  private String list2Str(List right) {
    Iterator rightIte = right.iterator();
    StringBuffer sb = new StringBuffer();
    while (rightIte.hasNext()) {
      CharSymbol charSym = (CharSymbol) rightIte.next();
      sb.append(charSym.charValue());
    }
    return sb.toString();
  }

  private List getArrayVarList() {
    Variable arrayVar = (Variable) params.get(arrayVarIndex);
    List arrayVarList = new ArrayList();
    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(arrayVar)) {
        arrayVarList.add(rule.getRight().get(0));
      }
    }
    return arrayVarList;
  }

  protected List printAsciiCharacter(String arg) {
    Charset asciiCharset = Charset.forName("US-ASCII");
    CharsetDecoder decoder = asciiCharset.newDecoder();
    int argInt = 0;
    if (isNumber(arg)) {
      argInt = Integer.parseInt(arg);  
    }
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
