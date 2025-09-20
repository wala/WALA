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
import java.net.URI;
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

    URI absoluteUri;
    if (!isAbsoluteUrl(urlFound))
      try {
        if (urlFound.startsWith("//")) {
          // create URL taking only the protocol from the context
          String origHostAndPath = urlFound.substring(2); // removing "//"
          String host;
          String path;
          int indexOf = origHostAndPath.indexOf('/');
          if (indexOf > 0) {
            host = origHostAndPath.substring(0, indexOf);
            path = origHostAndPath.substring(indexOf);
          } else {
            host = origHostAndPath;
            path = "";
          }
          absoluteUri = new URI(context.getProtocol(), host, path, null);
        } else if (urlFound.startsWith("/")) {
          // create URL taking the protocol and the host from the context
          absoluteUri = new URI(context.getProtocol(), context.getHost(), urlFound, null);
        } else {
          // "concat" URL to context
          int backDir = 0; // removing directories due to "../"
          while (urlFound.startsWith("../")) {
            urlFound = urlFound.substring(3);
            backDir++;
          }
          StringBuilder contextPath = new StringBuilder();
          String path = context.getPath().replace("\\", "/");
          boolean isContextDirectory = path.endsWith("/");
          String[] split = path.split("/");
          // we are also removing last element in case of a directory
          int rightTrimFromPath = (isContextDirectory ? 0 : 1) + backDir;

          for (int i = 0; i < split.length - rightTrimFromPath; i++) {
            contextPath.append(split[i]);
            contextPath.append('/');
          }
          absoluteUri =
              new URI(context.getProtocol(), context.getHost(), contextPath + urlFound, null);
        }
      } catch (URISyntaxException problem) {
        throw new IllegalArgumentException(problem);
      }
    else {
      absoluteUri = URI.create(urlFound);
    }
    return absoluteUri.toURL();
  }

  private static boolean isAbsoluteUrl(String orig) {
    return orig.startsWith("http");
  }
}
