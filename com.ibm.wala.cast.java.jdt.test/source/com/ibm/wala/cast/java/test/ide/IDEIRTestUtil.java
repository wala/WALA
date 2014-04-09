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
 * Created on Oct 3, 2005
 */
package com.ibm.wala.cast.java.test.ide;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.classLoader.JarFileModule;

public class IDEIRTestUtil {

	public static void populateScope(String projectName, JDTJavaSourceAnalysisEngine engine, Collection<String> sources, List<String> libs) throws IOException {

	  boolean foundLib = false;
		for (String lib : libs) {
			File libFile = new File(lib);
			if (libFile.exists()) {
				foundLib = true;
				engine.addSystemModule(new JarFileModule(new JarFile(libFile)));
			}
		}
		assert foundLib : "couldn't find library file from " + libs;

		for (String srcFilePath : sources) {
		  engine.addSourceModule(srcFilePath);		  
		}
	}
}
