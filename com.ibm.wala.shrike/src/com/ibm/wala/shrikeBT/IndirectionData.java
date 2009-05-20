package com.ibm.wala.shrikeBT;

public interface IndirectionData {

  int[] indirectlyReadLocals(int instructionIndex);
  
  int[] indirectlyWrittenLocals(int instructionIndex);
  
}
