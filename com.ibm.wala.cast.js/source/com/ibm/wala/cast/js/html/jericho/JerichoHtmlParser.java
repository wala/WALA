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
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;

import com.ibm.wala.cast.js.html.IHtmlCallback;
import com.ibm.wala.cast.js.html.IHtmlParser;


/**
 * @author danielk
 * Uses the Jericho parser to go over the HTML
 */
public class JerichoHtmlParser implements IHtmlParser{
    static{
      Config.LoggerProvider = LoggerProvider.STDERR;
    }

	public void parse(URL url, InputStream reader, IHtmlCallback callback, String fileName) {
		Parser parser = new Parser(callback, fileName);
		Source src;
		try {
			src = new Source(reader);
			List<Element> childElements = src.getChildElements();
			for (Iterator<Element> nodeIterator = childElements.iterator(); nodeIterator.hasNext();) {
				Element e = nodeIterator.next();
				parser.parse(e);
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
			for (Iterator<Element> nodeIterator = childElements.iterator(); nodeIterator.hasNext();) {
				Element child = nodeIterator.next();
				parse(child);
			}
			handler.handleEndTag(tag);
		}

	}

}
