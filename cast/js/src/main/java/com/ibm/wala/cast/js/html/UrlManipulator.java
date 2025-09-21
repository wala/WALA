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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class UrlManipulator {

  /**
   * @param urlFound the link as appear
   * @param context the URL in which the link appeared
   */
  public static URL relativeToAbsoluteUrl(String urlFound, URL context)
      throws MalformedURLException {
    urlFound = urlFound.replace("\\", "/").toLowerCase();

    try {
      return context.toURI().resolve(urlFound).toURL();
    } catch (final URISyntaxException problem) {
      throw new IllegalArgumentException(problem);
    }
  }
}
