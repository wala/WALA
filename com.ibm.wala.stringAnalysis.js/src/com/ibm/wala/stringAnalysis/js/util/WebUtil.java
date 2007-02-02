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
package com.ibm.wala.stringAnalysis.js.util;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.*;

import com.ibm.wala.automaton.string.*;
import com.ibm.wala.cast.js.util.*;
import com.ibm.wala.cast.js.util.Generator.CallbackFactory;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.stringAnalysis.util.*;

public class WebUtil extends com.ibm.wala.cast.js.util.WebUtil {
  static public class SimpleDHTMLCallback extends HTMLCallback {
    final private Set<String> eventHandlers;
    
    public SimpleDHTMLCallback(URL input, FileWriter out, FileWriter out2) {
      super(input, out, out2);
      this.eventHandlers = new HashSet<String>();
    }
    
    protected void writeElement(HTML.Tag t, MutableAttributeSet a, String tag, String cons, String varName) throws IOException {
    }

    protected void writeEventAttribute(HTML.Tag t, MutableAttributeSet a, String attr, String value, String varName) throws IOException {
      if(attr.substring(0,2).equals("on")) {
        eventHandlers.add(value);
      }
    }
    
    protected void writePortletAttribute(Tag t, MutableAttributeSet a, String attr, String value, String varName) throws IOException {
    }

    public Set<String> getEventHandlers() {
      return eventHandlers;
    }
  }
  
  static public class SimpleDHTMLCallbackFactory implements CallbackFactory {
    public ParserCallback createCallback(URL input, FileWriter out, FileWriter out2) {
      return new SimpleDHTMLCallback(input, out, out2);
    }
  }

  static public class SimpleDHTMLGenerator extends Generator {
    URL html;
    public SimpleDHTMLGenerator(URL html) {
      super(true, new SimpleDHTMLCallbackFactory());
      this.html = html;
    }
    
    protected void generatePreamble(FileWriter out, HTMLEditorKit.ParserCallback cb) throws IOException {
      InputStream is = html.openStream();
      byte b[] = new byte[256];
      int n = 0;
      StringBuffer buff = new StringBuffer();
      while((n = is.read(b, 0, 256))>0) {
        String s = new String(b, 0, n);
        s = s.replaceAll("[\\r\\n]", " ");
        s = s.replaceAll("\\\"", "\\\\\"");
        buff.append(s);
      }
      out.write("document = document._XML(\"" +  buff + "\")");
    }

    protected void generateTrailer(FileWriter out, HTMLEditorKit.ParserCallback cb) throws IOException {
      SimpleDHTMLCallback dcb = (SimpleDHTMLCallback) cb;
      for (String script : dcb.getEventHandlers()) {
        script = script.replaceAll("return\\s+", "");
        out.write("while(true){ " + script + " }\n");
      }
      out.write("DOCUMENT = document;");
    }
  }
  
  static public class DHTMLCallback extends HTMLCallback {
    final private Set<String> eventHandlers;
    
    public DHTMLCallback(URL input, FileWriter out, FileWriter out2) {
      super(input, out, out2);
      this.eventHandlers = new HashSet<String>();
    }
    
    protected void writeElement(HTML.Tag t, MutableAttributeSet a, String tag, String cons, String varName) throws IOException {
      getWriter().write("var " + varName + " = document.createElement(\"" + t.toString() + "\");\n");
      if(!stack.empty()) {
        getWriter().write(stack.peek() + ".appendChild(" + varName + ");\n");
      } else {
        getWriter().write("document.appendChild(" + varName + ");\n");
      }
    }

    protected void writeEventAttribute(HTML.Tag t, MutableAttributeSet a, String attr, String value, String varName) throws IOException {
      if(attr.substring(0,2).equals("on")) {
        getWriter().write(varName + "." + attr + " = function " + attr + "_" + varName + "(event) {" + value + "};\n");
        getWriter2().write("\n\nwhile (true) { var e = {target: " + varName + "}; " + varName + "." + attr + "(e); }\n\n");
      } else {
        getWriter().write(varName + ".setAttribute('" + attr + "', '" + value + "');\n");
      }
    }
    
    public Set<String> getEventHandlers() {
      return eventHandlers;
    }
  }
  
  static public class DHTMLCallbackFactory implements CallbackFactory {
    public ParserCallback createCallback(URL input, FileWriter out, FileWriter out2) {
      return new DHTMLCallback(input, out, out2);
    }
  }
  
  static public class DHTMLGenerator extends Generator {
    public DHTMLGenerator() {
      super(true, new DHTMLCallbackFactory());
    }
    
    protected void generatePreamble(FileWriter out) throws IOException {
      // we don't need the preamble.
      // super.generatePreamble(out);
    }

    protected void generateTrailer(FileWriter out, HTMLEditorKit.ParserCallback cb) throws IOException {
      out.write("DOCUMENT = node0");
    }
  }
}
