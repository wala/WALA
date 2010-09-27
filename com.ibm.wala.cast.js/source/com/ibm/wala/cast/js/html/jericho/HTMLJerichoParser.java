package com.ibm.wala.cast.js.html.jericho;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import au.id.jericho.lib.html.Config;
import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.LoggerProvider;
import au.id.jericho.lib.html.Source;

import com.ibm.wala.cast.js.html.IHtmlCallback;
import com.ibm.wala.cast.js.html.IHtmlParser;


/**
 * @author danielk
 * Uses the Jericho parser to go over the HTML
 */
public class HTMLJerichoParser implements IHtmlParser{
    static{
      Config.LoggerProvider = LoggerProvider.STDERR;
    }

	@SuppressWarnings("unchecked")
	public void parse(Reader reader, IHtmlCallback callback, String fileName) {
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

		@SuppressWarnings("unchecked")
		private void parse(Element root) {
			JerichoTag tag = new JerichoTag(root, fileName);
			handler.handleStartTag(tag);
			List<Element> childElements = root.getChildElements();
			for (Iterator<Element> nodeIterator = childElements.iterator(); nodeIterator.hasNext();) {
				Element child = nodeIterator.next();
				parse(child);
			}
			handler.handleEndTag(tag);
		}

	}

}
