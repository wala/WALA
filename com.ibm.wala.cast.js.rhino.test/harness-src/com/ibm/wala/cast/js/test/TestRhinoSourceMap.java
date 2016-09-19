/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.test;

import static com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.makeHierarchy;
import static com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.makeLoaders;
import static com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory;
import static com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.makeScriptScope;

import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.SourceBuffer;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;

public class TestRhinoSourceMap {

	  @Before
	  public void setUp() {
	    setTranslatorFactory(new CAstRhinoTranslatorFactory());
	  }

	  private static final String[][] jquery_spec_testSource = new String[][]{
		  new String[]{
				  "Ltests/jquery_spec_test.js/anonymous__0/isEmptyDataObject",
				  "function isEmptyDataObject(obj) {\n" +
"        for (var name in obj) {\n" +
"            if (name !== \"toJSON\") {\n" +
"                return false;\n" +
"            }\n" +
"        }\n" +
"        return true;\n" +
"    }"
		  },
		  new String[]{
				  "Ltests/jquery_spec_test.js/anonymous__0/anonymous__59/anonymous__62/anonymous__63/anonymous__64/anonymous__65",
				  "function anonymous__65() {\n" +
"                                    returned = fn.apply(this, arguments);\n" +
"                                    if (returned && jQuery.isFunction(returned.promise)) {\n" +
"                                        returned.promise().then(newDefer.resolve, newDefer.reject);\n" +
"                                    } else {\n" +
"                                        newDefer[action](returned);\n" +
"                                    }\n" +
"                                }"
		  },
		  new String[]{
				  "Ltests/jquery_spec_test.js/anonymous__0/anonymous__386/anonymous__392",
				  "function anonymous__392(map) {\n" +
"                if (map) {\n" +
"                    var tmp;\n" +
"                    if (state < 2) {\n" +
"                        for (tmp in map) {\n" +
"                            statusCode[tmp] = [ statusCode[tmp], map[tmp] ];\n" +
"                        }\n" +
"                    } else {\n" +
"                        tmp = map[jqXHR.status];\n" +
"                        jqXHR.then(tmp, tmp);\n" +
"                    }\n" +
"                }\n" +
"                return this;\n" +
"            }"
		  },
		  new String[]{
				  "Ltests/jquery_spec_test.js/anonymous__0/getWindow",
				  "function getWindow(elem) {\n" +
"        return jQuery.isWindow(elem) ? elem : elem.nodeType === 9 ? elem.defaultView || elem.parentWindow : false;\n" +
"    }"
		  },
		  new String[]{
				  "Ltests/jquery_spec_test.js/anonymous__0/anonymous__1/anonymous__7",
				  "function anonymous__7(elems, name, selector) {\n" +
"                var ret = this.constructor();\n" +
"                if (jQuery.isArray(elems)) {\n" +
"                    push.apply(ret, elems);\n" +
"                } else {\n" +
"                    jQuery.merge(ret, elems);\n" +
"                }\n" +
"                ret.prevObject = this;\n" +
"                ret.context = this.context;\n" +
"                if (name === \"find\") {\n" +
"                    ret.selector = this.selector + (this.selector ? \" \" : \"\") + selector;\n" +
"                } else if (name) {\n" +
"                    ret.selector = this.selector + \".\" + name + \"(\" + selector + \")\";\n" +
"                }\n" +
"                return ret;\n" +
"            }"
		  },
		  new String[]{
				  "Ltests/jquery_spec_test.js/anonymous__0/anonymous__1/anonymous__17",
				  "function anonymous__17() {\n" +
"            var options, name, src, copy, copyIsArray, clone, target = arguments[0] || {}, i = 1, length = arguments.length, deep = false;\n" +
"            if (typeof target === \"boolean\") {\n" +
"                deep = target;\n" +
"                target = arguments[1] || {};\n" +
"                i = 2;\n" +
"            }\n" +
"            if (typeof target !== \"object\" && !jQuery.isFunction(target)) {\n" +
"                target = {};\n" +
"            }\n" +
"            if (length === i) {\n" +
"                target = this;\n" +
"                --i;\n" +
"            }\n" +
"            for (; i < length; i++) {\n" +
"                if ((options = arguments[i]) != null) {\n" +
"                    for (name in options) {\n" +
"                        (function _forin_body_extra_1(name) { var src = target[name];\n" +
"                        var copy = options[name];\n" +
"                        if (target === copy) {\n" +
"                            return; //continue;\n" +
"                        }\n" +
"                        if (deep && copy && (jQuery.isPlainObject(copy) || (copyIsArray = jQuery.isArray(copy)))) {\n" +
"                            if (copyIsArray) {\n" +
"                                copyIsArray = false;\n" +
"                                clone = src && jQuery.isArray(src) ? src : [];\n" +
"                            } else {\n" +
"                                clone = src && jQuery.isPlainObject(src) ? src : {};\n" +
"                            }\n" +
"                            target[name] = jQuery.extend(deep, clone, copy);\n" +
"                        } else if (copy !== undefined) {\n" +
"                            target[name] = copy;\n" +
"                        } })(name);\n" +
"                    }\n" +
"                }\n" +
"            }\n" +
"            return target;\n" +
"        }"}
	  };
	  
	  @Test
	  public void testJquerySpecTestSourceMappings() throws IllegalArgumentException, IOException, CancelException, ClassHierarchyException {
		  checkFunctionBodies("jquery_spec_test.js", jquery_spec_testSource);
	  }

	  private void checkFunctionBodies(String fileName, String[][] assertions) throws IOException, ClassHierarchyException {
		  Map<String, String> sources = HashMapFactory.make();
		  for(String[] assertion : assertions) {
			  sources.put(assertion[0], assertion[1]);
		  }
		  
		  JavaScriptLoaderFactory loaders = makeLoaders(null);
		  AnalysisScope scope = makeScriptScope("tests", fileName, loaders);
		  IClassHierarchy cha = makeHierarchy(scope, loaders);
		  for(IClass cls : cha) {
			  if (cls.getName().toString().contains(fileName)) {
				  AstMethod fun = (AstMethod)cls.getMethod(AstMethodReference.fnSelector);
				 //System.err.println(fun.getDeclaringClass().getName() + " " + fun.getSourcePosition());
				  SourceBuffer sb = new SourceBuffer(fun.getSourcePosition());
				  //System.err.println(sb);
				  if (sources.containsKey(fun.getDeclaringClass().getName().toString())) {
					  System.err.println("checking source of " + fun.getDeclaringClass().getName() + " at " + fun.getSourcePosition());
					  Assert.assertEquals(sources.get(fun.getDeclaringClass().getName().toString()), sb.toString());
				  }
			  }			  
		  }
	}

}
