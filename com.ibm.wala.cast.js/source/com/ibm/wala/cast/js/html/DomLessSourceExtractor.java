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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

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

  public static Supplier<JSSourceExtractor> factory = DomLessSourceExtractor::new;

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
    private ITag currentCommentTag;
    
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
 
    protected void writeEntrypoint(String ep) {
      entrypointRegion.println(ep);
    }
    
    protected void addDefaultHandlerInvocations() {
      // always invoke window.onload
      writeEntrypoint("window.onload();");
    }

    protected Position makePos(ITag governingTag) {
      return governingTag.getElementPosition();
     }
    
    @Override
    public void handleEndTag(ITag tag) {
      if (tag.getName().equalsIgnoreCase("script")) {
        assert currentScriptTag != null;
        currentScriptTag = null;
      } else if (currentScriptTag != null && tag.getName().equals("!--")) {
        assert currentCommentTag != null;
        currentCommentTag = null;
      }
    }

    @Override
    public void handleText(Position p, String text) {
      if (currentScriptTag != null && currentCommentTag == null) {
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
      } else if (currentScriptTag != null && tag.getName().equals("!--")){
        currentCommentTag = tag;
      }
      handleDOM(tag);
    }

    private static boolean isUsableIdentifier(String x) {
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
        writeEntrypoint("\t" + fName + "(null);", pos, url, true);
      }
    }

    protected void writeEntrypoint(String text, Position pos, URL url, boolean b) {
      entrypointRegion.println(text, pos, url, b);
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
    
    private static String extructJS(String attValue) {
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
      URL scriptSrc = new URL(entrypointUrl, urlAsString);
      BOMInputStream bs;
      try {
        bs = new BOMInputStream(scriptSrc.openConnection().getInputStream(), false,
            ByteOrderMark.UTF_8, 
            ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE,
            ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
        if (bs.hasBOM()) {
          System.err.println("removing BOM " + bs.getBOM());
        }
      } catch (Exception e) {
        //it looks like this happens when we can't resolve the url?
        if (DEBUG) {
          System.err.println("Error reading script: " + scriptSrc);
          System.err.println(e);
          e.printStackTrace(System.err);
        }
        return;
      }
      try (
        final Reader scriptInputStream = new InputStreamReader(bs);
        final BufferedReader scriptReader = new BufferedReader(scriptInputStream);
        ) {
        String line;
        StringBuffer x = new StringBuffer();
        while ((line = scriptReader.readLine()) != null) {
          x.append(line).append("\n");
        }

        scriptRegion.println(x.toString(), scriptTag.getElementPosition(), scriptSrc, false);
      }
    }

    protected String getScriptName(URL url) {
      String file = url.getFile();
      int lastIdxOfSlash = file.lastIndexOf('/');
      file = (lastIdxOfSlash == (-1)) ? file : file.substring(lastIdxOfSlash + 1);
      return file;
    }

    protected void writeEventLoopHeader(SourceRegion finalRegion) {
      finalRegion.println("while (true){  // event loop model");      
    }
    
    @Override
    public void writeToFinalRegion(SourceRegion finalRegion) {
      // wrapping the embedded scripts with a fake method of the window. Required for making this == window.
      finalRegion.println("window.__MAIN__ = function __WINDOW_MAIN__(){");

      finalRegion.write(scriptRegion);

      finalRegion.write(domRegion);

      finalRegion.println("  document.URL = new String(\"" + entrypointUrl + "\");");

      writeEventLoopHeader(finalRegion);
      finalRegion.write(entrypointRegion);
      finalRegion.println("} // event loop model");
        
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

    IGeneratorCallback htmlCallback;
    try (Reader inputStreamReader = WebUtil.getStream(entrypointUrl)) {
      htmlCallback = createHtmlCallback(entrypointUrl, urlResolver);
      htmlParser.parse(entrypointUrl, inputStreamReader, htmlCallback, entrypointUrl.getFile());
    }

    SourceRegion finalRegion = new SourceRegion();
    htmlCallback.writeToFinalRegion(finalRegion);
    
    // writing the final region into one SourceFileModule.
    File outputFile = createOutputFile(entrypointUrl, DELETE_UPON_EXIT, USE_TEMP_NAME);
    tempFile = outputFile;
    FileMapping fileMapping;
    try (final PrintWriter printer = new PrintWriter(new FileWriter(outputFile))) {
      fileMapping = finalRegion.writeToFile(printer);
    }
    if (fileMapping == null) {
      fileMapping = new EmptyFileMapping();
    }
    MappedSourceModule singleFileModule = new MappedSourceFileModule(outputFile, outputFile.getName(), fileMapping);
    return Collections.singleton(singleFileModule);
  }

  protected IGeneratorCallback createHtmlCallback(URL entrypointUrl, IUrlResolver urlResolver) {
    return new HtmlCallback(entrypointUrl, urlResolver);
  }

  private static File createOutputFile(URL url, boolean delete, boolean useTempName) throws IOException {
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

