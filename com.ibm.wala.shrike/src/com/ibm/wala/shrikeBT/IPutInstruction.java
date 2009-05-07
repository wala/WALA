package com.ibm.wala.shrikeBT;

public interface IPutInstruction extends IInstruction {

  public String getClassType();

  public String getFieldType();

  public String getFieldName();

  public boolean isStatic();

}
