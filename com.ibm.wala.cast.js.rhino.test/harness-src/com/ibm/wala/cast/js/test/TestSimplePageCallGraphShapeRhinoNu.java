package com.ibm.wala.cast.js.test;

import org.junit.Before;

import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.IHtmlParserFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.html.nu_validator.NuValidatorHtmlParser;

public class TestSimplePageCallGraphShapeRhinoNu extends TestSimplePageCallGraphShapeRhino {

	public static void main(String[] args) {
		justThisTest(TestSimplePageCallGraphShapeRhinoNu.class);
	}

	@Before
	public void setUp() {
		super.setUp();
		WebUtil.setFactory(new IHtmlParserFactory() {
			public IHtmlParser getParser() {
				return new NuValidatorHtmlParser();
			}
		});
	}
}
