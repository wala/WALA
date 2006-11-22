/*
 * Created on Mar 3, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.wala.model.javax.servlet;

import java.util.Enumeration;
import java.util.Vector;

/**
 * @author sfink
 *
 */
public class ServletConfig implements javax.servlet.ServletConfig {

  /* (non-Javadoc)
   * @see javax.servlet.ServletConfig#getServletName()
   */
  public String getServletName() {
    return "some name";
  }

  /* (non-Javadoc)
   * @see javax.servlet.ServletConfig#getServletContext()
   */
  public javax.servlet.ServletContext getServletContext() {
    return com.ibm.wala.model.javax.servlet.ServletContext.getModelInstance();
  }

  /* (non-Javadoc)
   * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
   */
  public String getInitParameter(String arg0) {
    return new String("init parameter");
  }

  /* (non-Javadoc)
   * @see javax.servlet.ServletConfig#getInitParameterNames()
   */
  public Enumeration<String> getInitParameterNames() {
    Vector<String> v = new Vector<String>();
    v.add("a String");
    return v.elements();
  }

}
