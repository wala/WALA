package com.ibm.wala.shrikeBT;

public interface IConditionalBranchInstruction extends IInstruction {

  public interface IOperator {
  }

  public enum Operator implements IConditionalBranchInstruction.IOperator {
    EQ, NE, LT, GE, GT, LE;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  int getTarget();

  IOperator getOperator();

  String getType();
}
