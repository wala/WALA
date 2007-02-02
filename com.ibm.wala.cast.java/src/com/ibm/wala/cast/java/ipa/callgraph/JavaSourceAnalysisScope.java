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
/*
 * Created on Sep 27, 2005
 */
package com.ibm.wala.cast.java.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.Atom;

public class JavaSourceAnalysisScope extends AnalysisScope {

    public static final Atom SOURCE= Atom.findOrCreateUnicodeAtom("Source");

    public static final ClassLoaderReference SOURCE_REF= new ClassLoaderReference(SOURCE);

    public JavaSourceAnalysisScope() {
	SOURCE_REF.setParent(getLoader(APPLICATION));
	getLoader(SYNTHETIC).setParent(SOURCE_REF);

	loadersByName.put(SOURCE, SOURCE_REF);

	setLoaderImpl(getLoader(SYNTHETIC), "com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader");
	setLoaderImpl(SOURCE_REF, "com.ibm.domo.ast.java.translator.polyglot.PolyglotSourceLoaderImpl");
    }

    public ClassLoaderReference getSourceLoader() {
	return getLoader(SOURCE);
    }
}
