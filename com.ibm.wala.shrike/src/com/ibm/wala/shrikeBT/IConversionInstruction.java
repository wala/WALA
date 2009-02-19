package com.ibm.wala.shrikeBT;

public interface IConversionInstruction extends IInstruction {

  String getFromType();
  
  String getToType();
  
}
