package com.ibm.wala.shrikeBT;

public interface ITypeTestInstruction extends IInstruction {

  boolean firstClassTypes();
  
  String[] getTypes();
  
}
