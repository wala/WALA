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

import java.util.Collection;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.fixpoint.AbstractVariable;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ssa.SymbolTable;

/**
 * States for the ssa variables.
 * 
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 *
 */
public class NullPointerState extends AbstractVariable<NullPointerState> {
  
  /*
   * Inital state is UNKNOWN.
   * Lattice: BOTH < { NULL, NOT_NULL } < UNKNOWN
   */
  public enum State { UNKNOWN, BOTH, NULL, NOT_NULL }
  
  // maps ssa variable number -> State
  private final State[] vars;
  
  NullPointerState(int maxVarNum, SymbolTable symbolTable, ParameterState parameterState) {
    this(maxVarNum, symbolTable, parameterState, State.UNKNOWN);
  }

  NullPointerState(int maxVarNum, SymbolTable symbolTable, ParameterState parameterState, State defaultState) {
    this.vars = new State[maxVarNum + 1];
    
    // Initialize the states
    for (int i = 0; i < vars.length; i++) {
      if (symbolTable.isConstant(i)){
        if (symbolTable.isNullConstant(i)){
          vars[i] = State.NULL;
        } else {
          vars[i] = State.NOT_NULL;
        }
      } else {
        vars[i] = defaultState;
      }
    }
    
    // Add what we know about the parameters (if we know anything about them). 
    // They are the first vars by convention.
    if (parameterState != null) {
      for (int i = 0; i < parameterState.getStates().size(); i++){
        assert parameterState.getState(i) != null;
        vars[i + 1] = parameterState.getState(i);
        assert vars[i + 1] != null;
      }
    }
  }

  static AbstractMeetOperator<NullPointerState> meetOperator() {
    return StateMeet.INSTANCE;
  }
  
  /**
   * This function is not distributive, therefore we cannot use the kildall framework.
   * <pre>
   * v3 = phi v1, v2
   * ^ := Meet-operator
   * f := phiValueMeetFunction(3, {1, 2}) = v1,v2,v3 -&gt; v1,v2,[v1 ^ v2]
   * 
   * f(1,?,?) ^ f(?,1,?) = 1,?,? ^ ?,1,? = 1,1,?
   * 
   * f(1,?,? ^ ?,1,?) = f(1,1,?) = 1,1,1
   * 
   * => f(1,?,? ^ ?,1,?) != f(1,?,?) ^ f(?,1,?)
   * </pre> 
   */
  static UnaryOperator<NullPointerState> phiValueMeetFunction(int varNum, int[] fromVars) {
    return new PhiValueMeet(varNum, fromVars);
  }
  
  static UnaryOperator<NullPointerState> nullifyFunction(int varNum) {
    return new NullifyFunction(varNum);
  }
  
  static UnaryOperator<NullPointerState> denullifyFunction(int varNum) {
    return new DenullifyFunction(varNum);
  }
  
  static UnaryOperator<NullPointerState> identityFunction() {
    return IndentityFunction.INSTANCE;
  }
  
  static UnaryOperator<NullPointerState> phisFunction(Collection<UnaryOperator<NullPointerState>> phiFunctions) {
    return new OperatorUtil.UnaryOperatorSequence<>(phiFunctions);
  }

  boolean isNeverNull(int varNum) {
    assert varNum > 0 && varNum < vars.length;
    
    return vars[varNum] == State.NOT_NULL;
  }
  
  boolean isAlwaysNull(int varNum) {
    assert varNum > 0 && varNum < vars.length;
    
    return vars[varNum] == State.NULL;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.fixpoint.IVariable#copyState(com.ibm.wala.fixpoint.IVariable)
   */
  @Override
  public void copyState(NullPointerState v) {
    assert v.vars.length == vars.length;
    
    for (int i = 0; i < v.vars.length; i++) {
      vars[i] = v.vars[i];
    }
  }

  /**
   * This is the meet operator for the NullPointerState variables.
   * <pre>
   * ? == unknown, 1 == not null, 0 == null, * == both
   * 
   * meet | ? | 0 | 1 | * |  &lt;- rhs
   * -----|---|---|---|---|
   *    ? | ? | 0 | 1 | * |
   * -----|---|---|---|---|
   *    0 | 0 | 0 | * | * |
   * -----|---|---|---|---|
   *    1 | 1 | * | 1 | * |
   * -----|---|---|---|---|
   *    * | * | * | * | * |
   * ----------------------
   *    ^
   *    |
   *   lhs
   * </pre> 
   */
  boolean meet(final int varNum, final State rhs) {
    final State lhs = vars[varNum];

    if (lhs != State.BOTH && rhs != lhs && rhs != State.UNKNOWN) {
      if (lhs == State.UNKNOWN) {
        vars[varNum] = rhs;
        return true;
      } else {
        vars[varNum] = State.BOTH;
        return true;
      }
    } else {
      return false;
    }
  }

  boolean meet(NullPointerState other) {
    assert other.vars.length == vars.length;
    
    boolean changed = false;
    
    for (int i = 0; i < vars.length; i++) {
      changed |= meet(i, other.vars[i]);
    }
    
    return changed;
  }
  

  boolean nullify(int varNum) {
    if (vars[varNum] != State.NULL) {
      vars[varNum] = State.NULL;
      return true;
    }
    
    return false;
  }
  
  boolean denullify(int varNum) {
    if (vars[varNum] != State.NOT_NULL) {
      vars[varNum] = State.NOT_NULL;
      return true;
    }
    
    return false;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NullPointerState) {
      NullPointerState other = (NullPointerState) obj;
      assert vars.length == other.vars.length;
      
      for (int i = 0; i < vars.length; i++) {
        if (vars[i] != other.vars[i]) {
          return false;
        }
      }
      
      return true;
    }
    
    return false;
  }
  
