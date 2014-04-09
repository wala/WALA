package com.ibm.wala.cast.js.test;

import org.junit.Before;

import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;

public class TestMozillaBugPagesRhino extends TestMozillaBugPages {

	  public static void main(String[] args) {
		    justThisTest(TestMozillaBugPagesRhino.class);
		  }

	  @Before
	  public void setUp() {
		  com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
	  }

}
