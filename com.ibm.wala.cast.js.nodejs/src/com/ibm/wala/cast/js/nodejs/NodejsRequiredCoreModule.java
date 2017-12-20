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
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.io.TemporaryFile;

/**
 * @author Brian Pfretzschner &lt;brian.pfretzschner@gmail.com&gt;
 */
public class NodejsRequiredCoreModule extends NodejsRequiredSourceModule {

	/**
	 * Core modules list for Nodejs v6.2.2
	 * https://github.com/nodejs/node/blob/v6.2.2/lib/internal/module.js 
	 */
	private static final Set<String> CORE_MODULES = HashSetFactory.make(Arrays.asList(
			"assert", "buffer", "child_process", "cluster", "crypto", "dgram", "dns", "domain", "events", "fs", "http",
			"https", "net", "os", "path", "punycode", "querystring", "readline", "repl", "stream", "string_decoder",
			"tls", "tty", "url", "util", "v8", "vm", "zlib",
			
			// Non-public files
			"timers", "constants", "freelist", "smalloc", 
			"_debugger", "_http_agent", "_http_client", "_http_common", "_http_incoming", "_http_outgoing",
			"_http_server", "_linklist", "_stream_duplex", "_stream_passthrough", "_stream_readable",
			"_stream_transform", "_stream_writable", "_tls_common", "_tls_legacy", "_tls_wrap"));
	
	protected NodejsRequiredCoreModule(File f, SourceFileModule clonedFrom) throws IOException {
		super(FilenameUtils.getBaseName(f.getName()), f, clonedFrom);
	}

	private static InputStream getModule(String name) {
	  return NodejsRequiredCoreModule.class.getClassLoader().getResourceAsStream("core-modules/" + name + ".js");

	}
	public static NodejsRequiredCoreModule make(String name) throws IOException {
		File file = new File(System.getProperty("java.io.tmpdir"), name+".js");
		TemporaryFile.streamToFile(file, getModule(name));
		SourceFileModule sourceFileModule = CAstCallGraphUtil.makeSourceModule(file.toURI().toURL(), file.getName());
		return new NodejsRequiredCoreModule(file, sourceFileModule);
	}
	
	public static boolean isCoreModule(String name) {
		return CORE_MODULES.contains(name);
	}

}
