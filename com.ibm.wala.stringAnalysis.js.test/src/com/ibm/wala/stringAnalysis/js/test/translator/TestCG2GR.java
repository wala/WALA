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

import com.ibm.wala.automaton.grammar.string.ISimplify;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.js.translator.JSFunctionNameResolver;
import com.ibm.wala.stringAnalysis.js.translator.JSSSA2Rule;
import com.ibm.wala.stringAnalysis.translator.BB2GR;
import com.ibm.wala.stringAnalysis.translator.CG2GR;
import com.ibm.wala.stringAnalysis.translator.FunctionNameCalleeResolver;
import com.ibm.wala.stringAnalysis.translator.IR2GR;
import com.ibm.wala.stringAnalysis.translator.SSA2Rule;

public class TestCG2GR extends TestJSTranslatorBase {

    public void testTranslate() {
        SSA2Rule ssa2rule = new JSSSA2Rule();
        BB2GR bb2gr = new BB2GR(ssa2rule);
        IR2GR ir2gr = new IR2GR(bb2gr);
        CG2GR cg2gr = new CG2GR(ir2gr, new FunctionNameCalleeResolver(new JSFunctionNameResolver()));
        ISimplify g = cg2gr.translate(getCallGraphBuilder());
        assertTrue(g instanceof GR);
        GR gr = (GR) g;
        //Trace.println("--- GR:");
        //Trace.println(SAUtil.prettyFormat(gr));
    }
}
