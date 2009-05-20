package com.ibm.wala.shrikeBT;

public interface IGetInstruction extends IInstruction, IMemoryOperation {

  public String getClassType();

  public String getFieldName();

  public String getFieldType();

  public boolean isStatic();
  
}