  public State getState(int ssaVarNum){
    return vars[ssaVarNum];
  }
  
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer("<");
    for (State var : vars) {
      switch (var) {
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
  
  private static class StateMeet extends AbstractMeetOperator<NullPointerState> {

    private final static StateMeet INSTANCE = new StateMeet();
    
    private StateMeet() {}
    
    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
      return o instanceof StateMeet;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable[])
     */
    @Override
    public byte evaluate(NullPointerState lhs, NullPointerState[] rhs) {
      boolean changed = false;
      
      // meet rhs first
      for (NullPointerState state : rhs) {
        changed |= lhs.meet(state);
      }

      return (changed ? FixedPointConstants.CHANGED : FixedPointConstants.NOT_CHANGED);
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
     */
    @Override
    public int hashCode() {
      return 4711;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
     */
    @Override
    public String toString() {
      return "NullPointerStateMeet";
    }

  }
  
  private static class PhiValueMeet extends UnaryOperator<NullPointerState> {

    private final int varNum;
    private final int[] fromVars;
    
    /**
     * Creates an operator that merges the states of the given variables
     * fromVars into the state of the phi varaiable varNum
     * @param varNum Variable number of a phi value
     * @param fromVars Array of variable numbers the phi value refers to.
     */
    private PhiValueMeet(int varNum, int[] fromVars) {
      this.varNum = varNum;
      this.fromVars = fromVars;
    }
    
    @Override
    public byte evaluate(NullPointerState lhs, NullPointerState rhs) {
      boolean changed = false;
      if (!lhs.equals(rhs)) {
        lhs.copyState(rhs);
        changed = true;
      }
      lhs.vars[varNum] = State.UNKNOWN;
      for (int from : fromVars) {
          changed |= lhs.meet(varNum, rhs.vars[from]);
      }
      
      return (changed ? FixedPointConstants.CHANGED : FixedPointConstants.NOT_CHANGED);
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      
      if (o instanceof PhiValueMeet) {
        PhiValueMeet other = (PhiValueMeet) o;
        if (varNum == other.varNum && fromVars.length == other.fromVars.length) {
          for (int i = 0; i < fromVars.length; i++) {
            if (fromVars[i] != other.fromVars[i]) {
              return false;
            }
          }
          
          return true;
        }
      }
      
      return false;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
     */
    @Override
    public int hashCode() {
      return 11000 + varNum;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
     */
    @Override
    public String toString() {
      StringBuffer str = new StringBuffer("PhiValueMeet(" + varNum + ", [");
      
      for (int i = 0; i < fromVars.length; i++) {
        str.append(fromVars[i]);
        str.append(i == fromVars.length - 1 ? "" : ",");
      }
      
      str.append("])");
      
      return str.toString();
    }

  }
  
  
  private static class NullifyFunction extends UnaryOperator<NullPointerState> {

    private final int varNum;
    
    private NullifyFunction(int varNum) {
      this.varNum = varNum;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.UnaryOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable)
     */
    @Override
    public byte evaluate(NullPointerState lhs, NullPointerState rhs) {
      byte state = FixedPointConstants.NOT_CHANGED;
      
      if (!lhs.equals(rhs)) {
        lhs.copyState(rhs);
        state = FixedPointConstants.CHANGED;
      }
      
      if (lhs.nullify(varNum)) {
        state = FixedPointConstants.CHANGED;
      }
        
      return state;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
      return o instanceof NullifyFunction && ((NullifyFunction) o).varNum == varNum;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
     */
    @Override
    public int hashCode() {
      return 47000 + varNum;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
     */
    @Override
    public String toString() {
      return "Nullify(" + varNum + ")";
    }
    
  }

  private static class DenullifyFunction extends UnaryOperator<NullPointerState> {

    private final int varNum;
    
    private DenullifyFunction(int varNum) {
      assert varNum >= 0;
      this.varNum = varNum;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.UnaryOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable)
     */
    @Override
    public byte evaluate(NullPointerState lhs, NullPointerState rhs) {
      byte state = FixedPointConstants.NOT_CHANGED;
      
      if (!lhs.equals(rhs)) {
        lhs.copyState(rhs);
        state = FixedPointConstants.CHANGED;
      }
      
      if (lhs.denullify(varNum)) {
        state = FixedPointConstants.CHANGED;
      }
        
      return state;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
      return o instanceof DenullifyFunction && ((DenullifyFunction) o).varNum == varNum;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
     */
    @Override
    public int hashCode() {
      return -47000 - varNum;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
     */
    @Override
    public String toString() {
      return "Denullify(" + varNum + ")";
    }
    
  }
  
  private static class IndentityFunction extends UnaryOperator<NullPointerState> {

    private static final IndentityFunction INSTANCE = new IndentityFunction();
    
    private IndentityFunction() {
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.UnaryOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable)
     */
    @Override
    public byte evaluate(NullPointerState lhs, NullPointerState rhs) {
      if (lhs.equals(rhs)) {
        return FixedPointConstants.NOT_CHANGED;
      } else {
        lhs.copyState(rhs);
        return FixedPointConstants.CHANGED;
      }
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
      return o instanceof IndentityFunction;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
     */
    @Override
    public int hashCode() {
      return 8911;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
     */
    @Override
    public String toString() {
      return "Id";
    }
  }
}
