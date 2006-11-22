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
package com.ibm.wala.util.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.ibm.wala.util.Atom;

/**
 *  Generic handler for quick-and-dirty parsing of simple XML files.
 *  Provides accessors for attributes, and simple error handling
 *  functions.
 */
public abstract class SimpleXMLHandler extends DefaultHandler {
  private Locator locator = null;
  private Attributes atts = null;

  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  protected abstract void element(String name) throws SAXException;

  public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
    this.atts = atts;
    element(name);
    this.atts = null;
  }
  protected String getString(String key) throws SAXException {
    int idx = atts.getIndex(key);
    if (idx == -1)
      throw new SAXException("Missing '" + key + "' attribute" + LN());
    return atts.getValue(idx);
  }
  protected Atom getAtom(String key) throws SAXException {
    return Atom.findOrCreateUnicodeAtom(getString(key));
  }
  protected Integer getInteger(String key) throws SAXException {
    try {
      return Integer.decode(getString(key));
    } catch (NumberFormatException e) {
      throw new SAXException(e.toString() + LN());
    }
  }
  protected String LN() {
    return ", line " + locator.getLineNumber();
  }

  ////////////////////////////////////////////////////////////////////////

  private String filename = "<unknown>";

  /**
   *  Parse XML specified by file name.
   */
  public void go(File xmlFile) throws IOException {
    filename = xmlFile.toString();
    InputStream s = new FileInputStream(xmlFile);
    parse(s);
    s.close();
  }

  /**
   *  Parse XML specified by resource name, w.r.t. classloader path.
   */
  public void go(String xmlFile) throws IOException {
    filename = xmlFile;
    InputStream s = getClass().getClassLoader().getResourceAsStream(xmlFile);
    if (s == null)
      throw new FileNotFoundException(xmlFile);
    parse(s);
    s.close();
  }

  /**
   *  Parse XML from specified InputStream.
   */
  public void parse(InputStream s) throws IOException {
    System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
    try {
      XMLReader xr = XMLReaderFactory.createXMLReader();
      xr.setContentHandler(this);
      xr.setErrorHandler(this);
      xr.parse(new InputSource(s));
    } catch (SAXException e) {
      throw new Error("Error while reading xml file: " + filename + "\n" + e.getMessage());
    } catch (IOException e) {
      throw new IOException("Error while reading xml file: " + filename + "\n" + e.getMessage());
    }
  }
}
