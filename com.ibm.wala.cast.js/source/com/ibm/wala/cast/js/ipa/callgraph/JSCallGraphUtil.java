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
package com.ibm.wala.cast.js.ipa.callgraph;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.StandardFunctionTargetSelector;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.JSAstTranslator;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

public class JSCallGraphUtil extends com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil {

  private static final boolean DEBUG = false;

  /**
   * the translator factory to be used for analysis TODO: pass the factory where
   * needed instead of using a global?
   */
  public static JavaScriptTranslatorFactory translatorFactory;
  
  /**
   * preprocessor to run generated CAst trees through, null if none
   */
  public static CAstRewriterFactory preprocessor;

  /**
   * Set up the translator factory. This method should be called before invoking
   * {@link #makeLoaders()}.
   */
  public static void setTranslatorFactory(JavaScriptTranslatorFactory translatorFactory) {
    JSCallGraphUtil.translatorFactory = translatorFactory;
  }

  public static JavaScriptTranslatorFactory getTranslatorFactory() {
    return translatorFactory;
  }
  
  public static void setPreprocessor(CAstRewriterFactory preprocessor) {
    JSCallGraphUtil.preprocessor = preprocessor;
  }

  public static JSAnalysisOptions makeOptions(AnalysisScope scope, IClassHierarchy cha, Iterable<Entrypoint> roots) {
    final JSAnalysisOptions options = new JSAnalysisOptions(scope, /*
                                                                * AstIRFactory.
                                                                * makeDefaultFactory
                                                                * (keepIRs),
                                                                */roots);

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
    options.setSelector(new StandardFunctionTargetSelector(cha, options.getMethodTargetSelector()));

    options.setUseConstantSpecificKeys(true);

    options.setUseStacksForLexicalScoping(true);

    return options;
  }

  public static JavaScriptLoaderFactory makeLoaders() {
    if (translatorFactory == null) {
      throw new IllegalStateException("com.ibm.wala.cast.js.ipa.callgraph.Util.setTranslatorFactory() must be invoked before makeLoaders()");
    }
    return new JavaScriptLoaderFactory(translatorFactory, preprocessor);
  }

  public static IClassHierarchy makeHierarchy(AnalysisScope scope, ClassLoaderFactory loaders) throws ClassHierarchyException {
    return ClassHierarchy.make(scope, loaders, JavaScriptLoader.JS);
  }

  public static Iterable<Entrypoint> makeScriptRoots(IClassHierarchy cha) {
    return new JavaScriptEntryPoints(cha, cha.getLoader(JavaScriptTypes.jsLoader));
  }

  /**
   * Get all the nodes in CG with name funName. If funName is of the form
   * <code>"ctor:nm"</code>, return nodes corresponding to constructor function
   * for <code>nm</code>. If funName is of the form <code>"suffix:nm"</code>,
   * return nodes corresponding to functions whose names end with
   * <code>nm</code>. Otherwise, return nodes for functions whose name matches
   * funName exactly.
   */
  public static Collection<CGNode> getNodes(CallGraph CG, String funName) {
    boolean ctor = funName.startsWith("ctor:");
    boolean suffix = funName.startsWith("suffix:");
    if (ctor) {
      TypeReference TR = TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName("L" + funName.substring(5)));
      MethodReference MR = JavaScriptMethods.makeCtorReference(TR);
      return CG.getNodes(MR);
    } else if (suffix) {
      Set<CGNode> nodes = new HashSet<CGNode>();
      String tail = funName.substring(7);
      for (CGNode n : CG) {
        if (n.getMethod().getReference().getDeclaringClass().getName().toString().endsWith(tail)) {
          nodes.add(n);
        }
      }
      return nodes;
    } else {
      TypeReference TR = TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName("L" + funName));
      MethodReference MR = AstMethodReference.fnReference(TR);
      return CG.getNodes(MR);
    }
  }

  /**
   * @param cha
   * @param cl
   * @param fileName
   * @param url
   * @return The set of class names that where defined in the CHA as a result
   *         loading process.
   * @throws IOException
   */
  public static Set<String> loadAdditionalFile(IClassHierarchy cha, JavaScriptLoader cl, String fileName, URL url)
      throws IOException {
    try {
      SourceURLModule M = new SourceURLModule(url);
      TranslatorToCAst toCAst = getTranslatorFactory().make(new CAstImpl(), M);
      final Set<String> names = new HashSet<String>();
      JSAstTranslator toIR = new JSAstTranslator(cl) {
        @Override
        protected void defineFunction(CAstEntity N, WalkContext definingContext, AbstractCFG cfg, SymbolTable symtab,
            boolean hasCatchBlock, TypeReference[][] caughtTypes, boolean hasMonitorOp, AstLexicalInformation LI,
            DebuggingInformation debugInfo) {
          String fnName = "L" + composeEntityName(definingContext, N);
          names.add(fnName);
          super.defineFunction(N, definingContext, cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, LI, debugInfo);
        }

        @Override
        protected void leaveFunctionStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
          CAstEntity fn = (CAstEntity) n.getChild(0).getValue();
          Scope cs = c.currentScope();
          if (!cs.contains(fn.getName())
              || cs.lookup(fn.getName()).getDefiningScope().getEntity().getKind() == CAstEntity.SCRIPT_ENTITY) {
            int result = processFunctionExpr(n, c);
            assignValue(n, c, cs.lookup(fn.getName()), fn.getName(), result);
          } else {
            super.leaveFunctionStmt(n, c, visitor);
          }
        }
      };
      CAstEntity tree;
      try {
        tree = toCAst.translateToCAst();
        if (DEBUG) {
          CAstPrinter.printTo(tree, new PrintWriter(System.err));
        }
        toIR.translate(tree, M);
        for (String name : names) {
          IClass fcls = cl.lookupClass(name, cha);
          cha.addClass(fcls);
        }
        return names;
      } catch (Error e) {
        return Collections.emptySet();
      }
     } catch (RuntimeException e) {
      return Collections.emptySet();
    }
  }
}
