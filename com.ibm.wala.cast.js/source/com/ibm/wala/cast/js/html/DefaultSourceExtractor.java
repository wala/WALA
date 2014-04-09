/******************************************************************************
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cast.js.html;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;

public class DefaultSourceExtractor extends DomLessSourceExtractor{

  private static class HtmlCallBack extends DomLessSourceExtractor.HtmlCallback{

    private final HashMap<String, String> constructors = HashMapFactory.make();
 
    private final Stack<String> stack = new Stack<String>();

    private final Stack<ITag> forms = new Stack<ITag>();
    private final Set<Pair<ITag,String>> sets = new HashSet<Pair<ITag,String>>();

    public HtmlCallBack(URL entrypointUrl, IUrlResolver urlResolver) {
      super(entrypointUrl, urlResolver);
      constructors.put("FORM", "DOMHTMLFormElement");
      constructors.put("TABLE", "DOMHTMLTableElement");
    }

    @Override
    public void handleEndTag(ITag tag) {
      super.handleEndTag(tag);
      endElement(stack.pop());
      if (tag.getName().equalsIgnoreCase("FORM")) {
        forms.pop();
      }
      for(String v : tag.getAllAttributes().values()) {
        if (v != null && v.startsWith("javascript:")) {
          entrypointRegion.println(v.substring(11), makePos(tag.getStartingLineNum(), tag));
        }
      }
   }

    @Override
    protected void handleDOM(ITag tag, String funcName) {

      String cons = constructors.get(tag.getName().toUpperCase());
      if(cons == null) cons = "DOMHTMLElement";
      writeElement(tag, cons, funcName);
      newLine();
    }

    private void printlnIndented(String line, ITag relatedTag){
      StringBuilder indentedLine = new StringBuilder();
      for (int i = 0 ; i < stack.size() ; i++){
        indentedLine.append("  ");
      }
      indentedLine.append(line);

      if (relatedTag == null){
        domRegion.println(indentedLine.toString());
      } else {
        domRegion.println(indentedLine.toString(), makePos(relatedTag.getStartingLineNum(), relatedTag));
      }
    }

    private void newLine(){
      domRegion.println("");
    }
    
    protected void writeElement(ITag tag, String cons, String varName){

      printlnIndented("function make_" + varName + "(parent) {", tag);
      stack.push(varName);
      
      printlnIndented("this.temp = " + cons + ";", tag);
      printlnIndented("this.temp(\"" + tag.getName() + "\");", tag);
      for (Map.Entry<String, String> e : tag.getAllAttributes().entrySet()){
        String attr = e.getKey();
        String value = e.getValue();
        writeAttribute(tag, attr, value, "this", varName);
      }

      if (tag.getName().equalsIgnoreCase("FORM")) {
        forms.push(tag);
        printlnIndented("  document.forms[document.formCount++] = this;", tag);
        printlnIndented("  var currentForm = this;", tag);
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
            printlnIndented("  currentForm." + prop + " = new Array();", tag);
            printlnIndented("  currentForm." + prop + "Counter = 0;", tag);
          }
          printlnIndented("  currentForm." + prop + "[currentForm." + prop + "Counter++] = this;", tag);
        } else {
          printlnIndented("  currentForm." + prop + " = this;", tag);          
        }
      }
      }

      printlnIndented(varName + " = this;", tag);
      printlnIndented("dom_nodes." + varName + " = this;", tag);
      printlnIndented("parent.appendChild(this);", tag);
    }

    protected void writeAttribute(ITag tag, String attr, String value, String varName, String varName2) {
      writePortletAttribute(tag, attr, value, varName);
      writeEventAttribute(tag, attr, value, varName, varName2);
    }

    protected void writeEventAttribute(ITag tag, String attr, String value, String varName, String varName2){
      if(attr.substring(0,2).equals("on")) {
        printlnIndented(varName + "." + attr + " = function " + tag.getName().toLowerCase() + "_" + attr + "(event) {" + value + "};", tag);
        entrypointRegion.println(varName2 + "." + attr + "(null);", makePos(tag.getStartingLineNum(), tag));
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
        printlnIndented(varName + "['" + attr + "'] = '" + value + "';", tag);
      }
    }

    protected void writePortletAttribute(ITag tag, String attr, String value, String varName){
      if(attr.equals("portletid")) {
        if(value.substring(value.length()-4).equals("vice")) {
          newLine(); newLine();
          printlnIndented("function cVice() { var contextVice = " + varName + "; }\ncVice();\n", tag);
        } else if(value.substring(value.length()-4).equals("root")) {
          newLine(); newLine();
          printlnIndented("function cRoot() { var contextRoot = " + varName + "; }\ncRoot();\n", tag);
        }
      }
    }

    protected void endElement(String name) {
      printlnIndented("};", null);
      if (stack.isEmpty()) {
        printlnIndented("new make_" + name + "(document);\n\n", null);
      } else {
        printlnIndented("new make_" + name + "(this);\n", null);
      }
    }
  }

  @Override
  protected IGeneratorCallback createHtmlCallback(URL entrypointUrl, IUrlResolver urlResolver) {
    return new HtmlCallBack(entrypointUrl, urlResolver);
  }
}
