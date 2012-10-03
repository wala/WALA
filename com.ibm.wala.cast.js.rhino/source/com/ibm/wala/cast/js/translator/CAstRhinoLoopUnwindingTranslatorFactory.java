package com.ibm.wala.cast.js.translator;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.js.translator.JavaScriptLoopUnwindingTranslatorFactory;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.classLoader.SourceModule;

public class CAstRhinoLoopUnwindingTranslatorFactory 
  extends JavaScriptLoopUnwindingTranslatorFactory 
{
  public CAstRhinoLoopUnwindingTranslatorFactory(int unwindFactor) {
    super(unwindFactor);
  }

  public CAstRhinoLoopUnwindingTranslatorFactory() {
    this(3);
  }

  @Override
  protected TranslatorToCAst translateInternal(CAst Ast, SourceModule M, String N) {
    return new CAstRhinoTranslator(M, true);
  }
}

