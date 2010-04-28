package com.ibm.wala.shrikeBT;

public interface IInstanceofInstruction extends IInstruction {

  boolean firstClassType();
  
  String getType();
  
}
