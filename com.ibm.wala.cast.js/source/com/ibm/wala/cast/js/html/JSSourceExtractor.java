package com.ibm.wala.cast.js.html;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.ibm.wala.classLoader.SourceFileModule;

/**
 * Extracts scripts from a given URL of an HTML. Retrieves also attached js files. 
 * Provides file and line mapping for each extracted SourceFileModule back to the original file and line number.
 * 
 * @author yinnonh
 * @author danielk
 */
public interface JSSourceExtractor {

  public Map<SourceFileModule, FileMapping> extractSources(URL entrypointUrl, IHtmlParser htmlParser, IUrlResolver urlResolver) throws IOException;
  
}
