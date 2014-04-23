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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

/**
 * extracts JavaScript source code from HTML, with no model of the actual
 * DOM data structure
 */
public class DomLessSourceExtractor extends JSSourceExtractor {
  private static final Pattern LEGAL_JS_IDENTIFIER_REGEXP = Pattern.compile("^[a-zA-Z$_][a-zA-Z\\d$_]*$");
  private static final Pattern LEGAL_JS_KEYWORD_REGEXP = Pattern.compile("^((break)|(case)|(catch)|(continue)|(debugger)|(default)|(delete)|(do)|(else)|(finally)|(for)|(function)|(if)|(in)|(instanceof)|(new)|(return)|(switch)|(this)|(throw)|(try)|(typeof)|(var)|(void)|(while)|(with))$");


  protected interface IGeneratorCallback extends IHtmlCallback {
    void writeToFinalRegion(SourceRegion finalRegion);
  }
  
  protected static class HtmlCallback implements IGeneratorCallback{
    
    public static final boolean DEBUG = false;
    
    protected final URL entrypointUrl;
    protected final IUrlResolver urlResolver;

    protected final SourceRegion scriptRegion;
    protected final SourceRegion domRegion;
    protected final SourceRegion entrypointRegion;
    
    private ITag currentScriptTag;
    
    private int nodeCounter = 0;
    private int scriptNodeCounter = 0;
 
    public HtmlCallback(URL entrypointUrl, IUrlResolver urlResolver) {
      this.entrypointUrl = entrypointUrl;
      this.urlResolver  = urlResolver;
      this.scriptRegion = new SourceRegion();
      this.domRegion = new SourceRegion();
      this.entrypointRegion = new SourceRegion();
      addDefaultHandlerInvocations();
    }
 
    private void addDefaultHandlerInvocations() {
      // always invoke window.onload
      entrypointRegion.println("window.onload();");
    }

    protected Position makePos(int lineNumber, ITag governingTag) {
      return makePos(entrypointUrl, lineNumber, governingTag);
    }
     
    protected Position makePos(final URL url, final int lineNumber, ITag governingTag) {
      return governingTag.getElementPosition();
     }
    
    @Override
    public void handleEndTag(ITag tag) {
      if (tag.getName().equalsIgnoreCase("script")) {
        assert currentScriptTag != null;
        currentScriptTag = null;
      }
    }

    @Override
    public void handleText(Position p, String text) {
      if (currentScriptTag != null) {
        if (text.startsWith("<![CDATA[")) {
         assert text.endsWith("]]>");
         text = text.substring(9, text.length()-11);
        }
        
        URL url = entrypointUrl;
        try {
          url = new URL(entrypointUrl, "#" + scriptNodeCounter);
        } catch (MalformedURLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        scriptRegion.println(text, currentScriptTag.getContentPosition(), url, true);
      }
    }

    @Override
    public void handleStartTag(ITag tag) {
      if (tag.getName().equalsIgnoreCase("script")) {
        handleScript(tag);
        assert currentScriptTag == null;
        currentScriptTag = tag;
        scriptNodeCounter++;
      }
      handleDOM(tag);
    }

    private boolean isUsableIdentifier(String x) {
      return x != null &&
          LEGAL_JS_IDENTIFIER_REGEXP.matcher(x).matches() &&
          !LEGAL_JS_KEYWORD_REGEXP.matcher(x).matches();
    }
    
    /**
     * Model the HTML DOM
     * 
     * @param tag
     *            - the HTML tag to module
     */
    protected void handleDOM(ITag tag) {
      // Get the name of the modeling function either from the id attribute or a
      // running counter
      Pair<String,Position> idAttribute = tag.getAttributeByName("id");
      String funcName;
      if (idAttribute != null &&  isUsableIdentifier(idAttribute.fst)) {
        funcName = idAttribute.fst;
      } else {
        funcName = "node" + (nodeCounter++);
      }
      handleDOM(tag, funcName);
    }

    protected void handleDOM(ITag tag, String funcName) {
      Map<String, Pair<String,Position>> attributeSet = tag.getAllAttributes();
      for (Entry<String, Pair<String, Position>> a : attributeSet.entrySet()) {
        handleAttribute(a, funcName, tag);
      }
    }

    private void handleAttribute(Entry<String, Pair<String,Position>> a, String funcName, ITag tag) {
      URL url = entrypointUrl;
      try {
        url = new URL(entrypointUrl, "#" + tag.getElementPosition().getFirstOffset());
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        if (DEBUG) {
          e.printStackTrace();
        }
      }
      Position pos = a.getValue().snd;
      String attName = a.getKey();
      String attValue = a.getValue().fst;
      if (attName.toLowerCase().startsWith("on") || (attValue != null && attValue.toLowerCase().startsWith("javascript:"))) {
        String fName = tag.getName().toLowerCase() + "_" + attName + "_" + funcName;
        String signatureLine = "function " + fName + "(event) {";
        // Defines the function  
        domRegion.println(signatureLine + "\n" + extructJS(attValue) + "\n}", pos, url, true);
        // Run it
        entrypointRegion.println("\t" + fName + "(null);", pos, url, true);
      }
    }

    protected static Pair<String,Character> quotify(String value) { 
      char quote;
      if (value.indexOf('"') < 0) {
        quote= '"';
      } else if (value.indexOf("'") < 0) {
        quote= '"';
      } else {
        quote= '"';
        value = value.replaceAll("\"", "\\\"");          
      }

      if (value.indexOf('\n') >= 0) {
        value = value.replaceAll("\n", "\\n");
      }

      return Pair.make(value, quote);
    }
    
    private String extructJS(String attValue) {
      if (attValue == null){
        return "";
      }
      
      String content;
      if (attValue.toLowerCase().equals("javascript:")) {
        content = attValue.substring("javascript:".length());
      } else {
        content = attValue;
      }

      return content;
    }

    protected void handleScript(ITag tag) {

      Pair<String,Position> content = tag.getAttributeByName("src");

      try {
        if (content != null) {
          // script is out-of-line
          getScriptFromUrl(content.fst, tag);
        }

      } catch (IOException e) {
        if (DEBUG) {
          System.err.println("Error reading script file: " + e.getMessage());
        }
      }
    }

    private void getScriptFromUrl(String urlAsString, ITag scriptTag) throws IOException, MalformedURLException {
//      URL absoluteUrl = UrlManipulator.relativeToAbsoluteUrl(urlAsString, this.entrypointUrl);
//      URL scriptSrc = urlResolver.resolve(absoluteUrl);
      URL scriptSrc = new URL(entrypointUrl, urlAsString);
      if (scriptSrc == null) { //Error resolving URL
        return;
      }

      InputStream scriptInputStream;
      try {
         scriptInputStream = scriptSrc.openConnection().getInputStream();
      } catch (Exception e) {
        //it looks like this happens when we can't resolve the url?
        if (DEBUG) {
          System.err.println("Error reading script: " + scriptSrc);
          System.err.println(e);
          e.printStackTrace(System.err);
        }
        return;
      }
      
      BufferedReader scriptReader = null;
      try {
        String line;
        scriptReader = new BufferedReader(new UnicodeReader(scriptInputStream, "UTF8"));
        StringBuffer x = new StringBuffer();
        while ((line = scriptReader.readLine()) != null) {
          x.append(line).append("\n");
        }

        scriptRegion.println(x.toString(), scriptTag.getElementPosition(), scriptSrc, false);

      } finally {
        if (scriptReader != null) {
          scriptReader.close();
        }
      }
    }

    protected String getScriptName(URL url) throws MalformedURLException {
      String file = url.getFile();
      int lastIdxOfSlash = file.lastIndexOf('/');
      file = (lastIdxOfSlash == (-1)) ? file : file.substring(lastIdxOfSlash + 1);
      return file;
    }

    @Override
    public void writeToFinalRegion(SourceRegion finalRegion) {
      // wrapping the embedded scripts with a fake method of the window. Required for making this == window.
      finalRegion.println("window.__MAIN__ = function __WINDOW_MAIN__(){");

      finalRegion.write(scriptRegion);

      finalRegion.write(domRegion);

      finalRegion.println("  document.URL = new String(\"" + entrypointUrl + "\");");

      finalRegion.println("while (true){ ");
      finalRegion.write(entrypointRegion);
      finalRegion.println("} // while (true)");
        
      finalRegion.println("} // end of window.__MAIN__");
      finalRegion.println("window.__MAIN__();");
    }
  }

