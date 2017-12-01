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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
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
import com.ibm.wala.ide.util.JdtPosition;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * A SourceModuleTranslator whose implementation of loadAllSources() uses the PolyglotFrontEnd pseudo-compiler to generate DOMO IR
 * for the sources in the compile-time classpath.
 * 
 * @author rfuhrer
 */
// remove me comment: Jdt little-case = not OK, upper case = OK
public class JDTSourceModuleTranslator implements SourceModuleTranslator {
  private final class JdtAstToIR extends ASTRequestor {
    private final Entry<IProject, Map<ICompilationUnit, EclipseSourceFileModule>> proj;

    private JdtAstToIR(Entry<IProject, Map<ICompilationUnit, EclipseSourceFileModule>> proj) {
      this.proj = proj;
    }

    @Override
    public void acceptAST(ICompilationUnit source, CompilationUnit ast) {

      try {
        JDTJava2CAstTranslator<JdtPosition> jdt2cast = makeCAstTranslator(ast, proj.getValue().get(source).getIFile(), source.getUnderlyingResource().getLocation().toOSString());
        final Java2IRTranslator java2ir = makeIRTranslator();
        java2ir.translate(proj.getValue().get(source), jdt2cast.translateToCAst());
      } catch (JavaModelException e) {
        e.printStackTrace();
      }

      if (! "true".equals(System.getProperty("wala.jdt.quiet"))) {
        IProblem[] problems = ast.getProblems();
        int length = problems.length;
        if (length > 0) {
          StringBuffer buffer = new StringBuffer();
          for (int i = 0; i < length; i++) {
            buffer.append(problems[i].getMessage());
            buffer.append('\n');
          }
          if (length != 0)
            System.err.println("Unexpected problems in " + source.getElementName() + buffer.toString());
        }
      }
    }
  }

  protected boolean dump;
  protected JDTSourceLoaderImpl sourceLoader;

  public JDTSourceModuleTranslator(AnalysisScope scope, JDTSourceLoaderImpl sourceLoader) {
    this(scope, sourceLoader, false);
  }

  public JDTSourceModuleTranslator(AnalysisScope scope, JDTSourceLoaderImpl sourceLoader, boolean dump) {
    computeClassPath(scope);
    this.sourceLoader = sourceLoader;
    this.dump = dump;
  }

  private static void computeClassPath(AnalysisScope scope) {
    StringBuffer buf = new StringBuffer();

    ClassLoaderReference cl = scope.getApplicationLoader();

    while (cl != null) {
      List<Module> modules = scope.getModules(cl);

      for (Module m : modules) {
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

  @Override
  public void loadAllSources(Set<ModuleEntry> modules) {
    // TODO: we might need one AST (-> "Object" class) for all files.
    // TODO: group by project and send 'em in

    // sort files into projects
    Map<IProject, Map<ICompilationUnit,EclipseSourceFileModule>> projectsFiles = new HashMap<>();
    for (ModuleEntry m : modules) {
      assert m instanceof EclipseSourceFileModule : "Expecing EclipseSourceFileModule, not " + m.getClass();
      EclipseSourceFileModule entry = (EclipseSourceFileModule) m;
      IProject proj = entry.getIFile().getProject();
      if (!projectsFiles.containsKey(proj)) {
        projectsFiles.put(proj, new HashMap<ICompilationUnit,EclipseSourceFileModule>());
      }
      projectsFiles.get(proj).put(JavaCore.createCompilationUnitFrom(entry.getIFile()), entry);
    }

    final ASTParser parser = ASTParser.newParser(AST.JLS8);
 
    for (final Map.Entry<IProject,Map<ICompilationUnit,EclipseSourceFileModule>> proj : projectsFiles.entrySet()) {
      parser.setProject(JavaCore.create(proj.getKey()));
      parser.setResolveBindings(true);
 
      Set<ICompilationUnit> units = proj.getValue().keySet();
      parser.createASTs(units.toArray(new ICompilationUnit[units.size()]), new String[0], new JdtAstToIR(proj), null);

    }
  }

  protected Java2IRTranslator makeIRTranslator() {
    return new Java2IRTranslator(sourceLoader);
  }

  protected JDTJava2CAstTranslator<JdtPosition> makeCAstTranslator(CompilationUnit cu, final IFile sourceFile, String fullPath) {
    return new JDTJava2CAstTranslator<JdtPosition>(sourceLoader, cu, fullPath, false, dump) {
      @Override
      public JdtPosition makePosition(int start, int end) {
        return new JdtPosition(start, end, this.cu.getLineNumber(start), this.cu.getLineNumber(end), sourceFile, this.fullPath);
      }
    };
  }

}
