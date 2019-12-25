/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
 * Copyright (c) 2009-2012,
 *
 * <p>Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>, Adam
 * Foltzer <acfoltzer@galois.com>) Steve Suh <suhsteve@gmail.com>
 *
 * <p>All rights reserved.
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. The names of the contributors may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.scandroid.util;

import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import java.net.URI;

/**
 * @author acfoltzer
 *     <p>An abstraction of the options for a SCanDroid execution
 */
public interface ISCanDroidOptions {

  /** @return whether to create a full call graph pdf */
  public boolean pdfCG();

  /** @return whether to create an application-only call graph pdf */
  public boolean pdfPartialCG();

  /** @return whether to create a call graph of application + 1 level of system calls */
  public boolean pdfOneLevelCG();

  /** @return whether to create a system + 1 level of application call graph */
  public boolean systemToApkCG();

  /** @return whether to print a full call graph to stdout */
  public boolean stdoutCG();

  /** @return whether to include the Android library in flow analysis */
  public boolean includeLibrary();

  /** @return whether to analyze each entry point separately */
  public boolean separateEntries();

  /** @return whether to bring up a GUI to analyze domain elements for flow analysis */
  public boolean ifdsExplorer();

  /** @return whether to look for main methods and add them as entry points */
  public boolean addMainEntrypoints();

  /** @return whether to use ServerThread.run as the entry point for analysis */
  public boolean useThreadRunMain();

  /** @return whether to run string prefix analysis */
  public boolean stringPrefixAnalysis();

  /** @return whether to stop after generating the call graph */
  public boolean testCGBuilder();

  /** @return whether to log class hierarchy warnings */
  public boolean classHierarchyWarnings();

  /** @return whether to log call graph builder warnings */
  public boolean cgBuilderWarnings();

  /** @return whether to check conformance to built-in policy */
  public boolean useDefaultPolicy();

  /** @return the URI pointing to the jar or apk to analyze */
  public URI getClasspath();

  /** @return the filename portion of the classpath to analyze */
  public String getFilename();

  /** @return a URI to the Android library jar */
  public URI getAndroidLibrary();

  /** @return the ReflectionOptions for this run */
  public ReflectionOptions getReflectionOptions();

  /** @return a URI to the XML method summaries file */
  public URI getSummariesURI();
}
