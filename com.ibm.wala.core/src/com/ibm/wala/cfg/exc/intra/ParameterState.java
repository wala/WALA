/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cfg.exc.intra;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.cfg.exc.intra.NullPointerState.State;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.fixpoint.AbstractVariable;
/**
 * Encapsulates the state of all parameters of an invoked method
 * 
 * @author markus
 *
 */
public class ParameterState extends AbstractVariable<ParameterState> {
  /*
   * Inital state is UNKNOWN.
   * Lattice: BOTH < { NULL, NOT_NULL } < UNKNOWN 
   * 
   * public enum State { UNKNOWN, BOTH, NULL, NOT_NULL }; as defined in NullPointerState
   * 
   */
  
  public static final int NO_THIS_PTR = -1;
  
  // maps the parmeter's varNum --> State
  private final HashMap<Integer, State> params = new HashMap<>();
  
  public static ParameterState createDefault(IMethod m) {
    ParameterState p = new ParameterState();
    
    if (!m.isStatic()) {
      // set this pointer to NOT_NULL
      p.setState(0, State.NOT_NULL);
    }
    
    return p;
  }
  
  public ParameterState() {
  }
  
  /**
   * Constructor to make a <code>ParameteState</code> out of a regular <code>NullPointerState</code>.
   * 
   * @param state The <code>NullPointerState</code> to parse.
   * @param parameterNumbers The numbers of parameters in <code>state</code>
   */
  public ParameterState(NullPointerState state, int[] parameterNumbers) {
    //by convention the first ssa vars are the parameters
    for (int i=0; i < parameterNumbers.length; i++){
      params.put(i, state.getState(parameterNumbers[i]));
    }
  }

  public void setState(int varNum, State state) {
    State prev = params.get(varNum);
    if (prev != null) {
      switch (prev) {
      case UNKNOWN:
        if (state != State.UNKNOWN) {
          throw new IllegalArgumentException("Try to set " + prev + " to " + state);
        }
        break;
      case NULL:
        if (!(state == State.BOTH || state == State.NULL)) {
          throw new IllegalArgumentException("Try to set " + prev + " to " + state);
        }
        break;
      case NOT_NULL:
        if (!(state == State.BOTH || state == State.NOT_NULL)) {
          throw new IllegalArgumentException("Try to set " + prev + " to " + state);
        }
        break;
      default:
        throw new UnsupportedOperationException(String.format("unexpected previous state %s", prev));
      }
    }
    params.put(varNum, state);
  }
  
  public HashMap<Integer, State> getStates() {
    return this.params;
  }
  
  /**
   * Returns the state of an specified parameter.
   * 
   * @param varNum The SSA var num of the parameter
   * @return the state of the parameter defined with <code>varNum</code>
   */
  public State getState(int varNum) {
    State state = params.get(varNum);
    if (state == null) {
      throw new IllegalArgumentException("No mapping for variable " + varNum + "in ParameterState " + this.toString());
    }
    return state;
  }

  @Override
  public void copyState(ParameterState v) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer("<");
    Set<Entry<Integer, State>> paramsSet = params.entrySet();
        
    for (Entry<Integer, State> param : paramsSet){
      switch (param.getValue()) {
      case BOTH:
        buf.append('*');
        break;
      case NOT_NULL:
        buf.append('1');
        break;
      case NULL:
        buf.append('0');
        break;
      case UNKNOWN:
        buf.append('?');
        break;
      default:
        throw new IllegalStateException();
      }
    }
    buf.append('>');
    
    return buf.toString();
  }
}
