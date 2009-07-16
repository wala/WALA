package com.ibm.wala.cast.js.translator;

import java.net.URL;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.classLoader.ModuleEntry;

public class CAstRhinoTranslatorFactory implements JavaScriptTranslatorFactory {

	public TranslatorToCAst make(CAst ast, ModuleEntry M, URL sourceURL, String localFileName) {
		return new CAstRhinoTranslator(M);
	}
}

