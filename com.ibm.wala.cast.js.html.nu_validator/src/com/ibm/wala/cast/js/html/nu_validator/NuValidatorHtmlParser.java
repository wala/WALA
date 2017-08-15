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
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.ibm.wala.cast.js.html.IHtmlCallback;
import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.ITag;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.LineNumberPosition;
import com.ibm.wala.util.collections.Pair;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;

public class NuValidatorHtmlParser implements IHtmlParser {

  @Override
  public void parse(final URL url, final Reader reader, final IHtmlCallback handler, final String fileName) {
    URL xx = null;
	try {
		xx = new URL("file://" + fileName);
	} catch (MalformedURLException e1) {
		e1.printStackTrace();
	}
    final URL localFileName = xx;

    HtmlParser parser = new HtmlParser();
    parser.setXmlPolicy(XmlViolationPolicy.ALLOW);
    parser.setContentHandler(new ContentHandler() {
      private Locator locator;
      private Stack<ITag> tags = new Stack<>();
      
      private int countLines(char[] ch, int start, int length) {
        LineNumberReader r = new LineNumberReader(new StringReader (new String(ch, start, length)));  
        try {
          while (r.read() > -1);
        } catch (IOException e) {
          throw new RuntimeException("cannot read from string", e);
        }
        return r.getLineNumber();
      }
      
      @Override
      public void setDocumentLocator(Locator locator) {
        this.locator = locator;
      }

      @Override
      public void startElement(String uri, final String localName, String qName, final Attributes atts) throws SAXException {
        final Position line = new LineNumberPosition(url, localFileName, locator.getLineNumber());
        tags.push(new ITag() {

          @Override
          public String getName() {
             return localName;
          }

          @Override
          public Pair<String,Position> getAttributeByName(String name) {
        	  if (atts.getValue(name) != null) {
        		  return Pair.make(atts.getValue(name), line);
        	  } else {
        		  return null;
        	  }
          }

          @Override
          public Map<String, Pair<String,Position>> getAllAttributes() {
            return new AbstractMap<String,Pair<String,Position>>() {
              private Set<Map.Entry<String,Pair<String,Position>>> es = null;
             
              @Override
              public Set<java.util.Map.Entry<String, Pair<String,Position>>> entrySet() {
                if (es == null) {
                  es = new HashSet<>();
                  for(int i = 0; i < atts.getLength(); i++) {
                    final int index = i;
                    es.add(new Map.Entry<String,Pair<String,Position>>() {

                      @Override
                      public String getKey() {
                        return atts.getLocalName(index).toLowerCase();
                      }

                      @Override
                      public Pair<String,Position> getValue() {
                    	  if (atts.getValue(index) != null) {
                    		  return Pair.make(atts.getValue(index), line);
                    	  } else {
                    		  return null;
                    	  }
                      }

                      @Override
                      public Pair<String,Position> setValue(Pair<String,Position> value) {
                        throw new UnsupportedOperationException();
                      }
                    });
                  }
                }
                return es;
              }
             };
          }

          @Override
          public Position getElementPosition() {
            return line;
          }

          @Override
          public Position getContentPosition() {
              return line;
            }
        });
        
        handler.handleStartTag(tags.peek());
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
        handler.handleEndTag(tags.pop());
      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException {
        handler.handleText(new LineNumberPosition(url, localFileName, locator.getLineNumber() - countLines(ch, start, length)), new String(ch, start, length));
      }

      @Override
      public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        handler.handleText(new LineNumberPosition(url, localFileName, locator.getLineNumber()), new String(ch, start, length));
      }

      @Override
      public void startDocument() throws SAXException {
        // do nothing
      }

      @Override
      public void endDocument() throws SAXException {
        // do nothing
      }

      @Override
      public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // do nothing
      }

      @Override
      public void endPrefixMapping(String prefix) throws SAXException {
        // do nothing
      }

      @Override
      public void processingInstruction(String target, String data) throws SAXException {
        // do nothing
      }

      @Override
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
