package com.ibm.wala.cast.js.translator;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.classLoader.SourceModule;

public class CAstRhinoTranslatorFactory implements JavaScriptTranslatorFactory {

	public TranslatorToCAst make(CAst ast, SourceModule M) {
		return new CAstRhinoTranslator(M);
	}
}

