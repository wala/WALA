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
package com.ibm.wala.ide.util;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTRequestor;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionRef;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ide.classloader.EclipseSourceFileModule;
import com.ibm.wala.ide.jsdt.Activator;
import com.ibm.wala.ide.util.HeadlessUtil.EclipseCompiler;
import com.ibm.wala.ide.util.HeadlessUtil.Parser;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

public class JsdtUtil {

  public static URL getPrologueFile(String file, Plugin plugin) {
    plugin = plugin!= null? plugin: Activator.getDefault();
    JavaScriptLoader.addBootstrapFile(file);
    return plugin.getClass().getClassLoader().getResource(file);
  }

  private static final boolean useCreateASTs = false;
  
  public static class CGInfo {
    public final Graph<IMember> cg = SlowSparseNumberedGraph.make();
    public final Set<FunctionInvocation> calls = HashSetFactory.make();
  }

  public static Set<ModuleEntry> getJavaScriptCodeFromProject(String project) throws IOException, CoreException {
    IJavaScriptProject p = JavaScriptHeadlessUtil.getJavaScriptProjectFromWorkspace(project);
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
    AnalysisScope s = JavaScriptEclipseProjectPath.make(p, Collections.<Pair<String,Plugin>>emptySet()).toAnalysisScope(new CAstAnalysisScope(JSCallGraphUtil.makeLoaders(), Collections.singleton(JavaScriptLoader.JS)));

    List<Module> modules = s.getModules(JavaScriptTypes.jsLoader);
    Set<ModuleEntry> mes = HashSetFactory.make();
    for(Module m : modules) {
      for(ModuleEntry entry : Iterator2Iterable.make(m.getEntries())) {
        mes.add(entry);
      }
    }
    return mes;
  }

  public static CGInfo buildJSDTCallGraph(Set<ModuleEntry> mes) {
    final CGInfo info = new CGInfo();
    HeadlessUtil.parseModules(mes, new EclipseCompiler<IJavaScriptUnit>() {
      @Override
      public IJavaScriptUnit getCompilationUnit(IFile file) {
        return JavaScriptCore.createCompilationUnitFrom(file);
      }
      @Override
      public Parser<IJavaScriptUnit> getParser() {
        return new Parser<IJavaScriptUnit>() {
          IJavaScriptProject project;

          @Override
          public void setProject(IProject project) {
            this.project = JavaScriptCore.create(project);
          }
          
          @Override
          public void processASTs(Map<IJavaScriptUnit, EclipseSourceFileModule> files, Function<Object[], Boolean> errors) {
            final ASTVisitor visitor = new ASTVisitor() {
              private final CallHierarchy ch = CallHierarchy.getDefault();
              @Override
              public boolean visit(FunctionDeclaration node) {
                try {
                if (node.resolveBinding() != null) {
                  IJavaScriptElement elt = node.resolveBinding().getJavaElement();
                  if (elt instanceof IFunction) 
                    try {
                      MethodWrapper mw = ch.getCallerRoot((IFunction) elt);
                      MethodWrapper calls[] = mw.getCalls(new NullProgressMonitor());
                      if (calls != null && calls.length > 0) {
                        System.err.println("calls: for " + elt);
                        for(MethodWrapper call : calls) {
                          System.err.println("calls: " + call.getMember());
                          if (! info.cg.containsNode((IFunction)elt)) {
                            info.cg.addNode((IFunction)elt);
                          }
                          if (! info.cg.containsNode(call.getMember())) {
                            info.cg.addNode(call.getMember());
                          }
                          info.cg.addEdge(call.getMember(), (IFunction)elt);
                        }
                      }
                    } catch (Throwable e) {
                      // Eclipse does whatever it wants, and we ignore stuff :)
                    }
                  }
                } catch (RuntimeException e) {
                  
                }
                // TODO Auto-generated method stub
                return super.visit(node);
              }

              @Override
              public boolean visit(FunctionRef node) {
                System.err.println(node.resolveBinding().getJavaElement());
                // TODO Auto-generated method stub
                return super.visit(node);
              }

              @Override
              public boolean visit(FunctionInvocation node) {
                info.calls.add(node);
                return super.visit(node);
              }
              
            };
       
            if (useCreateASTs) {
              ASTParser parser = ASTParser.newParser(AST.JLS3);
              parser.setProject(project);
              parser.setResolveBindings(true);
              parser.createASTs(files.keySet().toArray(new IJavaScriptUnit[files.size()]), new String[0], new ASTRequestor() {
                @Override
                public void acceptAST(IJavaScriptUnit source, JavaScriptUnit ast) {
                  ast.accept(visitor);
                }
              }, null);
            } else {
              for (Map.Entry<IJavaScriptUnit, EclipseSourceFileModule> f : files.entrySet()) {
                ASTParser parser = ASTParser.newParser(AST.JLS3);
                parser.setProject(project);
                parser.setResolveBindings(true);
                parser.setSource(f.getKey());
                try {
                  ASTNode ast = parser.createAST(null);
                  ast.accept(visitor);
                } catch (Throwable e) {
                  System.err.println("trouble with " + f.getValue() + ": " + e.getMessage());
                }
              }
            }
          }
        };
      }
    });
    return info;
  }

}
