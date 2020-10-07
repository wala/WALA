/*
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.js.test;

import com.ibm.wala.cast.js.ipa.callgraph.correlations.CorrelationFinder;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.CorrelationSummary;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.ClosureExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.CorrelatedPairExtractionPolicy;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.ExtractionPolicy;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.ExtractionPolicyFactory;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.io.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public abstract class TestCorrelatedPairExtraction {
  // set to "true" to use JUnit's assertEquals to check whether a test case passed;
  // if it is set to "false", expected and actual output are instead written to files expected.dump
  // and actual.dump
  // this is useful if the outputs are too big/too different for Eclipse's diff view to handle
  private static final boolean ASSERT_EQUALS = true;

  public void testRewriter(String in, String out) {
    testRewriter(null, in, out);
  }

  public void testRewriter(String testName, String in, String out) {
    File tmp = null;
    try {
      tmp = File.createTempFile("test", ".js");
      FileUtil.writeFile(tmp, in);

      final Map<IMethod, CorrelationSummary> summaries =
          makeCorrelationFinder()
              .findCorrelatedAccesses(
                  Collections.singleton(new SourceURLModule(tmp.toURI().toURL())));
      CAstImpl ast = new CAstImpl();
      CAstEntity inEntity = parseJS(tmp, ast);

      ExtractionPolicyFactory policyFactory =
          new ExtractionPolicyFactory() {
            @Override
            public ExtractionPolicy createPolicy(CAstEntity entity) {
              CorrelatedPairExtractionPolicy policy =
                  CorrelatedPairExtractionPolicy.make(entity, summaries);
              Assert.assertNotNull(policy);
              return policy;
            }
          };
      String actual =
          new CAstDumper().dump(new ClosureExtractor(ast, policyFactory).rewrite(inEntity));
      actual = TestForInBodyExtraction.eraseGeneratedNames(actual);

      FileUtil.writeFile(tmp, out);
      String expected = new CAstDumper().dump(parseJS(tmp, ast));
      expected = TestForInBodyExtraction.eraseGeneratedNames(expected);

      FileUtil.writeFile(new File("build/expected.dump"), expected);
      FileUtil.writeFile(new File("build/actual.dump"), actual);

      if (ASSERT_EQUALS) {
        Assert.assertEquals(testName, expected, actual);
      }

    } catch (IOException | ClassHierarchyException e) {
      e.printStackTrace();
    } finally {
      if (tmp != null && tmp.exists()) tmp.delete();
    }
  }

  protected CAstEntity parseJS(File tmp, CAstImpl ast) throws IOException {
    String moduleName = tmp.getName();
    SourceFileModule module = new SourceFileModule(tmp, moduleName, null);
    return parseJS(ast, module);
  }

  protected abstract CAstEntity parseJS(CAstImpl ast, SourceModule module) throws IOException;

  protected abstract CorrelationFinder makeCorrelationFinder();

  // example from the paper
  @Test
  public void test1() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    dest[p] = src[p];\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "     })(p);\n"
            + "  }\n"
            + "}");
  }

  // example from the paper, but with single-statement loop body
  @Test
  public void test2() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(var p in src)\n"
            + "    dest[p] = src[p];\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  for(var p in src)\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "     })(p);\n"
            + "}");
  }

  // example from the paper, but without var decl
  // currently fails because the loop index is a global variable
  @Test
  @Ignore
  public void test3() {
    testRewriter(
        "function extend(dest, src) {\n" + "  for(p in src)\n" + "    dest[p] = src[p];\n" + "}",
        "function extend(dest, src) {\n"
            + "  for(p in src)\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "     })(p);\n"
            + "}");
  }

  // example from the paper, but with separate var decl
  @Test
  public void test4() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  var p;\n"
            + "  for(p in src)\n"
            + "    dest[p] = src[p];\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  var p;\n"
            + "  for(p in src)\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "     })(p);\n"
            + "}");
  }

  // example from the paper, but with weirdly placed var decl
  @Test
  public void test5() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(p in src) {\n"
            + "    var p;\n"
            + "    dest[p] = src[p];\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  for(p in src) {\n"
            + "    var p;\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "     })(p);\n"
            + "  }\n"
            + "}");
  }

  // example from the paper, but with weirdly placed var decl in a different place
  @Test
  public void test6() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(p in src) {\n"
            + "    dest[p] = src[p];\n"
            + "    var p;\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  for(p in src) {\n"
            + "    var p;\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "     })(p);\n"
            + "  }\n"
            + "}");
  }

  // example where loop variable is referenced after the loop
  // currently fails because the check is not implemented yet
  @Test
  @Ignore
  public void test7() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    dest[p] = src[p];\n"
            + "    p = true;\n"
            + "  }\n"
            + "  return p;\n"
            + "}",
        null);
  }

  // example with "this"
  @Test
  public void test8() {
    testRewriter(
        "Object.prototype.extend = function(src) {\n"
            + "  for(var p in src)\n"
            + "    this[p] = src[p];\n"
            + "}",
        "Object.prototype.extend = function(src) {\n"
            + "  for(var p in src)\n"
            + "    (function _forin_body_0(p, thi$) {\n"
            + "      thi$[p] = src[p];\n"
            + "    })(p, this);\n"
            + "}");
  }

  // another example with "this"
  // fails since variables from enclosing functions are no longer in SSA form, hence no correlation
  // is found
  @Test
  @Ignore
  public void test9() {
    testRewriter(
        "function defglobals(globals) {\n"
            + "  for(var p in globals) {\n"
            + "    (function() {\n"
            + "      this[p] = globals[p];\n"
            + "    })();\n"
            + "  }\n"
            + "}",
        "function defglobals(globals) {\n"
            + "  for(var p in globals) {\n"
            + "    (function() {\n"
            + "      (function _forin_body_0(p, thi$) {\n"
            + "        thi$[p] = globals[p];\n"
            + "      })(p, this)\n"
            + "    })();\n"
            + "  }\n"
            + "}");
  }

  // an example with "break"
  @Test
  public void test10() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    if(p == \"stop\")\n"
            + "      break;\n"
            + "    dest[p] = src[p];\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    if(p == \"stop\")\n"
            + "      break;"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "    })(p);\n"
            + "  }\n"
            + "}");
  }

  // another example with "break"
  @Test
  public void test11() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    while(true) {\n"
            + "      dest[p] = src[p];\n"
            + "      break;\n"
            + "    }\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    while(true) {\n"
            + "      (function _forin_body_0(p) {\n"
            + "        dest[p] = src[p];\n"
            + "      })(p);\n"
            + "      break;\n"
            + "    }\n"
            + "  }\n"
            + "}");
  }

  // an example with labelled "break"
  @Test
  public void test12() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  outer: for(var p in src) {\n"
            + "    while(true) {\n"
            + "      dest[p] = src[p];\n"
            + "      break outer;\n"
            + "    }\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  outer: for(var p in src) {\n"
            + "    while(true) {\n"
            + "      (function _forin_body_0(p) {\n"
            + "        dest[p] = src[p];\n"
            + "      })(p);"
            + "      break outer;\n"
            + "    }\n"
            + "  }\n"
            + "}");
  }

  // an example with exceptions
  @Test
  public void test13() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    if(p == '__proto__')\n"
            + "      throw new Exception('huh?');\n"
            + "    dest[p] = src[p];\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    if(p == '__proto__')\n"
            + "      throw new Exception('huh?');\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "     })(p);\n"
            + "  }\n"
            + "}");
  }

  // an example with a "with" block
  @Test
  public void test14() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  var o = { dest: dest };\n"
            + "  with(o) {\n"
            + "    for(var p in src) {\n"
            + "      dest[p] = src[p];\n"
            + "    }\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  var o = { dest: dest };\n"
            + "  with(o) {\n"
            + "    for(var p in src) {\n"
            + "      (function _forin_body_0(p) {\n"
            + "        dest[p] = src[p];\n"
            + "      })(p);\n"
            + "    }\n"
            + "  }\n"
            + "}");
  }

  // example with two functions
  @Test
  public void test15() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(var p in src)\n"
            + "    dest[p] = src[p];\n"
            + "}\n"
            + "function foo() {\n"
            + "  extend({}, {});\n"
            + "}\n"
            + "foo();",
        "function extend(dest, src) {\n"
            + "  for(var p in src)\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest[p] = src[p];\n"
            + "    })(p);\n"
            + "}\n"
            + "function foo() {\n"
            + "  extend({}, {});\n"
            + "}\n"
            + "foo();");
  }

  @Test
  public void test16() {
    testRewriter(
        "function ext(dest, src) {\n"
            + "  for(var p in src)\n"
            + "    do_ext(dest, p, src);\n"
            + "}\n"
            + "function do_ext(x, p, y) { x[p] = y[p]; }",
        "function ext(dest, src) {\n"
            + "  for(var p in src)\n"
            + "    do_ext(dest, p, src);\n"
            + "}\n"
            + "function do_ext(x, p, y) { x[p] = y[p]; }");
  }

  @Test
  public void test17() {
    testRewriter(
        "function implement(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    dest.prototype[p] = src[p];\n"
            + "  }\n"
            + "}",
        "function implement(dest, src) {\n"
            + "  for(var p in src) {\n"
            + "    (function _forin_body_0(p) {\n"
            + "      dest.prototype[p] = src[p];\n"
            + "     })(p);\n"
            + "  }\n"
            + "}");
  }

  // fails since the assignment to "value" in the extracted version gets a (spurious) reference
  // error CFG edge
  @Test
  public void test18() {
    testRewriter(
        "function addMethods(source) {\n"
            + "  var properties = Object.keys(source);\n"
            + "  for (var i = 0, length = properties.length; i < length; i++) {\n"
            + "    var property = properties[i], value = source[property];\n"
            + "    this.prototype[property] = value;\n"
            + "  }\n"
            + "  return this;\n"
            + "}",
        "function addMethods(source) {\n"
            + "  var properties = Object.keys(source);\n"
            + "  for (var i = 0, length = properties.length; i < length; i++) {\n"
            + "    var property, value; property = properties[i]; value = (function _forin_body_0(property, thi$) { var value = source[property]; \n"
            + "    thi$.prototype[property] = value; return value; })(property, this);\n"
            + "  }\n"
            + "  return this;\n"
            + "}");
  }

  // slight variation of test18
  @Test
  public void test18_b() {
    testRewriter(
        "function addMethods(source) {\n"
            + "  var properties = Object.keys(source);\n"
            + "  for (var i = 0, length = properties.length; i < length; i++) {\n"
            + "    var property = properties[i], foo = 23, value = source[property];\n"
            + "    this.prototype[property] = value;\n"
            + "  }\n"
            + "  return this;\n"
            + "}",
        "function addMethods(source) {\n"
            + "  var properties = Object.keys(source);\n"
            + "  for (var i = 0, length = properties.length; i < length; i++) {\n"
            + "    var property, foo, value; property = properties[i]; foo = 23; value = (function _forin_body_0(property, thi$) { var value = source[property];\n"
            + "    thi$.prototype[property] = value; return value; })(property, this);\n"
            + "  }\n"
            + "  return this;\n"
            + "}");
  }

  // fails since the assignment to "value" in the extracted version gets a (spurious) reference
  // error CFG edge
  @Test
  public void test18_c() {
    testRewriter(
        "function addMethods(source) {\n"
            + "  var properties = Object.keys(source);\n"
            + "  for (var i = 0, length = properties.length; i < length; i++) {\n"
            + "    var property = properties[i], foo = 23, value = source[property], bar = 42;\n"
            + "    this.prototype[property] = value;\n"
            + "  }\n"
            + "  return this;\n"
            + "}",
        "function addMethods(source) {\n"
            + "  var properties = Object.keys(source);\n"
            + "  for (var i = 0, length = properties.length; i < length; i++) {\n"
            + "    var property, foo, value, bar; property = properties[i]; foo = 23; value = function _forin_body_0(property, thi$) { var value = source[property]; bar = 42;\n"
            + "    thi$.prototype[property] = value; return value; }(property, this);\n"
            + "  }\n"
            + "  return this;\n"
            + "}");
  }

  @Test
  public void test19() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  for(var p in src)\n"
            + "    if(foo(p)) write.call(dest, p, src[p]);\n"
            + "}\n"
            + "function write(p, v) { this[p] = v; }",
        "function extend(dest, src) {\n"
            + "  for(var p in src)\n"
            + "      (function _forin_body_0(p) { if(foo(p)) write.call(dest, p, src[p]); })(p);\n"
            + "}\n"
            + "function write(p, v) { this[p] = v; }");
  }

  // fails due to a missing LOCAL_SCOPE node
  @Test
  @Ignore
  public void test20() {
    testRewriter(
        "function every(object, fn, bind) {\n"
            + "  for(var key in object)\n"
            + "    if(hasOwnProperty.call(object, key) && !fn.call(bind, object[key], key)) return false;\n"
            + "}",
        "function every(object, fn, bind) {\n"
            + "  for(var key in object) {\n"
            + "    re$ = (function _forin_body_0(key) {\n"
            + "      if (hasOwnProperty.call(object, key) && !fn.call(bind, object[key], key)) return { type: 'return', value: false };\n"
            + "    })(key);\n"
            + "    if(re$) { if(re$.type == 'return') return re$.value; }\n"
            + "  }\n"
            + "}");
  }

  @Test
  public void test21() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  var x, y;\n"
            + "  for(var name in src) {\n"
            + "    x = dest[name];\n"
            + "    y = src[name];\n"
            + "    dest[name] = join(x,y);\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  var x, y;\n"
            + "  for(var name in src) {\n"
            + "    (function _forin_body_0(name) { x = dest[name];\n"
            + "    y = src[name];\n"
            + "    dest[name] = join(x,y); })(name);\n"
            + "  }\n"
            + "}");
  }

  @Test
  public void test22() {
    testRewriter(
        "function(object, keys){\n"
            + "  var results = {};\n"
            + "  for (var i = 0, l = keys.length; i < l; i++){\n"
            + "    var k = keys[i];\n"
            + "    if (k in object) results[k] = object[k];\n"
            + "  }\n"
            + "  return results;\n"
            + "}",
        "function(object, keys){\n"
            + "  var results = {};\n"
            + "  for (var i = 0, l = keys.length; i < l; i++){\n"
            + "    var k = keys[i];\n"
            + "    (function _forin_body_0(k) { if (k in object) results[k] = object[k]; })(k);\n"
            + "  }\n"
            + "  return results;\n"
            + "}");
  }

  // variant of test1
  @Test
  public void test23() {
    testRewriter(
        "function extend(dest, src) {\n"
            + "  var s;\n"
            + "  for(var p in src) {\n"
            + "    s = src[p];\n"
            + "    dest[p] = s;\n"
            + "  }\n"
            + "}",
        "function extend(dest, src) {\n"
            + "  var s;\n"
            + "  for(var p in src) {\n"
            + "    s = (function _forin_body_0(p) {\n"
            + "      var s;"
            + "      s = src[p];\n"
            + "      dest[p] = s;\n"
            + "      return s;"
            + "     })(p);\n"
            + "  }\n"
            + "}");
  }

  // cannot extract for-in body referring to "arguments"
  @Test
  public void test24() {
    testRewriter(
        "function extend(dest, src) {"
            + "  for(var p in src) {"
            + "    arguments[0][p] = src[p];"
            + "  }"
            + "}",
        "function extend(dest, src) {"
            + "  for(var p in src) {"
            + "    arguments[0][p] = src[p];"
            + "  }"
            + "}");
  }

  @Test
  public void test25() {
    testRewriter(
        "function eachProp(obj, func) {"
            + "   var prop;"
            + "   for (prop in obj) {"
            + "     if (hasProp(obj, prop)) {"
            + "       if (func(obj[prop], prop)) {"
            + "         break;"
            + "       }"
            + "      }"
            + "  }"
            + "}",
        "function eachProp(obj, func) {"
            + "   var prop;"
            + "   for (prop in obj) {"
            + "     if (hasProp(obj, prop)) {"
            + "       re$ = (function _forin_body_0 (prop) { if (func(obj[prop], prop)) { return { type: \"goto\", target: 0 }; } })(prop);"
            + "       if (re$) {"
            + "         if (re$.type == \"goto\") {"
            + "           if (re$.target == 0)"
            + "             break;"
            + "         }"
            + "       }"
            + "      }"
            + "  }"
            + "}");
  }
}
