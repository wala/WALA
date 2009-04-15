package com.ibm.wala.cast.ir.translator;

import java.io.IOException;

import com.ibm.wala.cast.tree.CAstEntity;

public interface TranslatorToCAst {

  public CAstEntity translateToCAst() throws IOException;

}
