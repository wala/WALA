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
package com.ibm.wala.stringAnalysis.js.test.translator;

import java.io.IOException;
import java.net.URL;

import com.ibm.wala.cast.js.test.Util;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.stringAnalysis.test.translator.TestTranslatorBase;
import com.ibm.wala.util.debug.Assertions;

abstract public class TestJSTranslatorBase extends TestTranslatorBase {
    static private ClassLoader loader =
        TestJSTranslatorBase.class.getClassLoader();

    protected PropagationCallGraphBuilder makeCallGraphBuilder() {
        return makeCallGraphBuilder("test.js");
    }
    
    protected PropagationCallGraphBuilder makeCallGraphBuilder(String testFile) {
        try {
            URL url = loader.getResource("com/ibm/wala/stringAnalysis/js/test/example/" + testFile);
            PropagationCallGraphBuilder builder = 
                Util.makeCGBuilder(
                        new SourceFileModule[]{ 
                                Util.makeSourceModule(url, testFile) });
            builder.makeCallGraph( builder.getOptions() );
            return builder;
        } catch (IOException e) {
            Assertions.UNREACHABLE();
            return null;
        }
    }
    
}
