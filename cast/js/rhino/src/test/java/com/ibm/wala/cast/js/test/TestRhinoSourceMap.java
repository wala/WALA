/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.test;

import static com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.makeHierarchy;
import static com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.makeLoaders;
import static com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory;
import static com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil.makeScriptScope;
import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.SourceBuffer;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashMapFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestRhinoSourceMap {

  @BeforeEach
  public void setUp() {
    setTranslatorFactory(new CAstRhinoTranslatorFactory());
  }

  private record Assertion(String key, @Language("JavaScript") String source) {}

  private static final Assertion[] jquery_spec_testSource = {
    new Assertion(
        "Ltests/jquery_spec_test.js/anonymous__0/isEmptyDataObject",
        """
        function isEmptyDataObject(obj) {
            for (var name in obj) {
                if (name !== "toJSON") {
                    return false;
                }
            }
            return true;
        }"""),
    new Assertion(
        "Ltests/jquery_spec_test.js/anonymous__0/anonymous__59/anonymous__62/anonymous__63/anonymous__64/anonymous__65",
        """
        function anonymous__65() {
            returned = fn.apply(this, arguments);
            if (returned && jQuery.isFunction(returned.promise)) {
                returned.promise().then(newDefer.resolve, newDefer.reject);
            } else {
                newDefer[action](returned);
            }
        }"""),
    new Assertion(
        "Ltests/jquery_spec_test.js/anonymous__0/anonymous__386/anonymous__392",
        """
        function anonymous__392(map) {
            if (map) {
                var tmp;
                if (state < 2) {
                    for (tmp in map) {
                        statusCode[tmp] = [ statusCode[tmp], map[tmp] ];
                    }
                } else {
                    tmp = map[jqXHR.status];
                    jqXHR.then(tmp, tmp);
                }
            }
            return this;
        }"""),
    new Assertion(
        "Ltests/jquery_spec_test.js/anonymous__0/getWindow",
        """
    function getWindow(elem) {
        return jQuery.isWindow(elem) ? elem : elem.nodeType === 9 ? elem.defaultView || elem.parentWindow : false;
    }"""),
    new Assertion(
        "Ltests/jquery_spec_test.js/anonymous__0/anonymous__1/anonymous__7",
        """
        function anonymous__7(elems, name, selector) {
            var ret = this.constructor();
            if (jQuery.isArray(elems)) {
                push.apply(ret, elems);
            } else {
                jQuery.merge(ret, elems);
            }
            ret.prevObject = this;
            ret.context = this.context;
            if (name === "find") {
                ret.selector = this.selector + (this.selector ? " " : "") + selector;
            } else if (name) {
                ret.selector = this.selector + "." + name + "(" + selector + ")";
            }
            return ret;
        }"""),
    new Assertion(
        "Ltests/jquery_spec_test.js/anonymous__0/anonymous__1/anonymous__17",
        """
        function anonymous__17() {
            var options, name, src, copy, copyIsArray, clone, target = arguments[0] || {}, i = 1, length = arguments.length, deep = false;
            if (typeof target === "boolean") {
                deep = target;
                target = arguments[1] || {};
                i = 2;
            }
            if (typeof target !== "object" && !jQuery.isFunction(target)) {
                target = {};
            }
            if (length === i) {
                target = this;
                --i;
            }
            for (; i < length; i++) {
                if ((options = arguments[i]) != null) {
                    for (name in options) {
                        (function _forin_body_extra_1(name) { var src = target[name];
                        var copy = options[name];
                        if (target === copy) {
                            return; //continue;
                        }
                        if (deep && copy && (jQuery.isPlainObject(copy) || (copyIsArray = jQuery.isArray(copy)))) {
                            if (copyIsArray) {
                                copyIsArray = false;
                                clone = src && jQuery.isArray(src) ? src : [];
                            } else {
                                clone = src && jQuery.isPlainObject(src) ? src : {};
                            }
                            target[name] = jQuery.extend(deep, clone, copy);
                        } else if (copy !== undefined) {
                            target[name] = copy;
                        } })(name);
                    }
                }
            }
            return target;
        }""")
  };

  @Test
  public void testJquerySpecTestSourceMappings()
      throws IllegalArgumentException, IOException, ClassHierarchyException {
    checkFunctionBodies("jquery_spec_test.js", jquery_spec_testSource);
  }

  private static void checkFunctionBodies(String fileName, Assertion[] assertions)
      throws IOException, ClassHierarchyException {
    Map<String, String> sources =
        Arrays.stream(assertions).collect(HashMapFactory.toMap(Assertion::key, Assertion::source));

    JavaScriptLoaderFactory loaders = makeLoaders(null);
    AnalysisScope scope = makeScriptScope("tests", fileName, loaders);
    IClassHierarchy cha = makeHierarchy(scope, loaders);
    for (IClass cls : cha) {
      if (cls.getName().toString().contains(fileName)) {
        AstMethod fun = (AstMethod) cls.getMethod(AstMethodReference.fnSelector);
        // System.err.println(fun.getDeclaringClass().getName() + " " + fun.getSourcePosition());
        SourceBuffer sb = new SourceBuffer(fun.getSourcePosition());
        // System.err.println(sb);
        TypeName declaringClassName = fun.getDeclaringClass().getName();
        String expectedToString = sources.get(declaringClassName.toString());
        if (expectedToString != null) {
          System.err.println(
              "checking source of " + declaringClassName + " at " + fun.getSourcePosition());
          assertThat(sb).hasToString(expectedToString);
        }
      }
    }
  }
}
