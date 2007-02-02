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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.FilteredTransition;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.NumberSymbol;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.Variable;

public class Chr extends Transducer {

  public Chr(int target) {
    super(target);
  }

  public Chr() {
    super();
  }
  
  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Set<ITransition> transitions = new HashSet<ITransition>();
    IState s0 = new State("s0");
    IState initState = s0;
    Set<IState> finalStates = new HashSet<IState>();
    finalStates.add(s0);

    ITransition t = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
            List newOutputs = null;
            if (symbol instanceof NumberSymbol) {
              try {
                newOutputs = generateAsciiCharacter(((NumberSymbol) symbol)
                    .intValue());
              }
              catch (AsciiOutOfBoundException e) {
                e.printStackTrace();
              }
            }
            else {
              throw new IllegalArgumentException();
            }
            return newOutputs;
          }
        }, null);
    transitions.add(t);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  protected List generateAsciiCharacter(int argInt)
      throws AsciiOutOfBoundException {
    Charset asciiCharset = Charset.forName("US-ASCII");
    CharsetDecoder decoder = asciiCharset.newDecoder();
    if (!(0 <= argInt && argInt <= 127)) {
      throw new AsciiOutOfBoundException(argInt);
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

  protected class AsciiOutOfBoundException extends Exception {
    public AsciiOutOfBoundException(int i) {
      super("AsciiOutOfBoundException");
    }
  }
}
