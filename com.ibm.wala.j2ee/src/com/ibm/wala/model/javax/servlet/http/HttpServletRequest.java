package com.ibm.wala.model.javax.servlet.http;

import javax.servlet.http.Cookie;

import com.ibm.wala.annotations.Internal;

@Internal
public class HttpServletRequest
  extends com.ibm.wala.model.javax.servlet.ServletRequest
  implements javax.servlet.http.HttpServletRequest {
  private final java.util.Hashtable<String, String> headers = new java.util.Hashtable<String, String>();

  public HttpServletRequest() {
    headers.put("header", "value");
    headers.get("header");
  }

  public String getAuthType() {
    return "none";
  }

  public String getContextPath() {
    return "this/that/the/other";
  }

  public Cookie[] getCookies() {
    return null;
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
    return "?stuff";
  }

  public String getRemoteUser() {
    return null;
  }

  public String getRequestedSessionId() {
    return "SESSION1";
  }

  public String getRequestURI() {
    return "this/that/the/other";
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
