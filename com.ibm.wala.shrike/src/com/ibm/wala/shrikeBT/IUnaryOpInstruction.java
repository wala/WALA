package com.ibm.wala.shrikeBT;

public interface IUnaryOpInstruction extends IInstruction {

  public interface IOperator {
  }

  public static enum Operator implements IOperator {
    NEG;
  
    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  IOperator getOperator();
  
  String getType();
}
