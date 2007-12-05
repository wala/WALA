/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.model.javax.servlet.http;

import javax.servlet.http.Cookie;

public class HttpServletResponse
  extends com.ibm.wala.model.javax.servlet.ServletResponse
  implements javax.servlet.http.HttpServletResponse {
  @SuppressWarnings("unchecked")
  private final java.util.Hashtable<String, Comparable> headers = new java.util.Hashtable<String, Comparable>();

  public void addCookie(Cookie cookie) {

  }

  public void addDateHeader(String name, long date) {
    headers.put(name, new java.util.Date(date));
  }

  public void addHeader(String name, String value) {
    headers.put(name, value);
  }

  public void addIntHeader(java.lang.String name, int value) {
    headers.put(name, new Integer(value));
  }

  public boolean containsHeader(String name) {
    return headers.containsKey(name);
  }

  public String encodeRedirectUrl(String url) {
    return url;
  }

  public String encodeRedirectURL(String url) {
    return url;
  }

  public String encodeUrl(String url) {
    return url;
  }

  public String encodeURL(String url) {
    return url;
  }

  public void sendError(int sc) {

  }

  public void sendError(int sc, String msg) {

  }

  public void sendRedirect(String location) {

  }

  public void setDateHeader(String name, long date) {
    headers.put(name, new java.util.Date(date));
  }

  public void setHeader(String name, String value) {
    headers.put(name, value);
  }

  public void setIntHeader(String name, int value) {
    headers.put(name, new Integer(value));
  }

  public void setStatus(int sc) {

  }

  public void setStatus(int sc, String sm) {

  }

}
