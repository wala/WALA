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

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;

import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import com.ibm.wala.util.collections.HashMapFactory;

public class HTMLCallback extends HTMLEditorKit.ParserCallback {
  private final URL input;
  private final FileWriter domTreeFile, embeddedScriptFile, entrypointFile;

  private int counter=0;
  private boolean script = false;

  private final HashMap<String, String> constructors = HashMapFactory.make();
	
  protected final Stack<String> stack;

  private int scriptStart;
  
  public HTMLCallback(URL input, FileWriter out, FileWriter out2, FileWriter entrypointFile) {
    this.input = input;
    this.domTreeFile = out;
    this.embeddedScriptFile = out2;
    this.entrypointFile = entrypointFile;
    stack = new Stack<String>();
    constructors.put("FORM", "DOMHTMLFormElement");
    constructors.put("TABLE", "DOMHTMLTableElement");
  }
  
  private void indent() throws IOException {
    for(int i = 0; i < stack.size(); i++) {
      domTreeFile.write("  ");
    }
  }
  	
  public void flush() throws BadLocationException {
      
  }
        
  private void writeEmbeddedScript(char[] text, int length) throws IOException {
    embeddedScriptFile.write(text, 0, length);
  }

  private void writeEmbeddedScript(String text) throws IOException {
    embeddedScriptFile.write(text);
 }
        
  protected String createElement(HTML.Tag t, MutableAttributeSet a, int pos) {
    String tag = t.toString().toUpperCase();
    if(tag.equals("SCRIPT")) {
      Object l = a.getAttribute(HTML.Attribute.LANGUAGE);
      if (l == null || l.toString().toUpperCase().indexOf("VB") < 0) {
        Object value = a.getAttribute( HTML.Attribute.SRC );

        // script is out-of-line
        if (value != null) {
          try {
            URL scriptSrc = new URL(input, value.toString());
            InputStreamReader scriptReader =
              new InputStreamReader(
                  scriptSrc.openConnection().getInputStream());

            int read;
            char[] buffer = new char[ 1024 ];
            while ( (read = scriptReader.read(buffer)) != -1 ) {
              writeEmbeddedScript(buffer, read);
            }
            embeddedScriptFile.flush();
            scriptReader.close();
          } catch (IOException e) {
            System.out.println("bad input script " + value);
          }

          // script is inline
        } else {
          script = true;
          scriptStart = pos;
        }
      }
    }
    
    String varName = null;
    Enumeration enu = a.getAttributeNames();
    while(enu.hasMoreElements()) {
      Object attrObj = enu.nextElement(); 
      String attr = attrObj.toString();
      String value = a.getAttribute(attrObj).toString();
      if (attr.equalsIgnoreCase("id")) {
        varName = value;
        break;
      }
    }
    if (varName == null) {
      varName = "node" + (counter++);
    }
    
    String cons = constructors.get(tag);
    if(cons == null) cons = "DOMHTMLElement";
    
    try {
      writeElement(t, a, tag, cons, varName);
      domTreeFile.write("\n");
    } catch (IOException e) {
      System.out.println("Error writing to file");
      System.exit(1);
    }
    return varName;
    }
  
    protected void writeElement(HTML.Tag t, MutableAttributeSet a, String tag, String cons, String varName) throws IOException {
      Enumeration enu = a.getAttributeNames();
      indent(); domTreeFile.write("function make_" + varName + "(parent) {\n");
      indent(); domTreeFile.write("  this.temp = " + cons + ";\n");
      indent(); domTreeFile.write("  this.temp(" + tag + ");\n");
      while(enu.hasMoreElements()) {
        Object attrObj = enu.nextElement(); 
        String attr = attrObj.toString();
        String value = a.getAttribute(attrObj).toString();
        domTreeFile.write("  ");
        writeAttribute(t, a, attr, value, "this", varName);
      }

      indent(); domTreeFile.write("  " + varName + " = this;\n");
      indent(); domTreeFile.write("  dom_nodes." + varName + " = this;\n");
      indent(); domTreeFile.write("  parent.appendChild(this);\n");
   }
  
    protected void writeAttribute(HTML.Tag t, MutableAttributeSet a, String attr, String value, String varName, String varName2) throws IOException {
      writePortletAttribute(t, a, attr, value, varName);
      writeEventAttribute(t, a, attr, value, varName, varName2);
    }
  
    protected void writeEventAttribute(HTML.Tag t, MutableAttributeSet a, String attr, String value, String varName, String varName2) throws IOException {
      if(attr.substring(0,2).equals("on")) {
        indent(); domTreeFile.write("function " + attr + "_" + varName2 + "(event) {" + value + "};\n");
        indent(); domTreeFile.write(varName + "." + attr + " = " + attr + "_" + varName2 + ";\n");
        entrypointFile.write("\n\n  " + varName2 + "." + attr + "(null);\n\n");
      } else if (value.startsWith("javascript:") || value.startsWith("javaScript:")) {
        indent(); domTreeFile.write("var " + varName + attr + " = " + value.substring(11) + "\n");
        indent(); domTreeFile.write(varName + ".setAttribute('" + attr + "', " + varName + attr + ");\n");
      } else {
        if (value.indexOf('\'') > 0) {
          value = value.replaceAll("\\'", "\\\\'");
      }
      if (value.indexOf('\n') > 0) {
        value = value.replaceAll("\\n", "\\\\n");
      }
      indent(); domTreeFile.write(varName + ".setAttribute('" + attr + "', '" + value + "');\n");
    }
  }

  protected void writePortletAttribute(HTML.Tag t, MutableAttributeSet a, String attr, String value, String varName) throws IOException {
    if(attr.equals("portletid")) {
      if(value.substring(value.length()-4).equals("vice")) {
        indent(); domTreeFile.write("\n\nfunction cVice() { var contextVice = " + varName + "; }\ncVice();\n\n");
      } else if(value.substring(value.length()-4).equals("root")) {
        indent(); domTreeFile.write("\n\nfunction cRoot() { var contextRoot = " + varName + "; }\ncRoot();\n\n");
      }
    }
  }
  
  public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
    String varName = createElement(t,a,pos);
    stack.push(varName);
  }
 
  private void endElement(String name) {
    try {
      indent(); domTreeFile.write("};\n");
      indent();
      if (stack.isEmpty()) {
        domTreeFile.write("new make_" + name + "(document);\n\n\n");
      } else {
        domTreeFile.write("new make_" + name + "(this);\n\n");
      }
    } catch (IOException e) {
      System.exit(-1);
    }
  }

  public void handleEndTag(HTML.Tag t, int pos) {
    if(t.toString().toUpperCase().equals("SCRIPT")) {
      if (script) try {
        int scriptEnd = pos;
        InputStreamReader script = new InputStreamReader(input.openStream());
        char[] buf = new char[ scriptEnd ];
        script.read(buf);
        String s = String.valueOf(buf, scriptStart, scriptEnd-scriptStart);
        writeEmbeddedScript(s.substring(s.indexOf('>')+1));
      } catch (IOException e) {
        
      }
      script = false;
    }
     endElement(stack.pop());
 }
    
  public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
    if (! script) {
      String nm = createElement(t, a, pos);
      endElement(nm);
    }
  }
    
  public void handleError(String errorMsg, int pos) {
    System.out.println("Error" + errorMsg);
  }
    
}
