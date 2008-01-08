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

import java.util.Enumeration;
import java.util.Vector;

/**
 * @author sfink
 *
 */
public class ServletConfig implements javax.servlet.ServletConfig {

  public String getServletName() {
    return "some name";
  }

  public javax.servlet.ServletContext getServletContext() {
    return com.ibm.wala.model.javax.servlet.ServletContext.getModelInstance();
  }

  public String getInitParameter(String arg0) {
    return ServletRequest.getInputString();
  }


  public Enumeration<String> getInitParameterNames() {
    Vector<String> v = new Vector<String>();
    v.add("a String");
    return v.elements();
  }

}
