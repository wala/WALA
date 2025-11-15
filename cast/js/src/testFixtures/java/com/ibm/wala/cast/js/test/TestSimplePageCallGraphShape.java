/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class TestSimplePageCallGraphShape extends TestJSCallGraphShape {

  protected abstract IHtmlParser getParser();

  private ExtractingToPredictableFileNames predictable;

  @BeforeEach
  public void setUp() {
    predictable = new ExtractingToPredictableFileNames();
  }

  @AfterEach
  public void tearDown() {
    predictable.close();
  }

  private static final List<GraphAssertion> assertionsForPage1 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page1.html"}),
          new GraphAssertion("page1.html", new String[] {"page1.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "page1.html/__WINDOW_MAIN__",
              new String[] {
                "prologue.js/String_prototype_substring",
                "prologue.js/String_prototype_indexOf",
                "preamble.js/DOMDocument/Document_prototype_write",
                "prologue.js/encodeURI"
              }));

  @Test
  public void testPage1() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/page1.html");
    assertThat(url).isNotNull();
    System.err.println("url is " + url);
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage1);
  }

  private static final List<GraphAssertion> assertionsForPage2 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page2.html"}),
          new GraphAssertion("page2.html", new String[] {"page2.html/__WINDOW_MAIN__"}));

  @Test
  public void testPage2() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/page2.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage2);
  }

  private static final List<GraphAssertion> assertionsForPage11 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page11.html"}),
          new GraphAssertion("page11.html", new String[] {"page11.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "page11.html/__WINDOW_MAIN__",
              new String[] {
                "preamble.js/DOMDocument/Document_prototype_createElement",
                "preamble.js/DOMNode/Node_prototype_appendChild",
                "preamble.js/DOMElement/Element_prototype_setAttribute"
              }));

  @Test
  public void testCrawlPage11() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page11.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage11);
  }

  private static final List<GraphAssertion> assertionsForPage11b =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page11b.html"}),
          new GraphAssertion("page11b.html", new String[] {"page11b.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "page11b.html/__WINDOW_MAIN__",
              new String[] {
                "preamble.js/DOMDocument/Document_prototype_createElement",
                "preamble.js/DOMNode/Node_prototype_appendChild",
                "preamble.js/DOMElement/Element_prototype_setAttribute"
              }));

  @Test
  public void testCrawlPage11b() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page11b.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage11b);
  }

  private static final List<GraphAssertion> assertionsForPage12 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page12.html"}),
          new GraphAssertion("page12.html", new String[] {"page12.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "page12.html/__WINDOW_MAIN__",
              new String[] {
                "page12.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/button_onclick"
              }),
          new GraphAssertion(
              "page12.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/button_onclick",
              new String[] {"page12.html/__WINDOW_MAIN__/callXHR"}),
          new GraphAssertion(
              "page12.html/__WINDOW_MAIN__/callXHR",
              new String[] {
                "preamble.js/DOMDocument/Document_prototype_getElementById",
                "preamble.js/XMLHttpRequest/xhr_open",
                "preamble.js/XMLHttpRequest/xhr_send"
              }),
          new GraphAssertion(
              "preamble.js/XMLHttpRequest/xhr_open",
              new String[] {"preamble.js/XMLHttpRequest/xhr_orsc_handler"}),
          new GraphAssertion(
              "preamble.js/XMLHttpRequest/xhr_send",
              new String[] {"preamble.js/XMLHttpRequest/xhr_orsc_handler"}),
          new GraphAssertion(
              "preamble.js/XMLHttpRequest/xhr_orsc_handler",
              new String[] {"page12.html/__WINDOW_MAIN__/handler"}));

  @Test
  public void testCrawlPage12() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page12.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage12);
  }

  private static final List<GraphAssertion> assertionsForPage13 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page13.html"}),
          new GraphAssertion("page13.html", new String[] {"page13.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "page13.html/__WINDOW_MAIN__",
              new String[] {
                "page13.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/button_onclick"
              }),
          new GraphAssertion(
              "page13.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/button_onclick",
              new String[] {"page13.html/__WINDOW_MAIN__/callXHR"}),
          new GraphAssertion(
              "page13.html/__WINDOW_MAIN__/callXHR",
              new String[] {
                "preamble.js/DOMDocument/Document_prototype_getElementById",
                "preamble.js/XMLHttpRequest/xhr_open",
                "preamble.js/XMLHttpRequest/xhr_setRequestHeader",
                "preamble.js/XMLHttpRequest/xhr_send"
              }),
          new GraphAssertion(
              "preamble.js/XMLHttpRequest/xhr_open",
              new String[] {"preamble.js/XMLHttpRequest/xhr_orsc_handler"}),
          new GraphAssertion(
              "preamble.js/XMLHttpRequest/xhr_send",
              new String[] {"preamble.js/XMLHttpRequest/xhr_orsc_handler"}),
          new GraphAssertion(
              "preamble.js/XMLHttpRequest/xhr_orsc_handler",
              new String[] {"page13.html/__WINDOW_MAIN__/handler"}));

  @Test
  public void testCrawlPage13() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page13.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage13);
  }

  private static final List<GraphAssertion> assertionsForPage15 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page15.html"}),
          new GraphAssertion("page15.html", new String[] {"page15.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "page15.html/__WINDOW_MAIN__",
              new String[] {"page15.html/__WINDOW_MAIN__/make_node0/make_node3/body_onload"}),
          new GraphAssertion(
              "page15.html/__WINDOW_MAIN__/make_node0/make_node3/body_onload",
              new String[] {"page15.html/__WINDOW_MAIN__/changeUrls"}));

  @Test
  public void testCrawlPage15() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page15.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage15);
  }

  private static final List<GraphAssertion> assertionsForPage16 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page16.html"}),
          new GraphAssertion("page16.html", new String[] {"page16.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "page16.html/__WINDOW_MAIN__",
              new String[] {
                "page16.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/a_onclick"
              }),
          new GraphAssertion(
              "page16.html/__WINDOW_MAIN__/make_node0/make_node3/make_node4/a_onclick",
              new String[] {"page16.html/__WINDOW_MAIN__/changeUrls"}));

  @Test
  public void testCrawlPage16() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page16.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage16);
  }

  private static final List<GraphAssertion> assertionsForPage17 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"page17.html"}),
          new GraphAssertion("page17.html", new String[] {"page17.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "page17.html/__WINDOW_MAIN__",
              new String[] {"page17.html/__WINDOW_MAIN__/loadScript"}),
          new GraphAssertion(
              "preamble.js",
              new String[] {"page17.html/__WINDOW_MAIN__/loadScript/_page17_handler"}),
          new GraphAssertion(
              "page17.html/__WINDOW_MAIN__/loadScript/_page17_handler",
              new String[] {"page17.html/__WINDOW_MAIN__/callFunction"}),
          new GraphAssertion(
              "page17.html/__WINDOW_MAIN__/callFunction", new String[] {"suffix:changeUrls"}));

  @Test
  public void testCrawlPage17() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/crawl/page17.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPage17);
  }

  private static final List<GraphAssertion> assertionsForApolloExample =
      List.of(
          new GraphAssertion(ROOT, new String[] {"apollo-example.html"}),
          new GraphAssertion(
              "apollo-example.html", new String[] {"apollo-example.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "apollo-example.html/__WINDOW_MAIN__",
              new String[] {"apollo-example.html/__WINDOW_MAIN__/sign_on"}),
          new GraphAssertion(
              "apollo-example.html/__WINDOW_MAIN__/sign_on",
              new String[] {"preamble.js/DOMWindow/window_open"}));

  @Test
  public void testApolloExample() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/apollo-example.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForApolloExample);
  }

  @Test
  public void testNojs() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/nojs.html");
    // all we need is for it to finish building CG successfully.
    JSCallGraphBuilderUtil.makeHTMLCG(url);
  }

  @Test
  public void testPage4() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/page4.html");
    JSCallGraphBuilderUtil.makeHTMLCG(url);
  }

  private static final List<SourceAssertion> sourceAssertionsForList =
      List.of(
          new SourceAssertion("suffix:update_display", "list.html#2", 4, 13),
          new SourceAssertion("suffix:update_with_item", "list.html#2", 9, 11),
          new SourceAssertion("suffix:add_item", "list.html#2", 15, 20),
          new SourceAssertion("suffix:collection", "pages/collection.js", 2, 14),
          new SourceAssertion("suffix:collection_add", "pages/collection.js", 7, 13),
          new SourceAssertion("suffix:forall_elt", "pages/collection.js", 9, 12),
          new SourceAssertion("suffix:forall_base", "pages/collection.js", 4, 4));

  @Test
  public void testList() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/list.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    CAstCallGraphUtil.dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);
    verifySourceAssertions(CG, sourceAssertionsForList);
  }

  @Test
  public void testIframeTest2() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/iframeTest2.html");
    JSCallGraphBuilderUtil.makeHTMLCG(url);
  }

  private static final List<GraphAssertion> assertionsForWindowX =
      List.of(
          new GraphAssertion(ROOT, new String[] {"window_x.html"}),
          new GraphAssertion("window_x.html", new String[] {"window_x.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "window_x.html/__WINDOW_MAIN__",
              new String[] {
                "window_x.html/__WINDOW_MAIN__/_f2", "window_x.html/__WINDOW_MAIN__/_f4"
              }),
          new GraphAssertion(
              "window_x.html/__WINDOW_MAIN__/_f2",
              new String[] {"window_x.html/__WINDOW_MAIN__/_f1"}),
          new GraphAssertion(
              "window_x.html/__WINDOW_MAIN__/_f4",
              new String[] {"window_x.html/__WINDOW_MAIN__/_f3"}));

  @Test
  public void testWindowX() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/window_x.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    CAstCallGraphUtil.dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForWindowX);
  }

  private static final List<GraphAssertion> assertionsForWindowOnLoad =
      List.of(
          new GraphAssertion(ROOT, new String[] {"window_on_load.html"}),
          new GraphAssertion(
              "window_on_load.html", new String[] {"window_on_load.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "window_on_load.html/__WINDOW_MAIN__",
              new String[] {"window_on_load.html/__WINDOW_MAIN__/onload_handler"}));

  @Test
  public void testWindowOnLoad() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/window_on_load.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    CAstCallGraphUtil.dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForWindowOnLoad);
  }

  public static final List<GraphAssertion> assertionsForSkeleton =
      List.of(
          new GraphAssertion(ROOT, new String[] {"skeleton.html"}),
          new GraphAssertion("skeleton.html", new String[] {"skeleton.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "skeleton.html/__WINDOW_MAIN__",
              new String[] {"skeleton.html/__WINDOW_MAIN__/dollar"}),
          new GraphAssertion(
              "skeleton.html/__WINDOW_MAIN__/dollar",
              new String[] {"skeleton.html/__WINDOW_MAIN__/bad_guy"}),
          new GraphAssertion(
              "skeleton.html/__WINDOW_MAIN__/bad_guy",
              new String[] {"skeleton.html/__WINDOW_MAIN__/dollar"}));

  @Test
  public void testSkeleton() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/skeleton.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForSkeleton);
  }

  public static final List<GraphAssertion> assertionsForSkeleton2 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"skeleton2.html"}),
          new GraphAssertion("skeleton2.html", new String[] {"skeleton2.html/__WINDOW_MAIN__"}),
          new GraphAssertion(
              "skeleton2.html/__WINDOW_MAIN__",
              new String[] {"skeleton2.html/__WINDOW_MAIN__/dollar"}),
          new GraphAssertion(
              "skeleton2.html/__WINDOW_MAIN__/dollar",
              new String[] {"ctor:skeleton2.html/__WINDOW_MAIN__/dollar_init"}),
          new GraphAssertion(
              "ctor:skeleton2.html/__WINDOW_MAIN__/dollar_init",
              new String[] {"skeleton2.html/__WINDOW_MAIN__/dollar_init"}),
          new GraphAssertion(
              "skeleton2.html/__WINDOW_MAIN__/dollar_init",
              new String[] {"skeleton2.html/__WINDOW_MAIN__/bad_guy"}),
          new GraphAssertion(
              "skeleton2.html/__WINDOW_MAIN__/bad_guy",
              new String[] {"skeleton2.html/__WINDOW_MAIN__/dollar"}));

  @Test
  public void testSkeleton2() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/skeleton2.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForSkeleton2);
  }

  /*
  @Test public void testJQuery() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/jquery.html");
    CallGraph CG = Util.makeHTMLCG(url);
  }
  */

  /*
  @Test public void testDojoTest() throws IllegalArgumentException, IOException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/dojo/test.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, null);
  }
  */

}
