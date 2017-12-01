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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;

public class AndroidAnalysisScope {

	private static final String BASIC_FILE = "primordial.txt";

	public static AnalysisScope setUpAndroidAnalysisScope(URI classpath, String exclusions, ClassLoader loader, URI... androidLib) throws IOException {
		AnalysisScope scope;
		if (androidLib == null || androidLib.length == 0) {
			scope = AnalysisScopeReader.readJavaScope(BASIC_FILE, new File(exclusions), loader);
		} else {
			scope = AnalysisScope.createJavaAnalysisScope();

			File exclusionsFile = new File(exclusions);
	        try (final InputStream fs = exclusionsFile.exists()? new FileInputStream(exclusionsFile): FileProvider.class.getClassLoader().getResourceAsStream(exclusionsFile.getName())) {
	        	scope.setExclusions(new FileOfClasses(fs));
	        }
	        
			scope.setLoaderImpl(ClassLoaderReference.Primordial,
					"com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

			for(URI al : androidLib) {
				try {
					scope.addToScope(ClassLoaderReference.Primordial, DexFileModule.make(new File(al)));
				} catch (Exception e) {
					e.printStackTrace();
					try (final JarFile jar = new JarFile(new File(al))) {
						scope.addToScope(ClassLoaderReference.Primordial, new JarFileModule(jar));
					}
				}
			}

		}

		scope.setLoaderImpl(ClassLoaderReference.Application,
				"com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

		scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(new File(classpath)));
		
		return scope;
	}
	
	/**
	 * Handle .apk file.
	 * 
	 * @param classPath
	 * @param scope
	 * @param loader
	 */
	public static void addClassPathToScope(String classPath,
			AnalysisScope scope, ClassLoaderReference loader) {
		if (classPath == null) {
			throw new IllegalArgumentException("null classPath");
		}
		try {
			String[] paths = classPath.split(File.pathSeparator);

			for (String path : paths) {
				if (path.endsWith(".jar")
						|| path.endsWith(".apk")
						|| path.endsWith(".dex")) { // Handle android file.
					File f = new File(path);
					scope.addToScope(loader, DexFileModule.make(f));
				} else {
					File f = new File(path);
					if (f.isDirectory()) { // handle directory FIXME not working
											// for .dex and .apk files into that
											// directory
						scope.addToScope(loader, new BinaryDirectoryTreeModule(
								f));
					} else { // handle java class file.
						try {
							scope.addClassFileToScope(loader, f);
						} catch (InvalidClassFileException e) {
							throw new IllegalArgumentException(
									"Invalid class file", e);
						}
					}
				}
			}

		} catch (IOException e) {
			Assertions.UNREACHABLE(e.toString());
		}
	}
}
