/*
 * Created on Sep 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.ibm.wala.model.javax.servlet.http;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;

import com.ibm.wala.model.SyntheticFactory;

/**
 * @author sfink
 *
 * TODO: finish this model
 */
public class HttpSession implements javax.servlet.http.HttpSession {

  private static final HttpSession singleton = new HttpSession();

  public static javax.servlet.http.HttpSession getModelInstance() {
      return singleton;
  }

  private Hashtable<String, Object> attributes = new Hashtable<String, Object>();
  private Hashtable<String, Object> values = new Hashtable<String, Object>();
  
  /**
   * 
   */
  public HttpSession() {
    super();
    // TODO Auto-generated constructor stub
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getCreationTime()
   */
  public long getCreationTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getId()
   */
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getLastAccessedTime()
   */
  public long getLastAccessedTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getServletContext()
   */
  public ServletContext getServletContext() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
   */
  public void setMaxInactiveInterval(int arg0) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
   */
  public int getMaxInactiveInterval() {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getSessionContext()
   */
  public HttpSessionContext getSessionContext() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
   */
  public Object getAttribute(String arg0) {
    if (arg0.length() > 5) {  // random condition.
      return attributes.get(arg0);
    } else {
      return SyntheticFactory.getObject();
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
   */
  public Object getValue(String arg0) {
    if (arg0.length() > 5) {  // random condition.
      return values.get( arg0 );
    } else {
      return SyntheticFactory.getObject();
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getAttributeNames()
   */
  public Enumeration<String> getAttributeNames() {
    return attributes.keys();
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#getValueNames()
   */
  public String[] getValueNames() {
    return new String[0];
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
   */
  public void setAttribute(String arg0, Object arg1) {
    attributes.put(arg0, arg1);
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
   */
  public void putValue(String arg0, Object arg1) {
      values.put(arg0, arg1);
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
   */
  public void removeAttribute(String arg0) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
   */
  public void removeValue(String arg0) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#invalidate()
   */
  public void invalidate() {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSession#isNew()
   */
  public boolean isNew() {
    // TODO Auto-generated method stub
    return false;
  }

}
