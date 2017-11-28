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
package com.ibm.wala.cast.js.html.jericho;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Set;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.js.html.IHtmlCallback;
import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.warnings.Warning;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Logger;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;


/**
 * @author danielk
 * Uses the Jericho parser to go over the HTML
 */
public class JerichoHtmlParser implements IHtmlParser{
    static Set<Warning> warnings = HashSetFactory.make();

    static{
      class CAstLoggerProvider implements LoggerProvider {
        @Override
        public Logger getLogger(String arg0) {
          class CAstLogger implements Logger {

            @Override
            public void debug(String arg0) {
              // TODO Auto-generated method stub
              
            }

            @Override
            public void error(final String arg0) {
              warnings.add(new Warning() {
                @Override
                public String getMsg() {
                  return arg0;
                }                
              });
            }

            @Override
            public void info(String arg0) {
              // TODO Auto-generated method stub
              
            }

            @Override
            public boolean isDebugEnabled() {
              return true;
            }

            @Override
            public boolean isErrorEnabled() {
              return true;
            }

            @Override
            public boolean isInfoEnabled() {
              return true;
            }

            @Override
            public boolean isWarnEnabled() {
              return true;
            }

            @Override
            public void warn(String arg0) {
              // TODO Auto-generated method stub
              
            }
            
          }
          
          return new CAstLogger();
        }
        
      }
      
      Config.LoggerProvider = new CAstLoggerProvider();
    }

	@Override
  public void parse(URL url, Reader reader, IHtmlCallback callback, String fileName) throws TranslatorToCAst.Error {
	  warnings.clear();
		Parser parser = new Parser(callback, fileName);
		Source src;
		try {
			src = new Source(reader);
			src.setLogger(Config.LoggerProvider.getLogger(fileName));
			List<Element> childElements = src.getChildElements();
			for (Element e : childElements) {
				parser.parse(e);
			}
			if (! warnings.isEmpty()) {
			  throw new TranslatorToCAst.Error(warnings);
			}
		} catch (IOException e) {
			System.err.println("Error parsing file: " + e.getMessage());
		}
	}
	/**
	 * @author danielk
	 * Inner class does the actual traversal of the HTML using recursion
	 */
	private static class Parser {
		private final IHtmlCallback handler;
		private final String fileName;

		public Parser(IHtmlCallback handler, String fileName) {
			this.handler = handler;
			this.fileName = fileName;
		}

		private void parse(Element root) {
			JerichoTag tag = new JerichoTag(root, fileName);
			handler.handleStartTag(tag);
			handler.handleText(tag.getElementPosition(), tag.getBodyText().snd);
			List<Element> childElements = root.getChildElements();
			for (Element child : childElements) {
				parse(child);
			}
			handler.handleEndTag(tag);
		}

	}

}
