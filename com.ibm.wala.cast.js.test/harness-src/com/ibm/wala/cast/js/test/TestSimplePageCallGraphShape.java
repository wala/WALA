/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;

public abstract class TestSimplePageCallGraphShape extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestSimplePageCallGraphShape.class);
  }

  private static final Object[][] assertionsForPage1 = new Object[][] {
    new Object[] { ROOT, new String[] { "page1.html" } },
    new Object[] { "page1.html",
        new String[] { "prologue.js/substring",
                       "prologue.js/indexOf",
                       "preamble.js/DOMDocument/write_to_dom",
                       "prologue.js/encodeURI"
        }
    }
  };

  @Test public void testPage1() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page1.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage1);
  }

  private static final Object[][] assertionsForPage2 = new Object[][] {
    new Object[] { ROOT, new String[] { "page2.html" } }
  };
  
  @Test public void testPage2() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page2.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage2);
  }

  private static final Object[][] assertionsForPage3 = new Object[][] {
    new Object[] { ROOT, new String[] { "page3.html" } }
  };
  
  @Test public void testPage3() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage3);
  }

  @Test public void testCrawl() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, null);
  }

  private static final Object[][] assertionsForPage11 = new Object[][] {
    new Object[] { ROOT, new String[] { "page11.html" } },
    new Object[] { "page11.html",
        new String[] { "preamble.js/DOMDocument/createElement",
                       "preamble.js/DOMNode/appendChild",
                       "preamble.js/DOMElement/setAttribute"
        }
    }
  };

  @Test public void testCrawlPage11() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page11.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage11);
  }

  private static final Object[][] assertionsForPage12 = new Object[][] {
    new Object[] { ROOT, new String[] { "page12.html" } },
    new Object[] { "page12.html",
        new String[] { "page12.html/make_node0/make_node3/make_node4/onclick_node4" } },
    new Object[] { "page12.html/make_node0/make_node3/make_node4/onclick_node4",
        new String[] { "page12.html/callXHR"
        }
    },
    new Object[]{ "page12.html/callXHR",
        new String[] { "preamble.js/DOMDocument/getElementById",
                       "preamble.js/_XMLHttpRequest/xhr_open",
                       "preamble.js/_XMLHttpRequest/xhr_send"
        }
    },
    new Object[]{ "preamble.js/_XMLHttpRequest/xhr_open",
        new String[] { "preamble.js/_XMLHttpRequest/xhr_orsc_handler" }
    },
    new Object[]{ "preamble.js/_XMLHttpRequest/xhr_send",
        new String[] { "preamble.js/_XMLHttpRequest/xhr_orsc_handler" }
    },
    new Object[]{ "preamble.js/_XMLHttpRequest/xhr_orsc_handler",
        new String[] { "page12.html/handler" }
    },
  };

  @Test public void testCrawlPage12() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page12.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage12);
  }

  private static final Object[][] assertionsForPage13 = new Object[][] {
    new Object[] { ROOT, new String[] { "page13.html" } },
    new Object[] { "page13.html",
        new String[] { "page13.html/make_node0/make_node3/make_node4/onclick_node4" } },
    new Object[] { "page13.html/make_node0/make_node3/make_node4/onclick_node4",
        new String[] { "page13.html/callXHR"
        }
    },
    new Object[]{ "page13.html/callXHR",
        new String[] { "preamble.js/DOMDocument/getElementById",
                       "preamble.js/_XMLHttpRequest/xhr_open",
                       "preamble.js/_XMLHttpRequest/xhr_setRequestHeader",
                       "preamble.js/_XMLHttpRequest/xhr_send"
        }
    },
    new Object[]{ "preamble.js/_XMLHttpRequest/xhr_open",
        new String[] { "preamble.js/_XMLHttpRequest/xhr_orsc_handler" }
    },
    new Object[]{ "preamble.js/_XMLHttpRequest/xhr_send",
        new String[] { "preamble.js/_XMLHttpRequest/xhr_orsc_handler" }
    },
    new Object[]{ "preamble.js/_XMLHttpRequest/xhr_orsc_handler",
        new String[] { "page13.html/handler" }
    }
  };

  @Test public void testCrawlPage13() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page13.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage13);
  }

  private static final Object[][] assertionsForPage15 = new Object[][] {
    new Object[] { ROOT, new String[] { "page15.html" } },
    new Object[] { "page15.html",
        new String[] { "page15.html/make_node0/make_node3/onload_node3"
        }
    },
    new Object[] { "page15.html/make_node0/make_node3/onload_node3",
        new String[] { "page15.html/changeUrls" }
    }
  };

  @Test public void testCrawlPage15() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page15.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage15);
  }

  private static final Object[][] assertionsForPage16 = new Object[][] {
    new Object[] { ROOT, new String[] { "page16.html" } },
    new Object[] { "page16.html",
        new String[] { "page16.html/make_node0/make_node3/make_node4/onclick_node4"
        }
    },
    new Object[] { "page16.html/make_node0/make_node3/make_node4/onclick_node4",
        new String[] { "page16.html/changeUrls" }
    }
  };

  @Test public void testCrawlPage16() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page16.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage16);
  }

  private static final Object[][] assertionsForPage17 = new Object[][] {
    new Object[] { ROOT, new String[] { "page17.html" } },
    new Object[] { "page17.html",
        new String[] { "page17.html/loadScript" }
    },
    new Object[] { "preamble.js",
        new String[] { "page17.html/loadScript/_page17_handler" }
    },
    new Object[] { "page17.html/loadScript/_page17_handler",
        new String[] { "page17.html/callFunction" }
    },
    new Object[] { "page17.html/callFunction",
        new String[] { "suffix:changeUrls" }
    }
  };
    
  @Test public void testCrawlPage17() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page17.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage17);
  }

  /*
  @Test public void testDojoTest() throws IllegalArgumentException, IOException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/dojo/test.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, null);
  }
    */
  
  private static final Object[][] assertionsForApolloExample = new Object[][] {
    new Object[] { ROOT, new String[] { "apollo-example.html" } },
    new Object[] { "apollo-example.html", new String[] { "apollo-example.html/signon" } },
    new Object[] { "apollo-example.html/signon", new String[] { "preamble.js/DOMWindow/window_open" } }
  };
  
  @Test public void testApolloExample() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/apollo-example.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForApolloExample);
  }

  @Test public void testNojs() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/nojs.html");
    CallGraph CG = Util.makeHTMLCG(url);
    // all we need is for it to finish building CG successfully.
  }

}
