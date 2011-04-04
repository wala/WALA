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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;


public class DomLessSourceExtractor extends JSSourceExtractor {
  private static final Pattern LEGAL_JS_IDENTIFIER_REGEXP = Pattern.compile("[a-zA-Z$_][a-zA-Z\\d$_]*");
  interface IGeneratorCallback extends IHtmlCallback {
    void writeToFinalRegion(SourceRegion finalRegion);
  }
  
  protected static class HtmlCallback implements IGeneratorCallback{
    protected final URL entrypointUrl;
    protected final IUrlResolver urlResolver;

    protected final SourceRegion scriptRegion;
    protected final SourceRegion domRegion;
    protected final SourceRegion entrypointRegion;
    
    private boolean inScriptRegion = false;
    
    private int counter = 0;
 
    public HtmlCallback(URL entrypointUrl, IUrlResolver urlResolver) {
      this.entrypointUrl = entrypointUrl;
      this.urlResolver  = urlResolver;
      this.scriptRegion = new SourceRegion();
      this.domRegion = new SourceRegion();
      this.entrypointRegion = new SourceRegion();
    }
    
    //Do nothing
    public void handleEndTag(ITag tag) {
      if (tag.getName().equalsIgnoreCase("script")) {
        assert inScriptRegion;
        inScriptRegion = false;
      }
    }

    
    public void handleText(int lineNumber, String text) {
      if (inScriptRegion) {
        scriptRegion.println(text, entrypointUrl, lineNumber);
      }
    }

    public void handleStartTag(ITag tag) {
      if (tag.getName().equalsIgnoreCase("script")) {
        handleScript(tag);
        assert !inScriptRegion;
        inScriptRegion = true;
      }
      handleDOM(tag);
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
      String idAttribute = tag.getAttributeByName("id");
      String funcName;
      if (idAttribute != null && LEGAL_JS_IDENTIFIER_REGEXP.matcher(idAttribute).matches()) {
        funcName = idAttribute;
      } else {
        funcName = "node" + (counter++);
      }
      handleDOM(tag, funcName);
    }

    protected void handleDOM(ITag tag, String funcName) {
      Map<String, String> attributeSet = tag.getAllAttributes();
      for (Entry<String, String> a : attributeSet.entrySet()) {
        handleAttribute(a, funcName, tag.getStartingLineNum());
      }
    }

    private void handleAttribute(Entry<String, String> a, String funcName, Integer lineNum) {
      String attName = a.getKey();
      String attValue = a.getValue();
      if (attName.toLowerCase().startsWith("on") || (attValue != null && attValue.toLowerCase().startsWith("javascript:"))) {
        String fName = attName + "_" + funcName;
        String signatureLine = "function " + fName + "(event) {";
        domRegion.println(signatureLine, entrypointUrl, lineNum);// Defines the function
        int offset = 0;
        for (String eventContentLine : extructJS(attValue)){
          domRegion.println("\t" + eventContentLine, entrypointUrl, lineNum + (offset++));
        }
        domRegion.println("}", entrypointUrl, lineNum);// Defines the function

        entrypointRegion.println("\t" + fName + "(null);", entrypointUrl, lineNum);// Run it
      }
    }

    private String[] extructJS(String attValue) {
      if (attValue == null){
        return new String[] {};
      }
      String content;
      if (attValue.toLowerCase().equals("javascript:")) {
        content = attValue.substring("javascript:".length());
      } else {
        content = attValue;
      }

      return content.split("\\n");
    }

    protected void handleScript(ITag tag) {

      String value = tag.getAttributeByName("src");

      try {
        if (value != null) {
          // script is out-of-line
          getScriptFromUrl(value);
        }

      } catch (IOException e) {
        System.err.println("Error reading script file: " + e.getMessage());
      }
    }

