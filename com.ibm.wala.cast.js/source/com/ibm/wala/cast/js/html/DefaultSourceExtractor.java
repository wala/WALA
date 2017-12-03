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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.function.Supplier;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;

public class DefaultSourceExtractor extends DomLessSourceExtractor{

  public static Supplier<JSSourceExtractor> factory = DefaultSourceExtractor::new;
  
  protected static class HtmlCallBack extends DomLessSourceExtractor.HtmlCallback{

    private final HashMap<String, String> constructors = HashMapFactory.make();
 
    private final Stack<String> stack = new Stack<>();

    private final Stack<ITag> forms = new Stack<>();
    private final Set<Pair<ITag,String>> sets = new HashSet<>();

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
      for(Entry<String,Pair<String, Position>> e : tag.getAllAttributes().entrySet()) {
        String a = e.getKey();
        String v = e.getValue().fst;
        if (v != null && v.startsWith("javascript:")) {
          try {
            writeEntrypoint("           " + v.substring(11), e.getValue().snd, new URL(tag.getElementPosition().getURL().toString() + "#" + a), true);
          } catch (MalformedURLException ex) {
            writeEntrypoint(v.substring(11), e.getValue().snd, entrypointUrl, false);
          }
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

    protected void printlnIndented(String line, ITag relatedTag){
      printlnIndented(line, relatedTag==null? null: relatedTag.getElementPosition());
    }
    
    private void printlnIndented(String line, Position pos){
      StringBuilder indentedLine = new StringBuilder();
      for (int i = 0 ; i < stack.size() ; i++){
        indentedLine.append("  ");
      }
      indentedLine.append(line);

      if (pos == null){
        domRegion.println(indentedLine.toString());
      } else {
        domRegion.println(indentedLine.toString(), pos, entrypointUrl, false);
      }
    }

    private void newLine(){
      domRegion.println("");
    }

    private static String makeRef(String object, String property) {
      assert object != null && property != null;
      return object + "[\"" + property + "\"]";
    }
    
    protected void writeElement(ITag tag, String cons, String varName){
      Map<String, Pair<String, Position>> attrs = tag.getAllAttributes();

      printlnIndented("function make_" + varName + "(parent) {", tag);
      stack.push(varName);

      printlnIndented("this.temp = " + cons + ";", tag);
      printlnIndented("this.temp(\"" + tag.getName() + "\");", tag);
      for (Map.Entry<String, Pair<String, Position>> e : attrs.entrySet()){
        String attr = e.getKey();
        String value = e.getValue().fst;
        writeAttribute(tag, attr, value, "this", varName);
      }

      if (tag.getName().equalsIgnoreCase("FORM")) {
        forms.push(tag);
        printlnIndented("  document.forms[document.formCount++] = this;", tag);
        printlnIndented("  var currentForm = this;", tag);
      } if (tag.getName().equalsIgnoreCase("INPUT")) {
        String prop = attrs.containsKey("name") ? attrs.get("name").fst : null;
        String type = attrs.containsKey("type") ? attrs.get("type").fst : null;

        if (type != null && prop != null) {
          //input tags do not need to be in a form
          if (!forms.isEmpty()) {
             if (type.equalsIgnoreCase("RADIO")) {
              if (! sets.contains(Pair.make(forms.peek(), prop))) {
                sets.add(Pair.make(forms.peek(), prop));
                printlnIndented("  " + makeRef("currentForm", prop) + " = new Array();", tag);
                printlnIndented("  " + makeRef("currentForm", prop + "Counter") + " = 0;", tag);
              }
              printlnIndented("  " + makeRef(makeRef("currentForm", prop), prop + "Counter++") + " = this;", tag);
            } else {
              printlnIndented("  " + makeRef("currentForm", prop) + " = this;", tag);          
            }
          }
        }
        
        inputElementCallback();
      }

      assert varName != null && !"".equals(varName);
      printlnIndented(varName + " = this;", tag);
      printlnIndented("document." + varName + " = this;", tag);
      printlnIndented("parent.appendChild(this);", tag);
    }

    protected void inputElementCallback() {
      // this space intentionally left blank 
    }

    protected void writeAttribute(ITag tag, String attr, String value, String varName, String varName2) {
      writePortletAttribute(tag, attr, value, varName);
      writeEventAttribute(tag, attr, value, varName, varName2);
    }

    protected void writeEventAttribute(ITag tag, String attr, String value, String varName, String varName2){
      //There should probably be more checking to see what the attributes are since we allow things like: ; to be used as attributes now.
      if(attr.length() >= 2 && attr.substring(0,2).equals("on")) {
        printlnIndented(varName + "." + attr + " = function " + tag.getName().toLowerCase() + "_" + attr + "(event) {" + value + "};", tag);
        writeEntrypoint(varName2 + "." + attr + "(null);", tag.getElementPosition(), entrypointUrl, false);
      } else if (value != null) {
        
        Pair<String, Character> x = quotify(value);
        value = x.fst;
        char quote = x.snd;
        
        if (attr.equals(attr.toUpperCase())) {
          attr = attr.toLowerCase();
        }
        
        printlnIndented(varName + "['" + attr + "'] = " + quote + value + quote + ";", tag);
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
      printlnIndented("};", (Position)null);
      if (stack.isEmpty()) {
        printlnIndented("new make_" + name + "(document);\n\n", (Position)null);
      } else {
        printlnIndented("new make_" + name + "(this);\n", (Position)null);
      }
    }
  }

  @Override
  protected IGeneratorCallback createHtmlCallback(URL entrypointUrl, IUrlResolver urlResolver) {
    return new HtmlCallBack(entrypointUrl, urlResolver);
  }
}
