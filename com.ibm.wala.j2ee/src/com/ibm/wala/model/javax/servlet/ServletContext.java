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

import javax.servlet.Servlet;

import com.ibm.wala.model.SyntheticFactory;

public class ServletContext implements javax.servlet.ServletContext {

  private static ServletContext singleton = new ServletContext();

  public static javax.servlet.ServletContext getModelInstance() {
    return singleton;
  }

  private static java.util.Hashtable<String, Object> values = new java.util.Hashtable<String, Object>();

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

  public javax.servlet.ServletContext getContext(String uripath) {
    return singleton;
  }

  public String getInitParameter(String name) {
    return ServletRequest.getInputString();
  }

  @SuppressWarnings("unchecked")
  public java.util.Enumeration getInitParameterNames() {
    return new java.util.Enumeration() {
      public boolean hasMoreElements() {
        return false;
      }

      public Object nextElement() {
        return null;
      }
    };
  }

  public int getMajorVersion() {
    return 2;
  }

  public String getMimeType(String file) {
    return null;
  }

  public int getMinorVersion() {
    return 3;
  }

  public javax.servlet.RequestDispatcher getNamedDispatcher(String name) {
    return new RequestDispatcher();
  }

  public String getRealPath(String path) {
    return path;
  }

  public javax.servlet.RequestDispatcher getRequestDispatcher(String path) {
    return new RequestDispatcher();
  }

  public java.net.URL getResource(String path) {
    try {
      return new java.net.URL(path);
    } catch (java.net.MalformedURLException e) {
      return null;
    }
  }

  public java.io.InputStream getResourceAsStream(String path) {
    return null;
  }

  public java.util.Set<Object> getResourcePaths(String path) {
    return java.util.Collections.emptySet();
  }

  public java.lang.String getServerInfo() {
    return "WALA J2EE model";
  }

  public Servlet getServlet(java.lang.String name) {
    return null;
  }

  public String getServletContextName() {
    return "WALA J2EE model ServletContext";
  }

  @SuppressWarnings("unchecked")
  public java.util.Enumeration getServletNames() {
    return null;
  }

  @SuppressWarnings("unchecked")
  public java.util.Enumeration getServlets() {
    return null;
  }

  public void log(Exception exception, String msg) {

  }

  public void log(String msg) {

  }

  public void log(String message, Throwable throwable) {

  }

  public void removeAttribute(String name) {
    values.remove(name);
  }

  public void setAttribute(String name, Object object) {
    values.put(name, object);
  }
}
