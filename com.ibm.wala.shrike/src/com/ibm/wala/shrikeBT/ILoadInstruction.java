package com.ibm.wala.shrikeBT;

public interface ILoadInstruction extends IInstruction {

  int getVarIndex();
  
  String getType();
  
}
