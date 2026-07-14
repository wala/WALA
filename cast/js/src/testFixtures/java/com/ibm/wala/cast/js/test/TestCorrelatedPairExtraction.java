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

import static org.assertj.core.api.Assertions.assertThat;

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
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public abstract class TestCorrelatedPairExtraction {
  // set to "true" to use `equals` to check whether a test case passed;
  // if it is set to "false", expected and actual output are instead written to files expected.dump
  // and actual.dump
  // this is useful if the outputs are too big/too different for Eclipse's diff view to handle
  private static final boolean ASSERT_EQUALS = true;

  @TempDir private File tmpDir;

  public void testRewriter(@Language("JavaScript") String in, @Language("JavaScript") String out) {
    testRewriter(null, in, out);
  }

  public void testRewriter(
      String testName, @Language("JavaScript") String in, @Language("JavaScript") String out) {
    try {
      final var tmp = File.createTempFile("test", ".js", tmpDir);
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
              assertThat(policy).isNotNull();
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
        assertThat(actual).as(testName).isEqualTo(expected);
      }

    } catch (IOException | ClassHierarchyException e) {
      e.printStackTrace();
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
        """
            function extend(dest, src) {
              for(var p in src) {
                dest[p] = src[p];
              }
            }""",
        """
            function extend(dest, src) {
              for(var p in src) {
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                 })(p);
              }
            }""");
  }

  // example from the paper, but with single-statement loop body
  @Test
  public void test2() {
    testRewriter(
        """
            function extend(dest, src) {
              for(var p in src)
                dest[p] = src[p];
            }""",
        """
            function extend(dest, src) {
              for(var p in src)
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                 })(p);
            }""");
  }

  // example from the paper, but without var decl
  // currently fails because the loop index is a global variable
  @Disabled
  @Test
  public void test3() {
    testRewriter(
        """
            function extend(dest, src) {
              for(p in src)
                dest[p] = src[p];
            }""",
        """
            function extend(dest, src) {
              for(p in src)
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                 })(p);
            }""");
  }

  // example from the paper, but with separate var decl
  @Test
  public void test4() {
    testRewriter(
        """
            function extend(dest, src) {
              var p;
              for(p in src)
                dest[p] = src[p];
            }""",
        """
            function extend(dest, src) {
              var p;
              for(p in src)
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                 })(p);
            }""");
  }

  // example from the paper, but with weirdly placed var decl
  @Test
  public void test5() {
    testRewriter(
        """
            function extend(dest, src) {
              for(p in src) {
                var p;
                dest[p] = src[p];
              }
            }""",
        """
            function extend(dest, src) {
              for(p in src) {
                var p;
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                 })(p);
              }
            }""");
  }

  // example from the paper, but with weirdly placed var decl in a different place
  @Test
  public void test6() {
    testRewriter(
        """
            function extend(dest, src) {
              for(p in src) {
                dest[p] = src[p];
                var p;
              }
            }""",
        """
            function extend(dest, src) {
              for(p in src) {
                var p;
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                 })(p);
              }
            }""");
  }

  // example where loop variable is referenced after the loop
  // currently fails because the check is not implemented yet
  @Disabled
  @Test
  public void test7() {
    testRewriter(
        """
            function extend(dest, src) {
              for(var p in src) {
                dest[p] = src[p];
                p = true;
              }
              return p;
            }""",
        null);
  }

  // example with "this"
  @Test
  public void test8() {
    testRewriter(
        """
            Object.prototype.extend = function(src) {
              for(var p in src)
                this[p] = src[p];
            }""",
        """
            Object.prototype.extend = function(src) {
              for(var p in src)
                (function _forin_body_0(p, thi$) {
                  thi$[p] = src[p];
                })(p, this);
            }""");
  }

  // another example with "this"
  // fails since variables from enclosing functions are no longer in SSA form, hence no correlation
  // is found
  @Disabled
  @Test
  public void test9() {
    testRewriter(
        """
            function defGlobals(globals) {
              for(var p in globals) {
                (function() {
                  this[p] = globals[p];
                })();
              }
            }""",
        """
            function defGlobals(globals) {
              for(var p in globals) {
                (function() {
                  (function _forin_body_0(p, thi$) {
                    thi$[p] = globals[p];
                  })(p, this)
                })();
              }
            }""");
  }

  // an example with "break"
  @Test
  public void test10() {
    testRewriter(
        """
            function extend(dest, src) {
              for(var p in src) {
                if(p == "stop")
                  break;
                dest[p] = src[p];
              }
            }""",
        """
            function extend(dest, src) {
              for(var p in src) {
                if(p == "stop")
                  break;\
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                })(p);
              }
            }""");
  }

  // another example with "break"
  @Test
  public void test11() {
    testRewriter(
        """
            function extend(dest, src) {
              for(var p in src) {
                while(true) {
                  dest[p] = src[p];
                  break;
                }
              }
            }""",
        """
            function extend(dest, src) {
              for(var p in src) {
                while(true) {
                  (function _forin_body_0(p) {
                    dest[p] = src[p];
                  })(p);
                  break;
                }
              }
            }""");
  }

  // an example with labelled "break"
  @Test
  public void test12() {
    testRewriter(
        """
            function extend(dest, src) {
              outer: for(var p in src) {
                while(true) {
                  dest[p] = src[p];
                  break outer;
                }
              }
            }""",
        """
            function extend(dest, src) {
              outer: for(var p in src) {
                while(true) {
                  (function _forin_body_0(p) {
                    dest[p] = src[p];
                  })(p);\
                  break outer;
                }
              }
            }""");
  }

  // an example with exceptions
  @Test
  public void test13() {
    testRewriter(
        """
            function extend(dest, src) {
              for(var p in src) {
                if(p == '__proto__')
                  throw new Exception('huh?');
                dest[p] = src[p];
              }
            }""",
        """
            function extend(dest, src) {
              for(var p in src) {
                if(p == '__proto__')
                  throw new Exception('huh?');
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                 })(p);
              }
            }""");
  }

  // an example with a "with" block
  @Test
  public void test14() {
    testRewriter(
        """
            function extend(dest, src) {
              var o = { dest: dest };
              with(o) {
                for(var p in src) {
                  dest[p] = src[p];
                }
              }
            }""",
        """
            function extend(dest, src) {
              var o = { dest: dest };
              with(o) {
                for(var p in src) {
                  (function _forin_body_0(p) {
                    dest[p] = src[p];
                  })(p);
                }
              }
            }""");
  }

  // example with two functions
  @Test
  public void test15() {
    testRewriter(
        """
            function extend(dest, src) {
              for(var p in src)
                dest[p] = src[p];
            }
            function foo() {
              extend({}, {});
            }
            foo();""",
        """
            function extend(dest, src) {
              for(var p in src)
                (function _forin_body_0(p) {
                  dest[p] = src[p];
                })(p);
            }
            function foo() {
              extend({}, {});
            }
            foo();""");
  }

  @Test
  public void test16() {
    testRewriter(
        """
            function ext(dest, src) {
              for(var p in src)
                do_ext(dest, p, src);
            }
            function do_ext(x, p, y) { x[p] = y[p]; }""",
        """
            function ext(dest, src) {
              for(var p in src)
                do_ext(dest, p, src);
            }
            function do_ext(x, p, y) { x[p] = y[p]; }""");
  }

  @Test
  public void test17() {
    testRewriter(
        """
            function implement(dest, src) {
              for(var p in src) {
                dest.prototype[p] = src[p];
              }
            }""",
        """
            function implement(dest, src) {
              for(var p in src) {
                (function _forin_body_0(p) {
                  dest.prototype[p] = src[p];
                 })(p);
              }
            }""");
  }

  // fails since the assignment to "value" in the extracted version gets a (spurious) reference
  // error CFG edge
  @Test
  public void test18() {
    testRewriter(
        """
            function addMethods(source) {
              var properties = Object.keys(source);
              for (var i = 0, length = properties.length; i < length; i++) {
                var property = properties[i], value = source[property];
                this.prototype[property] = value;
              }
              return this;
            }""",
        """
            function addMethods(source) {
              var properties = Object.keys(source);
              for (var i = 0, length = properties.length; i < length; i++) {
                var property, value; property = properties[i]; value = (function _forin_body_0(property, thi$) { var value = source[property];\s
                thi$.prototype[property] = value; return value; })(property, this);
              }
              return this;
            }""");
  }

  // slight variation of test18
  @Test
  public void test18_b() {
    testRewriter(
        """
            function addMethods(source) {
              var properties = Object.keys(source);
              for (var i = 0, length = properties.length; i < length; i++) {
                var property = properties[i], foo = 23, value = source[property];
                this.prototype[property] = value;
              }
              return this;
            }""",
        """
            function addMethods(source) {
              var properties = Object.keys(source);
              for (var i = 0, length = properties.length; i < length; i++) {
                var property, foo, value; property = properties[i]; foo = 23; value = (function _forin_body_0(property, thi$) { var value = source[property];
                thi$.prototype[property] = value; return value; })(property, this);
              }
              return this;
            }""");
  }

  // fails since the assignment to "value" in the extracted version gets a (spurious) reference
  // error CFG edge
  @Test
  public void test18_c() {
    testRewriter(
        """
            function addMethods(source) {
              var properties = Object.keys(source);
              for (var i = 0, length = properties.length; i < length; i++) {
                var property = properties[i], foo = 23, value = source[property], bar = 42;
                this.prototype[property] = value;
              }
              return this;
            }""",
        """
            function addMethods(source) {
              var properties = Object.keys(source);
              for (var i = 0, length = properties.length; i < length; i++) {
                var property, foo, value, bar; property = properties[i]; foo = 23; value = function _forin_body_0(property, thi$) { var value = source[property]; bar = 42;
                thi$.prototype[property] = value; return value; }(property, this);
              }
              return this;
            }""");
  }

  @Test
  public void test19() {
    testRewriter(
        """
            function extend(dest, src) {
              for(var p in src)
                if(foo(p)) write.call(dest, p, src[p]);
            }
            function write(p, v) { this[p] = v; }""",
        """
            function extend(dest, src) {
              for(var p in src)
                  (function _forin_body_0(p) { if(foo(p)) write.call(dest, p, src[p]); })(p);
            }
            function write(p, v) { this[p] = v; }""");
  }

  // fails due to a missing LOCAL_SCOPE node
  @Disabled
  @Test
  public void test20() {
    testRewriter(
        """
            function every(object, fn, bind) {
              for(var key in object)
                if(hasOwnProperty.call(object, key) && !fn.call(bind, object[key], key)) return false;
            }""",
        """
            function every(object, fn, bind) {
              for(var key in object) {
                re$ = (function _forin_body_0(key) {
                  if (hasOwnProperty.call(object, key) && !fn.call(bind, object[key], key)) return { type: 'return', value: false };
                })(key);
                if(re$) { if(re$.type == 'return') return re$.value; }
              }
            }""");
  }

  @Test
  public void test21() {
    testRewriter(
        """
            function extend(dest, src) {
              var x, y;
              for(var name in src) {
                x = dest[name];
                y = src[name];
                dest[name] = join(x,y);
              }
            }""",
        """
            function extend(dest, src) {
              var x, y;
              for(var name in src) {
                (function _forin_body_0(name) { x = dest[name];
                y = src[name];
                dest[name] = join(x,y); })(name);
              }
            }""");
  }

  @Test
  public void test22() {
    testRewriter(
        """
            function(object, keys){
              var results = {};
              for (var i = 0, l = keys.length; i < l; i++){
                var k = keys[i];
                if (k in object) results[k] = object[k];
              }
              return results;
            }""",
        """
            function(object, keys){
              var results = {};
              for (var i = 0, l = keys.length; i < l; i++){
                var k = keys[i];
                (function _forin_body_0(k) { if (k in object) results[k] = object[k]; })(k);
              }
              return results;
            }""");
  }

  // variant of test1
  @Test
  public void test23() {
    testRewriter(
        """
            function extend(dest, src) {
              var s;
              for(var p in src) {
                s = src[p];
                dest[p] = s;
              }
            }""",
        """
            function extend(dest, src) {
              var s;
              for(var p in src) {
                s = (function _forin_body_0(p) {
                  var s;\
                  s = src[p];
                  dest[p] = s;
                  return s;\
                 })(p);
              }
            }""");
  }

  // cannot extract for-in body referring to "arguments"
  @Test
  public void test24() {
    testRewriter(
        """
            function extend(dest, src) {
              for(var p in src) {
                arguments[0][p] = src[p];
              }
            }""",
        """
            function extend(dest, src) {
              for(var p in src) {
                arguments[0][p] = src[p];
              }
            }""");
  }

  @Test
  public void test25() {
    testRewriter(
        """
            function eachProp(obj, func) {
               var prop;
               for (prop in obj) {
                 if (hasProp(obj, prop)) {
                   if (func(obj[prop], prop)) {
                     break;
                   }
                  }
              }
            }""",
        """
            function eachProp(obj, func) {
               var prop;
               for (prop in obj) {
                 if (hasProp(obj, prop)) {
                   if (func(obj[prop], prop)) {
                     break;
                   }
                  }
              }
            }""",
        """
            function eachProp(obj, func) {
               var prop;
               for (prop in obj) {
                 if (hasProp(obj, prop)) {
                   re$ = (function _forin_body_0 (prop) { if (func(obj[prop], prop)) { return { type: "goto", target: 0 }; } })(prop);
                   if (re$) {
                     if (re$.type == "goto") {
                       if (re$.target == 0)
                         break;
                     }
                   }
                  }
              }
            }""");
  }
}
