package com.ibm.wala.shrikeBT;

public interface IComparisonInstruction extends IInstruction {

  public enum Operator {
    CMP,
    CMPL,
    CMPG;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  Operator getOperator();
  
  String getType();
  
}