  /**
   * for storing the name of the temp file created by extractSources()
   */
  private File tempFile;
  
  @Override
  public Set<MappedSourceModule> extractSources(URL entrypointUrl, IHtmlParser htmlParser, IUrlResolver urlResolver)
  throws IOException, Error {

    InputStream inputStreamReader = WebUtil.getStream(entrypointUrl);
    IGeneratorCallback htmlCallback = createHtmlCallback(entrypointUrl, urlResolver); 
    htmlParser.parse(entrypointUrl, inputStreamReader, htmlCallback, entrypointUrl.getFile());

    SourceRegion finalRegion = new SourceRegion();
    htmlCallback.writeToFinalRegion(finalRegion);
    
    // writing the final region into one SourceFileModule.
    File outputFile = createOutputFile(entrypointUrl, DELETE_UPON_EXIT, USE_TEMP_NAME);
    tempFile = outputFile;
    FileMapping fileMapping = finalRegion.writeToFile(new PrintWriter(new FileWriter(outputFile)));
    if (fileMapping == null) {
      fileMapping = new EmptyFileMapping();
    }
    MappedSourceModule singleFileModule = new MappedSourceFileModule(outputFile, outputFile.getName(), fileMapping);
    return Collections.singleton(singleFileModule);
  }

  protected IGeneratorCallback createHtmlCallback(URL entrypointUrl, IUrlResolver urlResolver) {
    return new HtmlCallback(entrypointUrl, urlResolver);
  }

  private File createOutputFile(URL url, boolean delete, boolean useTempName) throws IOException {
    File outputFile;
    String fileName = new File(url.getFile()).getName();
    if (fileName.length() < 5) {
      fileName = "xxxx" + fileName; 
    }
    if (useTempName) {
      outputFile = File.createTempFile(fileName, ".js");
    } else {
      outputFile = new File(fileName);
    }
    if (outputFile.exists()){
      outputFile.delete();
    }
    if(delete){
      outputFile.deleteOnExit();
    }
    return outputFile;
  }   


  public static void main(String[] args) throws IOException, Error {
//    DomLessSourceExtractor domLessScopeGenerator = new DomLessSourceExtractor();
    JSSourceExtractor domLessScopeGenerator = new DefaultSourceExtractor();
    JSSourceExtractor.DELETE_UPON_EXIT = false;
    URL entrypointUrl = new URL(args[0]);
    IHtmlParser htmlParser = new JerichoHtmlParser();
    IUrlResolver urlResolver = new IdentityUrlResolver();
    Set<MappedSourceModule> res = domLessScopeGenerator.extractSources(entrypointUrl , htmlParser , urlResolver);
    MappedSourceModule entry = res.iterator().next();
    System.out.println(entry);
    System.out.println(entry.getMapping());
    
  }

  @Override
  public File getTempFile() {
    return tempFile;
  }
}

