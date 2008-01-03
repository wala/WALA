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
  private final java.util.Hashtable<String, String> parameters = new java.util.Hashtable<String, String>();
  private String encoding = "iso88591-1";

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
    parameters.put(getInputString(), getInputString());
    parameters.put(getInputString(), getInputString());
    parameters.put(getInputString(), getInputString());
    parameters.put(getInputString(), getInputString());
    parameters.put(getInputString(), getInputString());
  }

  public Object getAttribute(String name) {
    if (name.length() > 5) {  // random condition.
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
    return null;
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
    return parameters.get(name);
  }

  public java.util.Map<String, String> getParameterMap() {
    return parameters;
  }

  public java.util.Enumeration<String> getParameterNames() {
    return parameters.keys();
  }

  public String[] getParameterValues(String name) {
    return new String[] {parameters.get(name)};
  }

  public java.lang.String getProtocol() {
    return "HTTP/1.1";
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
    return "http";
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
