/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
package com.ibm.wala.dalvik.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.dex.util.config.DexAnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.io.FileSuffixes;

public class AndroidAnalysisScope {
			

/** BEGIN Custom change: Fixes in AndroidAnalysisScope */    
    public static AnalysisScope setUpAndroidAnalysisScope(String androidLib, String classpath, String exclusions) throws IOException {
		AnalysisScope scope;
		if (androidLib == null) {
			scope = DexAnalysisScopeReader.makeTestAndroidBinaryAnalysisScope(classpath, exclusions);
		} else {
			scope = DexAnalysisScopeReader.makeAndroidBinaryAnalysisScope(classpath, exclusions);
		}
        setUpAnalysisScope(scope, androidLib==null? null: new File(androidLib).toURI());
		return scope;
	}
/** END Custom change: Fixes in AndroidAnalysisScope */    

	public static AnalysisScope setUpAndroidAnalysisScope(URI androidLib, URI classpath, String exclusions) throws IOException {
		AnalysisScope scope = DexAnalysisScopeReader.makeAndroidBinaryAnalysisScope(classpath, exclusions);
        setUpAnalysisScope(scope, androidLib);
		return scope;
	}
	
	private static void setUpAnalysisScope(AnalysisScope scope, URI androidLib) throws IOException {

		scope.setLoaderImpl(ClassLoaderReference.Application,
				"com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

        if (androidLib != null) {
        	if (FileSuffixes.isDexFile(androidLib)) {
        
/** END Custom change: Fixes in AndroidAnalysisScope */            
			Module dexMod = new DexFileModule(new File(androidLib));
			
//			Iterator<ModuleEntry> mitr = dexMod.getEntries();
//			while (mitr.hasNext()) {
//				ModuleEntry moduleEntry = (ModuleEntry) mitr.next();
//				logger.error("dex module: {}", moduleEntry.getName());
//			}

			scope.setLoaderImpl(ClassLoaderReference.Primordial,
					"com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

			scope.addToScope(ClassLoaderReference.Primordial, dexMod);
		} else {
/** BEGIN Custom change: Fixes in AndroidAnalysisScope */            
            if (FileSuffixes.isRessourceFromJar(androidLib)) {
            	scope.setLoaderImpl(ClassLoaderReference.Primordial,
        				"com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

                //final FileProvider fileProvider = new FileProvider();
                final InputStream is = androidLib.toURL().openStream();
                assert (is != null);
                final Module libMod = new JarStreamModule(new JarInputStream(is));
                scope.addToScope(ClassLoaderReference.Primordial, libMod);
                //throw new UnsupportedOperationException("Cannot extract lib from jar");

            } else {
            	// assume it is really a JVML jar file, not Android at all
    			scope.addToScope(ClassLoaderReference.Primordial, new JarFile(new File(
	    			androidLib)));
            }
/** END Custom change: Fixes in AndroidAnalysisScope */            
		}
	}
	}
    
}
