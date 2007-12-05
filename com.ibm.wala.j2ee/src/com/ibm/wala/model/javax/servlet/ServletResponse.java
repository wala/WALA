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

import javax.servlet.ServletOutputStream;

public class ServletResponse implements javax.servlet.ServletResponse {
    private java.util.Locale locale = java.util.Locale.getDefault();;

    private int bufferSize = -1;

    private final ServletOutputStream out = new ServletOutputStream() {
        public void write (int b) throws java.io.IOException {
	    
	}
    };

    public void flushBuffer() {

    }
    
    public int getBufferSize() {
	return bufferSize;
    }

    public String getCharacterEncoding() {
	return "iso8859-1";
    }

    public java.util.Locale getLocale() {
	return locale;
    }

    public ServletOutputStream getOutputStream() {
	return out;
    }

    public java.io.PrintWriter getWriter() {
	return new java.io.PrintWriter( out );
    }

    public boolean isCommitted() {
	return false;
    }

    public void reset() {

    }

    public void resetBuffer() {

    }

    public void setBufferSize(int size) {
	bufferSize = size;
    }

    public void setContentLength(int len) {
	
    }
     
    public void setContentType(java.lang.String type) {

    }

    public void setLocale(java.util.Locale loc) {
	locale = loc;
    }

    public String getContentType() {
      // TODO Auto-generated method stub
      return null;
    }

    public void setCharacterEncoding(String charset) {
      // TODO Auto-generated method stub
      
    }
}

