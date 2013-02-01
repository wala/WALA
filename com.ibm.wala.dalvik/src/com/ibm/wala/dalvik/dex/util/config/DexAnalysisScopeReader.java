/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Jonathan Bardin     <astrosus@gmail.com>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.dex.util.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.debug.Assertions;

/**
 * Create AnalysisScope from java & dalvik file.
 * 
 * @see com.ibm.wala.ipa.callgraph.AnalysisScope
 */
public class DexAnalysisScopeReader extends AnalysisScopeReader {

	private static final ClassLoader WALA_CLASSLOADER = AnalysisScopeReader.class
			.getClassLoader();

	private static final String BASIC_FILE = /**"conf" + File.separator
			+ */"primordial.txt";

	
	
	public static AnalysisScope makeAndroidBinaryAnalysisScope(String classPath, String exclusions) throws IOException {
		if (classPath == null) {
			throw new IllegalArgumentException("classPath null");
		}
		
		AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
		scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(exclusions.getBytes())));
		ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
		addClassPathToScope(classPath, scope, loader);
		return scope;
	}
	/**
	 * @param classPath
	 *            class path to analyze, delimited by File.pathSeparator
	 * @param exclusionsFile
	 *            file holding class hierarchy exclusions. may be null
	 * @throws IOException
	 * @throws IllegalStateException
	 *             if there are problems reading wala properties
	 */
	public static AnalysisScope makeAndroidBinaryAnalysisScope(
			String classPath, File exclusionsFile) throws IOException {
		if (classPath == null) {
			throw new IllegalArgumentException("classPath null");
		}
		AnalysisScope scope = AnalysisScopeReader.readJavaScope(BASIC_FILE,
				exclusionsFile, WALA_CLASSLOADER);
		ClassLoaderReference loader = scope
				.getLoader(AnalysisScope.APPLICATION);
		addClassPathToScope(classPath, scope, loader);
		return scope;
	}

	public static AnalysisScope makeAndroidBinaryAnalysisScope(
			JarFile classPath, File exclusionsFile) throws IOException {
		if (classPath == null) {
			throw new IllegalArgumentException("classPath null");
		}
		AnalysisScope scope = AnalysisScopeReader.readJavaScope(BASIC_FILE,
				exclusionsFile, WALA_CLASSLOADER);
		ClassLoaderReference loader = scope
				.getLoader(AnalysisScope.APPLICATION);
		scope.addToScope(loader, classPath);
		return scope;
	}

	public static AnalysisScope makeAndroidBinaryAnalysisScope(URI classPath,
			File exclusionsFile) throws IOException {
		if (classPath == null) {
			throw new IllegalArgumentException("classPath null");
		}
		AnalysisScope scope = AnalysisScopeReader.readJavaScope(BASIC_FILE,
				exclusionsFile, WALA_CLASSLOADER);
		ClassLoaderReference loader = scope
				.getLoader(AnalysisScope.APPLICATION);

		final String path = classPath.getPath();
		if (path.endsWith(".jar")) {
			scope.addToScope(loader, new JarFile(new File(classPath)));
		} else if (path.endsWith(".apk") || path.endsWith(".dex")) {
			scope.addToScope(loader, new DexFileModule(new File(classPath)));
		} else {
			throw new IOException(
					"could not determine type of classpath from file extension: "
							+ path);
		}

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

			for (int i = 0; i < paths.length; i++) {
				if (paths[i].endsWith(".jar")) { // handle jar file
					scope.addToScope(loader, new JarFile(paths[i]));
				} else if (paths[i].endsWith(".apk")
						|| paths[i].endsWith(".dex")) { // Handle android file.
					File f = new File(paths[i]);
					scope.addToScope(loader, new DexFileModule(f));
				} else {
					File f = new File(paths[i]);
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
									"Invalid class file");
						}
					}
				}
			}

		} catch (IOException e) {
			Assertions.UNREACHABLE(e.toString());
		}
	}
}
