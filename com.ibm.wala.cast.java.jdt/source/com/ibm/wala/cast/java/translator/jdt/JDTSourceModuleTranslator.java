/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * WALA JDT Frontend is Copyright (c) 2008 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.cast.java.translator.jdt;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.wala.cast.java.translator.Java2IRTranslator;
import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.classLoader.DirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ide.classloader.EclipseSourceFileModule;
import com.ibm.wala.ide.util.HeadlessUtil;
import com.ibm.wala.ide.util.HeadlessUtil.EclipseCompiler;
import com.ibm.wala.ide.util.HeadlessUtil.Parser;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.functions.Function;

/**
 * A SourceModuleTranslator whose implementation of loadAllSources() uses the PolyglotFrontEnd pseudo-compiler to generate DOMO IR
 * for the sources in the compile-time classpath.
 * 
 * @author rfuhrer
 */
// remove me comment: Jdt little-case = not OK, upper case = OK
public class JDTSourceModuleTranslator implements SourceModuleTranslator {
  protected JDTSourceLoaderImpl sourceLoader;

  public JDTSourceModuleTranslator(AnalysisScope scope, JDTSourceLoaderImpl sourceLoader) {
    computeClassPath(scope);
    this.sourceLoader = sourceLoader;
  }

  private void computeClassPath(AnalysisScope scope) {
    StringBuffer buf = new StringBuffer();

    ClassLoaderReference cl = scope.getApplicationLoader();

    while (cl != null) {
      List<Module> modules = scope.getModules(cl);

      for (Iterator<Module> iter = modules.iterator(); iter.hasNext();) {
        Module m = (Module) iter.next();

        if (buf.length() > 0)
          buf.append(File.pathSeparator);
        if (m instanceof JarFileModule) {
          JarFileModule jarFileModule = (JarFileModule) m;

          buf.append(jarFileModule.getAbsolutePath());
        } else if (m instanceof DirectoryTreeModule) {
          DirectoryTreeModule directoryTreeModule = (DirectoryTreeModule) m;

          buf.append(directoryTreeModule.getPath());
        } else
          Assertions.UNREACHABLE("Module entry is neither jar file nor directory");
      }
      cl = cl.getParent();
    }
  }
  
  /*
   * Project -> AST code from org.eclipse.jdt.core.tests.performance
   */

  public void loadAllSources(Set<ModuleEntry> modules) {
    // TODO: we might need one AST (-> "Object" class) for all files.
    // TODO: group by project and send 'em in
    System.out.println(modules);

    HeadlessUtil.parseModules(modules, new EclipseCompiler<ICompilationUnit, CompilationUnit>() {
      @Override
      public ICompilationUnit getCompilationUnit(IFile file) {
        return JavaCore.createCompilationUnitFrom(file);
      }
      @Override
      public Parser<ICompilationUnit, CompilationUnit> getParser() {
        return new Parser<ICompilationUnit, CompilationUnit>() {
          final ASTParser parser;
          {
            parser = ASTParser.newParser(AST.JLS3);
            parser.setResolveBindings(true);
          }
          @Override
          public void setProject(IProject project) {
            parser.setProject(JavaCore.create(project));
          }
          @Override
          public void processASTs(final Map<ICompilationUnit,EclipseSourceFileModule> files, final Function<Object[], Boolean> errors) {
            parser.createASTs(files.keySet().toArray(new ICompilationUnit[files.size()]), new String[0], new ASTRequestor() {
              public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
                try {
                  JDTJava2CAstTranslator jdt2cast = makeCAstTranslator(ast, source.getUnderlyingResource().getLocation().toOSString());
                  final Java2IRTranslator java2ir = makeIRTranslator();
                  java2ir.translate(files.get(source), jdt2cast.translateToCAst());
                } catch (JavaModelException e) {
                  e.printStackTrace();
                }

                errors.apply(ast.getProblems());
              }
            }, null);
          }
        };
      }
    });
  }

  protected Java2IRTranslator makeIRTranslator() {
    return new Java2IRTranslator(sourceLoader);
  }

  protected JDTJava2CAstTranslator makeCAstTranslator(CompilationUnit cu, String fullPath) {
    return new JDTJava2CAstTranslator(sourceLoader, cu, fullPath, false);
  }

}
