package com.ibm.wala.cast.java.ssa;

import com.ibm.wala.cast.ir.ssa.AstInstructionFactory;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.types.TypeReference;

public interface AstJavaInstructionFactory extends AstInstructionFactory {

  AstJavaInvokeInstruction JavaInvokeInstruction(int iindex, int result, int[] params, int exception, CallSiteReference site);
  
  AstJavaInvokeInstruction JavaInvokeInstruction(int iindex, int[] params, int exception, CallSiteReference site);
  
  AstJavaInvokeInstruction JavaInvokeInstruction(int iindex, int results[], int[] params, int exception, CallSiteReference site, Access[] lexicalReads, Access[] lexicalWrites);
 
  EnclosingObjectReference EnclosingObjectReference(int iidnex, int lval, TypeReference type);
  
  AstJavaNewEnclosingInstruction JavaNewEnclosingInstruction(int iindex, int result, NewSiteReference site, int enclosing);
  
}
