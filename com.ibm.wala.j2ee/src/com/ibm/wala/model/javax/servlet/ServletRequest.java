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
package com.ibm.wala.model.javax.servlet;

import java.io.IOException;

import javax.servlet.ServletInputStream;

import com.ibm.wala.model.SyntheticFactory;

public class ServletRequest implements javax.servlet.ServletRequest {

  private final java.util.Hashtable<String, Object> values = new java.util.Hashtable<String, Object>();

  private java.util.Hashtable<String, String> parameters;

  private String encoding = "iso88591-1";

  // lazily initialize bogus model of parameters
  private void initParameters() {
    parameters = new java.util.Hashtable<String, String>();
    parameters.put(getInputString(), getInputString());
  }

  /**
   * The semantics of this are bogus ... be careful to hijack this.
   */
  protected static String getInputString() {
    return "some input string";
  }

  /**
   * The semantics of this are bogus ... be careful to hijack this.
   */
  public ServletRequest() {
  }

  public Object getAttribute(String name) {
    if (name.length() > 5) { // random condition.
      return values.get(name);
    } else {
      return SyntheticFactory.getObject();
    }
  }

  public java.util.Enumeration<String> getAttributeNames() {
    return values.keys();
  }

  public java.lang.String getCharacterEncoding() {
    return encoding;
  }

  public int getContentLength() {
    return -1;
  }

  public java.lang.String getContentType() {
    return getInputString();
  }

  public javax.servlet.ServletInputStream getInputStream() {
    return new ServletInputStream() {
      @Override
      public int read() throws IOException {
        // kind of a bogus model, but ok for now ...
        String s = getInputString();
        int n = s.charAt(0);
        return n;
      }

    };
  }

  public java.util.Locale getLocale() {
    return java.util.Locale.getDefault();
  }

  @SuppressWarnings("unchecked")
  public java.util.Enumeration getLocales() {
    return new java.util.Enumeration() {
      private boolean done = false;

      public boolean hasMoreElements() {
        return !done;
      }

      public Object nextElement() {
        done = true;
        return getLocale();
      }
    };
  }

  public String getParameter(String name) {
    // Note, while the following is technically a more accurate model...
    // return parameters.get(name);
    // I'm using the following simpler model for now to avoid some unnecessary analysis
    // of flow through the heap. Revisit this decision if necessary.
    return getInputString();
  }

  public java.util.Map<String, String> getParameterMap() {
    initParameters();
    return parameters;
  }

  public java.util.Enumeration<String> getParameterNames() {
    initParameters();
    return parameters.keys();
  }

  public String[] getParameterValues(String name) {
    initParameters();
    return new String[] { parameters.get(name) };
  }

  public java.lang.String getProtocol() {
    return getInputString();
  }

  public java.io.BufferedReader getReader() {
    return null;
  }

  public String getRealPath(String path) {
    return path;
  }

  public String getRemoteAddr() {
    return "0.0.0.0";
  }

  public String getRemoteHost() {
    return "remotehost";
  }

  public javax.servlet.RequestDispatcher getRequestDispatcher(String path) {
    return new RequestDispatcher();
  }

  public String getScheme() {
    return getInputString();
  }

  public String getServerName() {
    return "localhost.localdomain";
  }

  public int getServerPort() {
    return 80;
  }

  public boolean isSecure() {
    return false;
  }

  public void removeAttribute(String name) {
    values.remove(name);
  }

  public void setAttribute(String name, Object o) {
    values.put(name, o);
  }

  public void setCharacterEncoding(String env) {
    encoding = env;
  }

  public String getLocalAddr() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getLocalName() {
    // TODO Auto-generated method stub
    return null;
  }

  public int getLocalPort() {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getRemotePort() {
    // TODO Auto-generated method stub
    return 0;
  }
}
