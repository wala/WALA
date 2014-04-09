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

import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.IHtmlParserFactory;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;

public abstract class TestSimplePageCallGraphShape extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestSimplePageCallGraphShape.class);
  }
    
  protected abstract IHtmlParser getParser();
  
  @Before
  public void setUp() {
    WebUtil.setFactory(new IHtmlParserFactory() {
      public IHtmlParser getParser() {
        return getParser();
      }
    });
  }

  @Before
  public void config() {
    JSSourceExtractor.USE_TEMP_NAME = false;
    JSSourceExtractor.DELETE_UPON_EXIT = true;
  }
  
  private static final Object[][] assertionsForPage1 = new Object[][] {
    new Object[] { ROOT, new String[] { "page1.html" } },
    new Object[] { "page1.html", new String[] { "page1.html/__WINDOW_MAIN__" } },
    new Object[] { "page1.html/__WINDOW_MAIN__",
        new String[] { "prologue.js/substring",
                       "prologue.js/indexOf",
                       "preamble.js/DOMDocument/write_to_dom",
                       "prologue.js/encodeURI"
        }
    }
  };

  @Test public void testPage1() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page1.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage1);
  }

  private static final Object[][] assertionsForPage2 = new Object[][] {
    new Object[] { ROOT, new String[] { "page2.html" } },
    new Object[] { "page2.html", new String[] { "page2.html/__WINDOW_MAIN__" } }
  };
  
  @Test public void testPage2() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page2.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage2);
  }

  private static final Object[][] assertionsForPage11 = new Object[][] {
    new Object[] { ROOT, new String[] { "page11.html" } },
    new Object[] { "page11.html", new String[] { "page11.html/__WINDOW_MAIN__" } },
    new Object[] { "page11.html/__WINDOW_MAIN__",
        new String[] { "preamble.js/DOMDocument/createElement",
                       "preamble.js/DOMNode/appendChild",
                       "preamble.js/DOMElement/setAttribute"
        }
    }
  };

  @Test public void testCrawlPage11() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page11.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage11);
  }

  private static final Object[][] assertionsForPage11b = new Object[][] {
    new Object[] { ROOT, new String[] { "page11b.html" } },
    new Object[] { "page11b.html", new String[] { "page11b.html/__WINDOW_MAIN__" } },
    new Object[] { "page11b.html/__WINDOW_MAIN__",
        new String[] { "preamble.js/DOMDocument/createElement",
                       "preamble.js/DOMNode/appendChild",
                       "preamble.js/DOMElement/setAttribute"
        }
    }
  };
  
  @Test public void testCrawlPage11b() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page11b.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage11b);
  }

  private static final Object[][] assertionsForPage12 = new Object[][] {
    new Object[] { ROOT, new String[] { "page12.html" } },
    new Object[] { "page12.html", new String[] { "page12.html/__WINDOW_MAIN__" } },
    new Object[] { "page12.html/__WINDOW_MAIN__",
        new String[] { "page12.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/button_onclick" } },
    new Object[] { "page12.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/button_onclick",
        new String[] { "page12.html/__WINDOW_MAIN__/callXHR"
        }
    },
    new Object[]{ "page12.html/__WINDOW_MAIN__/callXHR",
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
        new String[] { "page12.html/__WINDOW_MAIN__/handler" }
    },
  };

  @Test public void testCrawlPage12() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page12.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage12);
  }

  private static final Object[][] assertionsForPage13 = new Object[][] {
    new Object[] { ROOT, new String[] { "page13.html" } },
    new Object[] { "page13.html", new String[] { "page13.html/__WINDOW_MAIN__" } },
    new Object[] { "page13.html/__WINDOW_MAIN__",
        new String[] { "page13.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/button_onclick" } },
    new Object[] { "page13.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/button_onclick",
        new String[] { "page13.html/__WINDOW_MAIN__/callXHR"
        }
    },
    new Object[]{ "page13.html/__WINDOW_MAIN__/callXHR",
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
        new String[] { "page13.html/__WINDOW_MAIN__/handler" }
    }
  };

  @Test public void testCrawlPage13() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page13.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);    
    verifyGraphAssertions(CG, assertionsForPage13);
  }

  private static final Object[][] assertionsForPage15 = new Object[][] {
    new Object[] { ROOT, new String[] { "page15.html" } },
    new Object[] { "page15.html", new String[] { "page15.html/__WINDOW_MAIN__" } },
    new Object[] { "page15.html/__WINDOW_MAIN__",
        new String[] { "page15.html/__WINDOW_MAIN__/make_node0/make_node3/body_onload"
        }
    },
    new Object[] { "page15.html/__WINDOW_MAIN__/make_node0/make_node3/body_onload",
        new String[] { "page15.html/__WINDOW_MAIN__/changeUrls" }
    }
  };

  @Test public void testCrawlPage15() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page15.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage15);
  }

  private static final Object[][] assertionsForPage16 = new Object[][] {
    new Object[] { ROOT, new String[] { "page16.html" } },
    new Object[] { "page16.html", new String[] { "page16.html/__WINDOW_MAIN__" } },
       new Object[] { "page16.html/__WINDOW_MAIN__",
        new String[] { "page16.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/a_onclick"
        }
    },
    new Object[] { "page16.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/a_onclick",
        new String[] { "page16.html/__WINDOW_MAIN__/changeUrls" }
    }
  };

  @Test public void testCrawlPage16() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page16.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage16);
  }

  private static final Object[][] assertionsForPage17 = new Object[][] {
    new Object[] { ROOT, new String[] { "page17.html" } },
    new Object[] { "page17.html", new String[] { "page17.html/__WINDOW_MAIN__" } },
       new Object[] { "page17.html/__WINDOW_MAIN__",
        new String[] { "page17.html/__WINDOW_MAIN__/loadScript" }
    },
    new Object[] { "preamble.js",
        new String[] { "page17.html/__WINDOW_MAIN__/loadScript/_page17_handler" }
    },
    new Object[] { "page17.html/__WINDOW_MAIN__/loadScript/_page17_handler",
        new String[] { "page17.html/__WINDOW_MAIN__/callFunction" }
    },
    new Object[] { "page17.html/__WINDOW_MAIN__/callFunction",
        new String[] { "suffix:changeUrls" }
    }
  };
    
  @Test public void testCrawlPage17() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page17.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage17);
  }
  
  private static final Object[][] assertionsForApolloExample = new Object[][] {
    new Object[] { ROOT, new String[] { "apollo-example.html" } },
    new Object[] { "apollo-example.html", new String[] { "apollo-example.html/__WINDOW_MAIN__" } },
    new Object[] { "apollo-example.html/__WINDOW_MAIN__", new String[] { "apollo-example.html/__WINDOW_MAIN__/signon" } },
    new Object[] { "apollo-example.html/__WINDOW_MAIN__/signon", new String[] { "preamble.js/DOMWindow/window_open" } }
  };
  
  @Test public void testApolloExample() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/apollo-example.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForApolloExample);
  }

  @Test public void testNojs() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/nojs.html");
    // all we need is for it to finish building CG successfully.
    JSCallGraphBuilderUtil.makeHTMLCG(url);
  }

  @Test public void testPage4() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page4.html");
    JSCallGraphBuilderUtil.makeHTMLCG(url);
  }

  private static final Object[][] sourceAssertionsForList = new Object[][] {
    new Object[]{ "suffix:update_display", "list.html", 9, 18 },
    new Object[]{ "suffix:update_with_item", "list.html", 14, 16 },
    new Object[]{ "suffix:add_item", "list.html", 20, 25 },
    new Object[]{ "suffix:collection", "pages/collection.js", 2, 14 },
    new Object[]{ "suffix:collection_add", "pages/collection.js", 7, 13 },
    new Object[]{ "suffix:forall_elt", "pages/collection.js", 9, 12 },
    new Object[]{ "suffix:forall_base", "pages/collection.js", 4, 4 }
  };
    
  @Test public void testList() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/list.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
    verifySourceAssertions(CG, sourceAssertionsForList);
  }

  @Test public void testIframeTest2() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/iframeTest2.html");
    JSCallGraphBuilderUtil.makeHTMLCG(url);
  }

  private static final Object[][] assertionsForWindowx = new Object[][] {
    new Object[] { ROOT, new String[] { "windowx.html" } },
    new Object[] { "windowx.html", new String[] { "windowx.html/__WINDOW_MAIN__" } },
    new Object[] { "windowx.html/__WINDOW_MAIN__", new String[] { "windowx.html/__WINDOW_MAIN__/_f2", "windowx.html/__WINDOW_MAIN__/_f4" } },
    new Object[] { "windowx.html/__WINDOW_MAIN__/_f2", new String[] { "windowx.html/__WINDOW_MAIN__/_f1" } },
    new Object[] { "windowx.html/__WINDOW_MAIN__/_f4", new String[] { "windowx.html/__WINDOW_MAIN__/_f3" } }

  };

  @Test public void testWindowx() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/windowx.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForWindowx);
  }
  
  /*
  @Test public void testJQuery() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/jquery.html");
    CallGraph CG = Util.makeHTMLCG(url);
  }
  */

  /*
  @Test public void testDojoTest() throws IllegalArgumentException, IOException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/dojo/test.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, null);
  }
  */
  
}
