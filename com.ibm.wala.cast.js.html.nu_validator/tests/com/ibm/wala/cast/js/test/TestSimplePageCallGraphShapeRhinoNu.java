package com.ibm.wala.cast.js.test;

import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.nu_validator.NuValidatorHtmlParser;

public class TestSimplePageCallGraphShapeRhinoNu extends TestSimplePageCallGraphShapeRhino {

	public static void main(String[] args) {
		justThisTest(TestSimplePageCallGraphShapeRhinoNu.class);
	}

	@Override
	protected IHtmlParser getParser() {
		return new NuValidatorHtmlParser();
	}
}
