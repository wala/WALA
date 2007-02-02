/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.stringAnalysis.js.translator;

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.stringAnalysis.translator.*;
import com.ibm.wala.stringAnalysis.translator.repository.CharAt;
import com.ibm.wala.stringAnalysis.translator.repository.Concatenate;
import com.ibm.wala.stringAnalysis.translator.repository.Split;
import com.ibm.wala.stringAnalysis.translator.repository.Substr;
import com.ibm.wala.stringAnalysis.translator.repository.Substring;
import com.ibm.wala.stringAnalysis.translator.repository.ToLocaleLowerCase;
import com.ibm.wala.stringAnalysis.translator.repository.ToLocaleUpperCase;
import com.ibm.wala.stringAnalysis.translator.repository.ToLowerCase;
import com.ibm.wala.stringAnalysis.translator.repository.ToUpperCase;
import com.ibm.wala.stringAnalysis.translator.repository.TranslatorRepository;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.Trace;

public class JSTranslatorRepository extends TranslatorRepository {
    public JSTranslatorRepository() {
        // Global
        translatorMap.put("eval", UNSUPPORTED_TRANSLATOR); // eval(x), ES1
        translatorMap.put("parseInt", UNSUPPORTED_TRANSLATOR); // parseInt(string [, radix]) ES1
        translatorMap.put("parseFloat", UNSUPPORTED_TRANSLATOR); // parseFloat(string) ES1
        translatorMap.put("isNaN", UNSUPPORTED_TRANSLATOR); // isNaN(number) ES1
        translatorMap.put("isFinite", UNSUPPORTED_TRANSLATOR); // isFinite(number) ES1
        translatorMap.put("decodeURI", UNSUPPORTED_TRANSLATOR); // decodeURI(encodedURI) ES3
        translatorMap.put("decodeURIComponent", UNSUPPORTED_TRANSLATOR); // decodeURIComponent(x) ES3
        translatorMap.put("encodeURI", UNSUPPORTED_TRANSLATOR); // encodeURI(uri) ES3
        translatorMap.put("encodeURIComponent", UNSUPPORTED_TRANSLATOR); // encodeURIComponent(x) ES3
        translatorMap.put("escape", UNSUPPORTED_TRANSLATOR); // ES1
        translatorMap.put("unescape", UNSUPPORTED_TRANSLATOR); // ES1
        
        translatorMap.put("write", new Concatenate()); // $document.write

        // String Object
        translatorMap.put("op(add)", new Concatenate());
        translatorMap.put("substr", new Substr()); // ES1
        translatorMap.put("substring", new Substring()); // ES1
        translatorMap.put("toUpperCase", new ToUpperCase()); // ES1
        translatorMap.put("toLowerCase", new ToLowerCase()); // ES1
        translatorMap.put("toLocaleUpperCase", new ToLocaleUpperCase()); // ES3
        translatorMap.put("toLocaleLowerCase", new ToLocaleLowerCase()); // ES3
        translatorMap.put("concat", new Concatenate()); // ES3
        translatorMap.put("charAt", new CharAt()); // ES1
        translatorMap.put("split", new Split()); // ES1
        
        translatorMap.put("charCodeAt", UNSUPPORTED_TRANSLATOR); // ES1
        translatorMap.put("indexOf", UNSUPPORTED_TRANSLATOR); // ES1
        translatorMap.put("lastIndexOf", UNSUPPORTED_TRANSLATOR); // ES1
        
        translatorMap.put("localeCompare", UNSUPPORTED_TRANSLATOR); // ES3
        translatorMap.put("match", UNSUPPORTED_TRANSLATOR); // ES3
        translatorMap.put("replace", UNSUPPORTED_TRANSLATOR); // ES3
        translatorMap.put("search", UNSUPPORTED_TRANSLATOR); // ES3
        translatorMap.put("slice", UNSUPPORTED_TRANSLATOR); // ES3
        
        translatorMap.put("anchor", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("link", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("fontcolor", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("fontsize", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("big", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("blink", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("bold", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("fixed", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("italics", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("small", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("strike", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("sub", UNSUPPORTED_TRANSLATOR);
        translatorMap.put("sup", UNSUPPORTED_TRANSLATOR);
    }
}