    private void getScriptFromUrl(String urlAsString) throws IOException, MalformedURLException {
      URL absoluteUrl = UrlManipulator.relativeToAbsoluteUrl(urlAsString, this.entrypointUrl);
      URL scriptSrc = urlResolver.resolve(absoluteUrl);
      if (scriptSrc == null) { //Error resolving URL
        return;
      }

      InputStream scriptInputStream = scriptSrc.openConnection().getInputStream();
      try{
        int lineNum = 1;
        String line;
        BufferedReader scriptReader = new BufferedReader(new UnicodeReader(scriptInputStream, "UTF8"));
        
        while ((line = scriptReader.readLine()) != null) {
          scriptRegion.println(line, scriptSrc, lineNum++);
        }
      } finally {
        scriptInputStream.close();
      }
    }

    protected String getScriptName(URL url) throws MalformedURLException {
      String file = url.getFile();
      int lastIdxOfSlash = file.lastIndexOf('/');
      file = (lastIdxOfSlash == (-1)) ? file : file.substring(lastIdxOfSlash + 1);
      return file;
    }

    public void writeToFinalRegion(SourceRegion finalRegion) {
      finalRegion.println("document.URL = new String(\"" + entrypointUrl + "\");");

      // wrapping the embedded scripts with a fake method of the window. Required for making this == window.
      finalRegion.println("window.__MAIN__ = function __WINDOW_MAIN__(){");
      finalRegion.write(scriptRegion);

      finalRegion.println("while (true){ ");
      finalRegion.write(entrypointRegion);
      finalRegion.println("} // while (true)");
      
      finalRegion.println("} // end of window.__MAIN__");
      finalRegion.println("window.__MAIN__();");
  
      finalRegion.write(domRegion);
    }
  }

  public Set<MappedSourceModule> extractSources(URL entrypointUrl, IHtmlParser htmlParser, IUrlResolver urlResolver)
  throws IOException {

    InputStream inputStreamReader = getStream(entrypointUrl);
    IGeneratorCallback htmlCallback = createHtmlCallback(entrypointUrl, urlResolver); 
    htmlParser.parse(inputStreamReader, htmlCallback, entrypointUrl.getFile());

    SourceRegion finalRegion = new SourceRegion();
    htmlCallback.writeToFinalRegion(finalRegion);
    
    // writing the final region into one SourceFileModule.
    File outputFile = createOutputFile(entrypointUrl, DELETE_UPON_EXIT, USE_TEMP_NAME);
    FileMapping fileMapping = finalRegion.writeToFile(new PrintStream(outputFile));
    MappedSourceModule singleFileModule = new MappedSourceFileModule(outputFile, outputFile.getName(), fileMapping);
    return Collections.singleton(singleFileModule);
  }

  protected IGeneratorCallback createHtmlCallback(URL entrypointUrl, IUrlResolver urlResolver) {
    return new HtmlCallback(entrypointUrl, urlResolver);
  }

  private File createOutputFile(URL url, boolean delete, boolean useTempName) throws IOException {
    File outputFile;
    if (useTempName) {
      outputFile = File.createTempFile(new File(url.getFile()).getName(), ".js");
    } else {
      outputFile = new File(new File(url.getFile()).getName());
    }
    if (outputFile.exists()){
      outputFile.delete();
    }
    if(delete){
      outputFile.deleteOnExit();
    }
    return outputFile;
  }   


  private InputStream getStream(URL url) throws IOException {
    URLConnection conn = url.openConnection();
    conn.setDefaultUseCaches(false);
    conn.setUseCaches(false);

    return conn.getInputStream();
  }
  
  public static void main(String[] args) throws IOException {
//    DomLessSourceExtractor domLessScopeGenerator = new DomLessSourceExtractor();
    JSSourceExtractor domLessScopeGenerator = new DefaultSourceExtractor();
    JSSourceExtractor.DELETE_UPON_EXIT = false;
    URL entrypointUrl = new URL(args[0]);
    IHtmlParser htmlParser = new JerichoHtmlParser();
    IUrlResolver urlResolver = new IdentityUrlResolver();
    Set<MappedSourceModule> res = domLessScopeGenerator.extractSources(entrypointUrl , htmlParser , urlResolver);
    MappedSourceModule entry = res.iterator().next();
    System.out.println(entry);
    entry.getMapping().dump(System.out);
    
  }
}

