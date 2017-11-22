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
package com.ibm.wala.cast.js.html;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.function.Supplier;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.util.collections.Pair;

public class WebUtil { 

  public static final String preamble = "preamble.js";

  private static IHtmlParserFactory factory = JerichoHtmlParser::new;
  
  public static void setFactory(IHtmlParserFactory factory) {
    WebUtil.factory = factory;
  }

  /**
   * 
   * @param url
   * @return a pair (S,F), where S is a set of extracted sources, and F is the
   *         temp file holding the combined sources (or <code>null</code> if no
   *         such file exists)
   * @throws Error
   */
  public static Pair<Set<MappedSourceModule>,File> extractScriptFromHTML(URL url, Supplier<JSSourceExtractor> fSourceExtractor) throws Error {
    try {
      JSSourceExtractor extractor = fSourceExtractor.get();
      Set<MappedSourceModule> sources = extractor.extractSources(url, factory.getParser(), new IdentityUrlResolver());
      return Pair.make(sources, extractor.getTempFile());
    } catch (IOException e) {
      throw new RuntimeException("trouble with " + url, e);
    }
  }
  
  public static void main(String[] args) throws MalformedURLException, Error {
    System.err.println(extractScriptFromHTML(new URL(args[0]), Boolean.parseBoolean(args[1])? DefaultSourceExtractor.factory: DomLessSourceExtractor.factory));
  }

  public static Reader getStream(URL url) throws IOException {
    URLConnection conn = url.openConnection();
    conn.setDefaultUseCaches(false);
    conn.setUseCaches(false);
  
    return new InputStreamReader(conn.getInputStream());
  }
}
	
      
