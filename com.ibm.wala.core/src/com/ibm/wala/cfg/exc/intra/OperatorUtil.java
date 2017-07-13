package com.ibm.wala.cfg.exc.intra;

import java.util.Arrays;
import java.util.Collection;

import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.UnaryOperator;

/**
 * Combinators for {@link UnaryOperator}
 * 
 * @author Martin Hecker, martin.hecker@kit.edu 
 */
public class OperatorUtil {
  
  /**
   * An operator of the form lhs = op_1(op_2(..op_n(rhs)..))
   * 
   * @author Martin Hecker, martin.hecker@kit.edu 
   */
  public static class UnaryOperatorSequence<T extends IVariable<T>> extends UnaryOperator<T> {
    
    final UnaryOperator<T>[] operators;
    
    @SuppressWarnings("unchecked")
    public UnaryOperatorSequence(Collection<UnaryOperator<T>> operators) {
      if (operators.size() == 0 ) throw new IllegalArgumentException("Empty Operator-Sequence");
      this.operators = operators.toArray(new UnaryOperator[operators.size()]);
    }
    
    @SafeVarargs
    public UnaryOperatorSequence(UnaryOperator<T>... operators) {
      if (operators.length == 0 ) throw new IllegalArgumentException("Empty Operator-Sequence");
      this.operators = Arrays.copyOf(operators, operators.length);
    }
  
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null) return false;
      if (getClass() != o.getClass()) return false;
      
      UnaryOperatorSequence other = (UnaryOperatorSequence) o;
      return operators.equals(other.operators);
    }
  
    @Override
    public int hashCode() {
      return operators.hashCode();
    }
    
    @Override
    public String toString() {
      return Arrays.toString(operators);
    }
    
    @Override
    public byte evaluate(T lhs, T rhs) {
      assert (operators.length > 0);
      int result = operators[0].evaluate(lhs, rhs);
      
      for (int i = 1 ; i < operators.length; i++) {
        byte changed = operators[i].evaluate(lhs, lhs);
        result =   ((result | changed) & FixedPointConstants.CHANGED_MASK)
                 | ((result | changed) & FixedPointConstants.SIDE_EFFECT_MASK)
                 | ((result & changed) & FixedPointConstants.FIXED_MASK);
      }
      
      return (byte) result;
    }
  }

}
