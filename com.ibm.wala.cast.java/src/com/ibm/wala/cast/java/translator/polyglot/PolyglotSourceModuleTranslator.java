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
 * Created on Oct 6, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.main.Options;
import polyglot.main.UsageError;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.cast.java.translator.*;
import com.ibm.wala.classLoader.DirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;

/**
 * A SourceModuleTranslator whose implementation of loadAllSources() uses the PolyglotFrontEnd
 * pseudo-compiler to generate DOMO IR for the sources in the compile-time classpath.
 * @author rfuhrer
 */
public class PolyglotSourceModuleTranslator implements SourceModuleTranslator {
    private final ExtensionInfo fExtInfo;

    private String fClassPath;

    public PolyglotSourceModuleTranslator(AnalysisScope scope, IRTranslatorExtension extInfo, PolyglotSourceLoaderImpl sourceLoader) {
	fExtInfo= (ExtensionInfo) extInfo;
	computeClassPath(scope);
	extInfo.setSourceLoader(sourceLoader);	
    }

    private void computeClassPath(AnalysisScope scope) {
	StringBuffer buf= new StringBuffer();

	ClassLoaderReference cl= scope.getApplicationLoader();

	while (cl != null) {
	    Set/* <Module> */modules= scope.getModules(cl);

	    for(Iterator iter= modules.iterator(); iter.hasNext();) {
		Module m= (Module) iter.next();

		if (buf.length() > 0)
		    buf.append(File.pathSeparator);
		if (m instanceof JarFileModule) {
		    JarFileModule jarFileModule= (JarFileModule) m;

		    buf.append(jarFileModule.getAbsolutePath());
		} else if (m instanceof DirectoryTreeModule) {
		    DirectoryTreeModule directoryTreeModule= (DirectoryTreeModule) m;

		    buf.append(directoryTreeModule.getPath());
		} else
		    Assertions.UNREACHABLE("Module entry is neither jar file nor directory");
	    }
	    cl= cl.getParent();
	}
	fClassPath= buf.toString();
    }

    public void loadAllSources(Set modules) {
	Options opts= fExtInfo.getOptions();
	opts.assertions = true;
	Options.global = opts;
	try {
	    opts.parseCommandLine(new String[] { "-cp", fClassPath }, new HashSet());
	} catch (UsageError e) {
	    // Assertions.UNREACHABLE("Error parsing classpath spec???");
	}

	Compiler compiler= new PolyglotFrontEnd(fExtInfo);
	List/*<SourceStream>*/ streams= new ArrayList();

	// N.B.: 'modules' is a flat set of source file ModuleEntry's.
	for(Iterator it= modules.iterator(); it.hasNext(); ) {
	    SourceFileModule entry= (SourceFileModule) it.next();

	    Assertions._assert(entry.isSourceFile());

	    String filePath= entry.getAbsolutePath();

	    try {
		StreamSource srcStream= new StreamSource(entry.getInputStream(), filePath);

		streams.add(srcStream);
	    } catch (IOException e) {
		compiler.errorQueue().enqueue(new ErrorInfo(ErrorInfo.IO_ERROR, "Unable to open source file '" + entry.getName() + "'", Position.COMPILER_GENERATED));
	    }
	}
	compiler.compile(streams);
	// At this point, DOMO now "knows" about all the source-originated stuff
    }
}
