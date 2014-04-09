package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.ir.translator.AstTranslator.WalkContext;
import com.ibm.wala.cast.tree.CAstNode;

public interface ArrayOpHandler {
    void doArrayRead(WalkContext context, int result, int arrayValue, CAstNode arrayRef, int[] dimValues);

    void doArrayWrite(WalkContext context, int arrayValue, CAstNode arrayRef, int[] dimValues, int rval);
}
