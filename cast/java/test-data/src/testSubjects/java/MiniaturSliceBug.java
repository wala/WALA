/*
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
abstract class PrimitiveWrapper {
    
  /**
   * Sets the integer representation of the underlying primitive
   * to the given value.
   */
  public abstract void setIntValue(int i);
    
  /**
   * Returns the integer representation of the underlying primitive.
   * @return this.intVal
   */
  public abstract int intValue();
	
  /**
   * Returns true if this and the given object are 
   * pri
   * {@inheritDoc}
   */
  
  @Override
  abstract public boolean equals(Object o);
}

final class IntWrapper extends PrimitiveWrapper {
  private int val;
	
  /**
   * Constructs a wrapper for the given int.
   */
  public IntWrapper(int val) { 
    this.val = val;
  }

  @Override
  public int intValue() {
    return val;
  }

  @Override
  public void setIntValue(int i) {
    this.val = i;
  }
	
  @Override
  public boolean equals(Object o) {
    return o instanceof IntWrapper && ((IntWrapper)o).val==val;
  }
}

public class MiniaturSliceBug {

  public void validNonDispatchedCall(IntWrapper wrapper) { 
    wrapper.setIntValue(3);
    assert wrapper.intValue() == 3;
    wrapper.equals(wrapper);
  }

  public static void main(String[] args) {
    (new MiniaturSliceBug()).validNonDispatchedCall(new IntWrapper(-1));
  }

}
