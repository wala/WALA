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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wala.cast.js.html.IHtmlCallback;
import com.ibm.wala.cast.js.html.ITag;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

public class HTMLCallback implements IHtmlCallback {
  private final URL input;
  private final FileWriter domTreeFile, embeddedScriptFile, entrypointFile;

  private int counter=0;

  private final HashMap<String, String> constructors = HashMapFactory.make();
	
  protected final Stack<String> stack;

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
  	
  private void writeEmbeddedScript(char[] text, int length) throws IOException {
    embeddedScriptFile.write(text, 0, length);
  }

  private final Pattern ctrl = Pattern.compile("[\\p{Cntrl}&&[^\\p{Space}]]");

  private void writeEmbeddedScript(String text) throws IOException {
    Matcher m = ctrl.matcher(text);
    embeddedScriptFile.write(m.replaceAll(" "));
 }
        
  protected String createElement(ITag tag) {
//    String tag = t.toString().toUpperCase();
    if(tag.getName().equalsIgnoreCase("SCRIPT")) {
      String lang = tag.getAttributeByName("language");
      if (lang == null || lang.toUpperCase().indexOf("VB") < 0) {
        String src = tag.getAttributeByName("src");

        // script is out-of-line
        if (src != null) {
          try {
            URL scriptSrc = new URL(input, src);
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
            System.out.println("bad input script " + src);
          }

          // script is inline
        } else {
          String content = tag.getBodyText().snd;
          try {
            writeEmbeddedScript(content);
          } catch (IOException e) {
            System.err.println("Cannot write embedded script " + content);
          }
        }
      }
    }
    
    String varName = getVarNameForTag(tag);
    
    String cons = constructors.get(tag.getName().toUpperCase());
    if(cons == null) cons = "DOMHTMLElement";
    
    try {
      writeElement(tag, cons, varName);
      domTreeFile.write("\n");
    } catch (IOException e) {
      System.out.println("Error writing to file");
      System.exit(1);
    }
    return varName;
  }

  private String getVarNameForTag(ITag tag) {
    String varName = null;
    for (Map.Entry<String, String> e : tag.getAllAttributes().entrySet()){
      String attr = e.getKey();
      String value = e.getValue();
      if (attr.equalsIgnoreCase("id")) {
        if (value.indexOf('-') == -1) {
          varName = value;
        }
        break;
      }
    }
    if (varName == null) {
      varName = "node" + (counter++);
    }
    return varName;
  }
  
  private Stack<ITag> forms = new Stack<ITag>();
  private Set<Pair<ITag,String>> sets = new HashSet<Pair<ITag,String>>();
  
  protected void writeElement(ITag tag, String cons, String varName) throws IOException {
    
      indent(); domTreeFile.write("function make_" + varName + "(parent) {\n");
      indent(); domTreeFile.write("  this.temp = " + cons + ";\n");
      indent(); domTreeFile.write("  this.temp('" + tag.getName() + "');\n");
      for (Map.Entry<String, String> e : tag.getAllAttributes().entrySet()){
        String attr = e.getKey();
        String value = e.getValue();
        domTreeFile.write("  ");
        writeAttribute(tag, attr, value, "this", varName);
      }

      if (tag.getName().equalsIgnoreCase("FORM")) {
        forms.push(tag);
        indent(); domTreeFile.write("  var currentForm = this;\n");
      } if (tag.getName().equalsIgnoreCase("INPUT")) {
        String prop = tag.getAttributeByName("NAME");
        if (prop == null) {
          prop = tag.getAttributeByName("name");
        }
        
        String type = tag.getAttributeByName("TYPE");
        if (type == null) {
          type = tag.getAttributeByName("type");
        }
 
        if (type != null && prop != null) {
        if (type.equalsIgnoreCase("RADIO")) {
          if (! sets.contains(Pair.make(forms.peek(), prop))) {
            sets.add(Pair.make(forms.peek(), prop));
            indent(); domTreeFile.write("  currentForm." + prop + " = new Array();\n");
            indent(); domTreeFile.write("  currentForm." + prop + "Counter = 0;\n");
          }
          indent(); domTreeFile.write("  currentForm." + prop + "[currentForm." + prop + "Counter++] = this;\n");
        } else {
          indent(); domTreeFile.write("  currentForm." + prop + " = this;\n");          
        }
      }
      }
      
      indent(); domTreeFile.write("  " + varName + " = this;\n");
      indent(); domTreeFile.write("  dom_nodes." + varName + " = this;\n");
      indent(); domTreeFile.write("  parent.appendChild(this);\n");
   }
  
    protected void writeAttribute(ITag tag, String attr, String value, String varName, String varName2) throws IOException {
      writePortletAttribute(tag, attr, value, varName);
      writeEventAttribute(tag, attr, value, varName, varName2);
    }
  
    protected void writeEventAttribute(ITag tag, String attr, String value, String varName, String varName2) throws IOException {
      if(attr.substring(0,2).equals("on")) {
        indent(); domTreeFile.write("function " + attr + "_" + varName2 + "(event) {" + value + "};\n");
        indent(); domTreeFile.write(varName + "." + attr + " = " + attr + "_" + varName2 + ";\n");
        entrypointFile.write("\n\n  " + varName2 + "." + attr + "(null);\n\n");
      } else if (value != null) {
        if (value.indexOf('\'') > 0) {
          value = value.replaceAll("\\'", "\\\\'");
        }
        if (value.indexOf('\n') > 0) {
          value = value.replaceAll("\\n", "\\\\n");
        }
        if (attr.equals(attr.toUpperCase())) {
          attr = attr.toLowerCase();
        }
        // indent(); domTreeFile.write(varName + ".setAttribute('" + attr + "', '" + value + "');\n");
        indent(); domTreeFile.write(varName + "['" + attr + "'] = '" + value + "';\n");
      }
    }

  protected void writePortletAttribute(ITag tag, String attr, String value, String varName) throws IOException {
    if(attr.equals("portletid")) {
      if(value.substring(value.length()-4).equals("vice")) {
        indent(); domTreeFile.write("\n\nfunction cVice() { var contextVice = " + varName + "; }\ncVice();\n\n");
      } else if(value.substring(value.length()-4).equals("root")) {
        indent(); domTreeFile.write("\n\nfunction cRoot() { var contextRoot = " + varName + "; }\ncRoot();\n\n");
      }
    }
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

  public void handleEndTag(ITag tag) {
    endElement(stack.pop());
    if (tag.getName().equalsIgnoreCase("FORM")) {
      forms.pop();
    }
    for(String v : tag.getAllAttributes().values()) {
      if (v != null && v.startsWith("javascript:")) {
        try {
          entrypointFile.write( v.substring(11) );
        } catch (IOException e) {
          Assertions.UNREACHABLE(e.toString());
        }
      }
    }
  }

  public void handleStartTag(ITag tag) {
    String varName = createElement(tag);
    stack.push(varName);
  }
    
}
