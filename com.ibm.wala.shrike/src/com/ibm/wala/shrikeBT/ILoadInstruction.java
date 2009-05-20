package com.ibm.wala.shrikeBT;

public interface ILoadInstruction extends IInstruction, IMemoryOperation {

  int getVarIndex();

  String getType();

}
