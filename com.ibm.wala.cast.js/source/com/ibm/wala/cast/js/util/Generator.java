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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.IHtmlCallback;
import com.ibm.wala.cast.js.html.jericho.HTMLJerichoParser;

public class Generator {
  public static final String preamble = "preamble.js", temp1 = "temp1.js", temp2 = "temp2.js", temp3 = "temp3.js";
  
  public static interface CallbackFactory {
    IHtmlCallback createCallback(URL input, FileWriter domTreeFile, FileWriter embeddedScriptFile, FileWriter entrypointFile);
  }
  
  public static class HTMLCallbackFactory implements CallbackFactory {
    public IHtmlCallback createCallback(URL input, FileWriter domTreeFile, FileWriter embeddedScriptFile, FileWriter entrypointFile) {
      return new HTMLCallback(input, domTreeFile, embeddedScriptFile, entrypointFile);
    }
  }
  
  public static final CallbackFactory defaultCallbackFactory = new HTMLCallbackFactory();
  
  private CallbackFactory callbackFactory;
  private boolean ignoreCharset;
  
  public Generator(boolean ignoreCharset, CallbackFactory factory) {
    this.ignoreCharset = ignoreCharset;
    this.callbackFactory = factory;
  }
  
  public Generator() {
    this(true, defaultCallbackFactory);
  }
  
  /*
  private InputStreamReader getStream(String url) throws IOException {
    return getStream( Generator.class.getClassLoader().getResource( url ) );
  }
  */
  
  private InputStreamReader getStream(URL url) throws IOException {
    URLConnection conn = url.openConnection();
    conn.setDefaultUseCaches(false);
    conn.setUseCaches(false);
    
    return new InputStreamReader(conn.getInputStream());
  }
  
  public static void main(String args[]) throws IOException {
    Generator g = new Generator();
    if (new File(args[0]).exists()) {
      g.generate(new URL("file:" + args[0]), new File(args[1]));
    }
  }
  
  private void writeRegion(FileWriter out, String region, String tempFileName) throws IOException {
    FileReader tmp = new FileReader(tempFileName);
    BufferedReader tempIn = new BufferedReader(tmp);
    out.write("// " + region + " Region Begins\n");
    String line = tempIn.readLine();
    while(line != null) {
      out.write(line+"\n");
      line = tempIn.readLine();
    }
    out.write("// " + region + " Region Ends\n\n\n");
  }
  
  public void generate(URL input, File outFile) throws IOException {
    InputStreamReader fr = getStream( input );
    FileWriter out = new FileWriter(outFile);
    
    FileWriter out1 = new FileWriter(temp1);
    FileWriter out2 = new FileWriter(temp2);
    FileWriter out3 = new FileWriter(temp3);
    
    IHtmlParser parser = new HTMLJerichoParser();
    IHtmlCallback parseHandler = callbackFactory.createCallback(input, out1, out2, out3);
    parser.parse(fr, parseHandler, input.getFile());
    out1.flush();
    out1.close();
    out2.flush();
    out2.close();
    out3.flush();
    out3.close();
    
    // generatePreamble(out, cb);
    
    out.write("\n\ndocument.URL = new String('" + input + "');\n\n");
    
    writeRegion(out, "Embedded Script", temp2);

    writeRegion(out, "DOM Tree", temp1);
    
    out.write("while (true) {\n\n");
    
    writeRegion(out, "Entrypoints", temp3);

    out.write("\n}\n\n");

    generateTrailer(out, parseHandler);
    
    out.close();
  }

  /*
  protected void generatePreamble(FileWriter out, HTMLEditorKit.ParserCallback cb) throws IOException {
    InputStreamReader pm = getStream( preamble );
    BufferedReader pmIn = new BufferedReader(pm);
    
    out.write("//Preamble Begin\n");
    String line = pmIn.readLine();
    while(line != null) {
      out.write(line+"\n");
      line = pmIn.readLine();
    }
    out.write("//Preamble End\n\n\n");
  }
  */
  
  protected void generateTrailer(FileWriter out, IHtmlCallback parseHandler) throws IOException {
    out.write("//Trailer Begin\n");
    out.write("//Trailer End\n");
  }
}
