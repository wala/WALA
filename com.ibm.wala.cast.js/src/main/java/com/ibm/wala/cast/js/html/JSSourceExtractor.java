/*
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.html;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Set;

/**
 * Extracts scripts from a given URL of an HTML. Retrieves also attached js files. Provides file and
 * line mapping for each extracted SourceFileModule back to the original file and line number.
 *
 * @author yinnonh
 * @author danielk
 */
public abstract class JSSourceExtractor {

  public static boolean DELETE_UPON_EXIT = true;

  public static boolean USE_TEMP_NAME = true;

  /**
   * Returns the temporary file created by a call to {@link #extractSources(URL, IHtmlParser,
   * IUrlResolver, Reader)} which holds all the discovered JS source. If no such file exists,
   * returns {@code null}
   */
  public abstract File getTempFile();

  public Set<MappedSourceModule> extractSources(
      URL entrypointUrl, IHtmlParser htmlParser, IUrlResolver urlResolver)
      throws IOException, Error {
    try (Reader r = WebUtil.getStream(entrypointUrl)) {
      return extractSources(entrypointUrl, htmlParser, urlResolver, r);
    }
  }

  public abstract Set<MappedSourceModule> extractSources(
      URL entrypointUrl, IHtmlParser htmlParser, IUrlResolver urlResolver, Reader reader)
      throws IOException, Error;
}
