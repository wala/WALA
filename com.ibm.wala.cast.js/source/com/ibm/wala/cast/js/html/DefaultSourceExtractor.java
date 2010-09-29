package com.ibm.wala.cast.js.html;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.ibm.wala.util.collections.HashMapFactory;

public class DefaultSourceExtractor extends DomLessSourceExtractor{

  private static class HtmlCallBack extends DomLessSourceExtractor.HtmlCallback{

    private final HashMap<String, String> constructors = HashMapFactory.make();
    protected final Stack<String> stack;

    public HtmlCallBack(URL entrypointUrl, IUrlResolver urlResolver) {
      super(entrypointUrl, urlResolver);

      stack = new Stack<String>();
      constructors.put("FORM", "DOMHTMLFormElement");
      constructors.put("TABLE", "DOMHTMLTableElement");

    }

    @Override
    public void handleEndTag(ITag tag) {
      super.handleEndTag(tag);
      endElement(stack.pop());
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
        domRegion.println(indentedLine.toString(), fileName, relatedTag.getStartingLineNum());
      }
    }

    private void newLine(){
      domRegion.println("");
    }
    
    protected void writeElement(ITag tag, String cons, String varName){

      printlnIndented("function make_" + varName + "(parent) {", tag);
      stack.push(varName);
      
      printlnIndented("this.temp = " + cons + ";", tag);
      printlnIndented("this.temp(" + tag.getName() + ");", tag);
      for (Map.Entry<String, String> e : tag.getAllAttributes().entrySet()){
        String attr = e.getKey();
        String value = e.getValue();
        writeAttribute(tag, attr, value, "this", varName);
      }

      printlnIndented("" + varName + " = this;", tag);
      printlnIndented("dom_nodes." + varName + " = this;", tag);
      printlnIndented("parent.appendChild(this);", tag);
    }

    protected void writeAttribute(ITag tag, String attr, String value, String varName, String varName2) {
      writePortletAttribute(tag, attr, value, varName);
      writeEventAttribute(tag, attr, value, varName, varName2);
    }

    protected void writeEventAttribute(ITag tag, String attr, String value, String varName, String varName2){
      if(attr.substring(0,2).equals("on")) {
        printlnIndented("function " + attr + "_" + varName2 + "(event) {" + value + "};", tag);
        printlnIndented(varName + "." + attr + " = " + attr + "_" + varName2 + ";", tag);
        newLine(); newLine();
        printlnIndented(varName2 + "." + attr + "(null);\n", tag);
      } else if (value.startsWith("javascript:") || value.startsWith("javaScript:")) {
        printlnIndented("var " + varName + attr + " = " + value.substring(11), tag);
        printlnIndented(varName + ".setAttribute('" + attr + "', " + varName + attr + ");", tag);
      } else {
        if (value.indexOf('\'') > 0) {
          value = value.replaceAll("\\'", "\\\\'");
        }
        if (value.indexOf('\n') > 0) {
          value = value.replaceAll("\\n", "\\\\n");
        }
        printlnIndented(varName + ".setAttribute('" + attr + "', '" + value + "');", tag);
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

    private void endElement(String name) {
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
