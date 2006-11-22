package com.ibm.wala.model.javax.servlet;

import javax.servlet.*;

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
}

