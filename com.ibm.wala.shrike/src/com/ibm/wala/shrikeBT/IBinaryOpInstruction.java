package com.ibm.wala.shrikeBT;

public interface IBinaryOpInstruction extends IInstruction {

  public interface IOperator {
  }

  IBinaryOpInstruction.IOperator getOperator();

  String getType();

  public enum Operator implements IBinaryOpInstruction.IOperator {
    ADD, SUB, MUL, DIV, REM, AND, OR, XOR;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  public boolean throwsExceptionOnOverflow();

  public boolean isUnsigned();

}
