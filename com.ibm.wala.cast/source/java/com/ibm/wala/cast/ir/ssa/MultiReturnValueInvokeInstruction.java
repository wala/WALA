package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

abstract class MultiReturnValueInvokeInstruction
    extends SSAAbstractInvokeInstruction
{
  protected final int results[];

  protected MultiReturnValueInvokeInstruction(int results[], int exception, CallSiteReference site) {
    super(exception, site);
    this.results = results;
  }

  public int getNumberOfReturnValues() {
    return results==null? 0: results.length;
  }

  public int getReturnValue(int i) {
    return results[i];
  }

}
