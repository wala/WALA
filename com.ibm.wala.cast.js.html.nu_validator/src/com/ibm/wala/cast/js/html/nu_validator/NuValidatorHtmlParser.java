/******************************************************************************
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.html.nu_validator;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.ibm.wala.cast.js.html.IHtmlCallback;
import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.ITag;

public class NuValidatorHtmlParser implements IHtmlParser {

  public void parse(final InputStream reader, final IHtmlCallback handler, String fileName) {
    HtmlParser parser = new HtmlParser();
    parser.setXmlPolicy(XmlViolationPolicy.ALLOW);
    parser.setContentHandler(new ContentHandler() {
      private Locator locator;
      private Stack<ITag> tags = new Stack<ITag>();;
      
      private int countLines(char[] ch, int start, int length) {
        LineNumberReader r = new LineNumberReader(new StringReader (new String(ch, start, length)));  
        try {
          while (r.read() > -1);
        } catch (IOException e) {
          throw new RuntimeException("cannot read from string", e);
        }
        return r.getLineNumber();
      }
      
      public void setDocumentLocator(Locator locator) {
        this.locator = locator;
      }

      public void startElement(String uri, final String localName, String qName, final Attributes atts) throws SAXException {
        final int line = locator.getLineNumber();
        tags.push(new ITag() {

          public String getName() {
             return localName;
          }

          public String getAttributeByName(String name) {
            return atts.getValue(name);
          }

          public Map<String, String> getAllAttributes() {
            return new AbstractMap<String,String>() {
              private Set<Map.Entry<String,String>> es = null;
             
              @Override
              public Set<java.util.Map.Entry<String, String>> entrySet() {
                if (es == null) {
                  es = new HashSet<Map.Entry<String,String>>();
                  for(int i = 0; i < atts.getLength(); i++) {
                    final int index = i;
                    es.add(new Map.Entry<String,String>() {

                      public String getKey() {
                        return atts.getLocalName(index);
                      }

                      public String getValue() {
                        return atts.getValue(index);
                      }

                      public String setValue(String value) {
                        throw new UnsupportedOperationException();
                      }
                    });
                  }
                }
                return es;
              }
             };
          }

          public int getStartingLineNum() {
            return line;
          }
        });
        handler.handleStartTag(tags.peek());
      }

      public void endElement(String uri, String localName, String qName) throws SAXException {
        handler.handleEndTag(tags.pop());
      }

      public void characters(char[] ch, int start, int length) throws SAXException {
        handler.handleText(locator.getLineNumber() - countLines(ch, start, length), new String(ch, start, length));
      }

      public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        handler.handleText(locator.getLineNumber(), new String(ch, start, length));
      }

      public void startDocument() throws SAXException {
        // do nothing
      }

      public void endDocument() throws SAXException {
        // do nothing
      }

      public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // do nothing
      }

      public void endPrefixMapping(String prefix) throws SAXException {
        // do nothing
      }

      public void processingInstruction(String target, String data) throws SAXException {
        // do nothing
      }

      public void skippedEntity(String name) throws SAXException {
        // do nothing
      }
      
    });
    
    try {
      parser.parse(new InputSource(new InputStream() {
        @Override
        public int read() throws IOException {
          int v;
          do {
            v = reader.read();
          } while (v == '\r');
          return v;
        }
      }));
    } catch (IOException e) {
      assert false : e.toString();
    } catch (SAXException e) {
      assert false : e.toString();
    }
  }
}
