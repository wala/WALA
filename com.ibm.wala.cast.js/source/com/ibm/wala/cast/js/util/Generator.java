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

import java.io.*;
import java.net.*;

import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

public class Generator {
  public static final String preamble = "preamble.js", temp = "temp.js";
  
  public static interface CallbackFactory {
    HTMLEditorKit.ParserCallback createCallback(URL input, FileWriter out, FileWriter out2);
  }
  
  public static class HTMLCallbackFactory implements CallbackFactory {
    public HTMLEditorKit.ParserCallback createCallback(URL input, FileWriter out, FileWriter out2) {
      return new HTMLCallback(input, out, out2);
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
    this(false, defaultCallbackFactory);
  }
  
  private InputStreamReader getStream(String url) throws IOException {
    return getStream( Generator.class.getClassLoader().getResource( url ) );
  }

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
  
  public void generate(URL input, File outFile) throws IOException {
    InputStreamReader fr = getStream( input );
    FileWriter out = new FileWriter(outFile);
    FileWriter out2 = new FileWriter(temp);
    
    ParserDelegator pd = new ParserDelegator();
    HTMLEditorKit.ParserCallback cb = callbackFactory.createCallback(input, out, out2);

    generatePreamble(out, cb);
    
    out.write("//Generation of the DOM Tree Begins\n");
    pd.parse(fr, cb, ignoreCharset);
    out2.close();
    out.write("//Generation of the DOM Tree Ends\n\n\n");
    
    FileReader tmp = new FileReader(temp);
    BufferedReader tempIn = new BufferedReader(tmp);
    out.write("//Embedded Script Region Begins\n");
    String line = tempIn.readLine();
    while(line != null) {
      out.write(line+"\n");
      line = tempIn.readLine();
    }
    out.write("//Embedded Script Region Ends\n\n\n");
    
    generateTrailer(out, cb);
    
    out.close();
  }

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
  
  protected void generateTrailer(FileWriter out, HTMLEditorKit.ParserCallback cb) throws IOException {
    out.write("//Trailer Begin\n");
    out.write("//Trailer End\n");
  }
}
