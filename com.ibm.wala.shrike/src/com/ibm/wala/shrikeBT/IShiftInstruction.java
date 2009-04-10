package com.ibm.wala.shrikeBT;

public interface IShiftInstruction extends IInstruction {

  public enum Operator implements IBinaryOpInstruction.IOperator {
    SHL,
    SHR,
    USHR;
  }

  Operator getOperator();
  
  String getType();
  
  boolean isUnsigned();
  
}
