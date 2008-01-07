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

import com.ibm.wala.annotations.Internal;

@Internal
public class HttpServletRequest
  extends com.ibm.wala.model.javax.servlet.ServletRequest
  implements javax.servlet.http.HttpServletRequest {
  private final java.util.Hashtable<String, String> headers = new java.util.Hashtable<String, String>();

  public HttpServletRequest() {
    super();
    headers.put(getInputString(), getInputString());
  }

  public String getAuthType() {
    return getInputString();
  }

  public String getContextPath() {
    return "this/that/the/other";
  }

  public Cookie[] getCookies() {
    Cookie cookie = new Cookie(getInputString(), getInputString());
    cookie.setComment(getInputString());
    return new Cookie[] { cookie } ;
    
  }

  public long getDateHeader(String name) {
    return 0;
  }

  public String getHeader(String name) {
    return headers.get(name);
  }

  public java.util.Enumeration<String> getHeaderNames() {
    return headers.keys();
  }

  public java.util.Enumeration<String> getHeaders(String name) {
    return headers.elements();
  }

  public int getIntHeader(String name) {
    return (new Integer(getHeader(name))).intValue();
  }

  public String getMethod() {
    return "GET";
  }

  public java.lang.String getPathInfo() {
    return null;
  }

  public String getPathTranslated() {
    return "path";
  }

  public String getQueryString() {
    return getInputString();
  }

  public String getRemoteUser() {
    return getInputString();
  }

  public String getRequestedSessionId() {
    return "SESSION1";
  }

  public String getRequestURI() {
    return getInputString();
  }

  public StringBuffer getRequestURL() {
    return new StringBuffer(getRequestURI());
  }

  public String getServletPath() {
    return "this/that/the/other";
  }

  public javax.servlet.http.HttpSession getSession() {
    return HttpSession.getModelInstance();
  }

  public javax.servlet.http.HttpSession getSession(boolean create) {
    return HttpSession.getModelInstance();
  }

  public java.security.Principal getUserPrincipal() {
    return null;
  }

  public boolean isRequestedSessionIdFromCookie() {
    return true;
  }

  public boolean isRequestedSessionIdFromUrl() {
    return true;
  }

  public boolean isRequestedSessionIdFromURL() {
    return true;
  }

  public boolean isRequestedSessionIdValid() {
    return true;
  }

  public boolean isUserInRole(String role) {
    return true;
  }
}
