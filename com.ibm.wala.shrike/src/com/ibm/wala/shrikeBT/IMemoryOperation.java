package com.ibm.wala.shrikeBT;

public interface IMemoryOperation {

  /**
   *  Denotes whether this instruction is taking the address of whatever
   * location it refers to.
   * @return whether this instruction is taking the address of a location
   */
  public boolean isAddressOf();
  
}
