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
package com.ibm.wala.cast.js.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

public class WebUtil {
  private static final String outputDir;
  private static final Generator defaultGenerator = new Generator();

  static {
    String dir = System.getProperty("java.io.tmpdir");
    if (! dir.endsWith(File.separator))
      dir = dir + File.separator;
    
    outputDir = dir;
  }
  
  public static SourceFileModule extractScriptFromHTML(String url) {
    try {
      if (! url.startsWith("file://")) {
        url = "file://" + url;
      }
      return extractScriptFromHTML(new URL(url), defaultGenerator);
    } catch (MalformedURLException e) {
      Assertions.UNREACHABLE( e.toString() );
      return null;
    }
  }
  
  public static SourceFileModule extractScriptFromHTML(URL url) {
    return extractScriptFromHTML(url, defaultGenerator);
  }
  
  public static SourceFileModule extractScriptFromHTML(URL url, Generator generator) {
    try {
      String urlFile = url.getFile();
      String urlName = urlFile.substring(urlFile.lastIndexOf('/'));
      File F = new File(outputDir + urlName);
      Trace.println("making driver at " + F + " " + outputDir);
      if (F.exists()) F.delete();
      
      generator.generate(url, F);

      return new SourceFileModule(F, urlName.substring(1));

    } catch (IOException e) {
      Assertions.UNREACHABLE("error processing " + url + ": " + e);
      return null;
    }
  }
}
	
      
