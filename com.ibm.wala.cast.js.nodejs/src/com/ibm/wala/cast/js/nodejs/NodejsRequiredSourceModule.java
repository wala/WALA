/******************************************************************************
 * Copyright (c) 2002 - 2016 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Brian Pfretzschner - initial implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.nodejs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.Streams;

/**
 * This class is intended to be used whenever a JavaScript module is dynamically
 * required by JavaScript (CommonJS). The required file will be loaded and
 * wrapped in a function call to simulate the real behavior of a CommonJS
 * environment. The resulting function will be named GLOBAL_PREFIX + relative
 * file-name. To retrieve the final function name, use getFunctioName().
 * 
 * @author Brian Pfretzschner &lt;brian.pfretzschner@gmail.com&gt;
 */
public class NodejsRequiredSourceModule extends SourceFileModule {

	private final static String MODULE_WRAPPER_FILENAME = "module-wrapper.js";
	private final static String JSON_WRAPPER_FILENAME = "json-wrapper.js";
	
	private final static String FILENAME_PLACEHOLDER = "/*/ WALA-INSERT-FILENAME-HERE /*/";
	private final static String DIRNAME_PLACEHOLDER = "/*/ WALA-INSERT-DIRNAME-HERE /*/";
	private final static String CODE_PLACEHOLDER = "/*/ WALA-INSERT-CODE-HERE /*/";

	private static String MODULE_WRAPPER_SOURCE = null;
	private static String JSON_WRAPPER_SOURCE = null;

	private final String className;

	/**
	 * @param f
	 *            Must be a file located below folder workingDir.
	 * @param clonedFrom
	 * @throws IOException
	 */
	protected NodejsRequiredSourceModule(String className, File f, SourceFileModule clonedFrom) throws IOException {
		super(f, clonedFrom);

		// Generate className based on the given file name
		this.className = className;
		assert className.matches("[a-zA-Z_$][0-9a-zA-Z_$]*") : "Invalid className: " + className;

		if (MODULE_WRAPPER_SOURCE == null || JSON_WRAPPER_SOURCE == null) {
			// Populate the cache that hold the module wrapper source code
			loadWrapperSources();
		}
	}

	@Override
	public InputStream getInputStream() {
		String moduleSource = null;
		try (final InputStream inputStream = super.getInputStream()) {
			moduleSource = IOUtils.toString(inputStream);
		} catch (IOException e) {
			Assertions.UNREACHABLE(e.getMessage());
		}
		
		String wrapperSource = null;
		String ext = FilenameUtils.getExtension(getFile().toString()).toLowerCase();
		if (ext.equals("js")) {
			// JS file -> use module wrapper
			wrapperSource = MODULE_WRAPPER_SOURCE;
		}
		else if (ext.equals("json")) {
			// JSON file -> use JSON wrapper
			wrapperSource = JSON_WRAPPER_SOURCE;
		}
		else {
			// No clue -> try module wrapper
			System.err.println("NodejsRequiredSourceModule: Unsupported file type ("+ext+"), continue anyway.");
			wrapperSource = MODULE_WRAPPER_SOURCE;
		}

		String wrappedModuleSource = wrapperSource
				.replace(FILENAME_PLACEHOLDER, getFile().getName())
				.replace(DIRNAME_PLACEHOLDER, getFile().getParent().toString())
				.replace(CODE_PLACEHOLDER, moduleSource);

		return IOUtils.toInputStream(wrappedModuleSource);
	}

	@Override
	public String getClassName() {
		return className;
	}

	 @Override
	  public String getName() {
	    return className;
	  }

	private static void loadWrapperSources() throws IOException {
		MODULE_WRAPPER_SOURCE = loadWrapperSource(MODULE_WRAPPER_FILENAME);
		JSON_WRAPPER_SOURCE = loadWrapperSource(JSON_WRAPPER_FILENAME);
	}
	
	private static String loadWrapperSource(String filename) throws IOException {
		try (final InputStream url = NodejsRequiredSourceModule.class.getClassLoader().getResourceAsStream(filename)) {
		  return new String(Streams.inputStream2ByteArray(url));
		}
	}

	/**
	 * Generate a className based on the file name and path of the module file.
	 * The path should be encoded in the className since the file name is not unique.
	 * 
	 * @param rootDir
	 * @param file
	 */
	public static String convertFileToClassName(File rootDir, File file) {
		URI normalizedWorkingDirURI = rootDir.getAbsoluteFile().toURI().normalize();
		URI normalizedFileURI = file.getAbsoluteFile().toURI().normalize();
		String relativePath = normalizedWorkingDirURI.relativize(normalizedFileURI).getPath();
		
		return FilenameUtils.removeExtension(relativePath)
				.replace("/", "_")
				.replace("-", "__")
				.replace(".", "__");
	}
	
	public static NodejsRequiredSourceModule make(File rootDir, File file) throws IOException {
		String className = convertFileToClassName(rootDir, file);
		SourceFileModule sourceFileModule = CAstCallGraphUtil.makeSourceModule(file.toURI().toURL(), file.getName());
		return new NodejsRequiredSourceModule(className, file, sourceFileModule);
	}
}
