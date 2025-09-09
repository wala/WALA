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
package com.ibm.wala.cast.java.translator.jdt.ecj;

import com.ibm.wala.cast.ir.translator.CAstGenerator;
import com.ibm.wala.cast.java.translator.jdt.JDTJava2CAstTranslator;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;
import com.ibm.wala.classLoader.DirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.StringFilter;
import com.ibm.wala.util.io.TemporaryFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

/**
 * A SourceModuleTranslator whose implementation of loadAllSources() uses the PolyglotFrontEnd
 * pseudo-compiler to generate DOMO IR for the sources in the compile-time classpath.
 *
 * @author rfuhrer
 */
// remove me comment: Jdt little-case = not OK, upper case = OK
public class ECJSourceModuleProcessor implements CAstGenerator {
  protected class ECJJavaToCAstTranslator extends JDTJava2CAstTranslator<Position> {
    public ECJJavaToCAstTranslator(
        ClassLoaderReference sourceLoader,
        CompilationUnit astRoot,
        String fullPath,
        boolean replicateForDoLoops,
        boolean dump) {
      super(sourceLoader, astRoot, fullPath, replicateForDoLoops, dump);
    }

    @Override
    public Position makePosition(int start, int end) {
      return new AbstractSourcePosition() {

        @Override
        public URL getURL() {
          try {
            return new URL("file://" + fullPath);
          } catch (MalformedURLException e) {
            assert false : fullPath;
            return null;
          }
        }

        @Override
        public Reader getReader() throws IOException {
          return new InputStreamReader(getURL().openConnection().getInputStream());
        }

        @Override
        public int getFirstLine() {
          return cu.getLineNumber(start);
        }

        @Override
        public int getLastLine() {
          return cu.getLineNumber(end);
        }

        @Override
        public int getFirstCol() {
          return cu.getColumnNumber(start);
        }

        @Override
        public int getLastCol() {
          return cu.getColumnNumber(end);
        }

        @Override
        public int getFirstOffset() {
          return start;
        }

        @Override
        public int getLastOffset() {
          return end;
        }
      };
    }
  }

  protected boolean dump;
  protected ClassLoaderReference sourceLoaderRef;
  private final String[] sources;
  private final String[] libs;
  protected final StringFilter exclusions;

  public ECJSourceModuleProcessor(AnalysisScope scope, ClassLoaderReference sourceLoader) {
    this(scope, sourceLoader, false);
  }

  public ECJSourceModuleProcessor(
      AnalysisScope scope, ClassLoaderReference sourceLoader, boolean dump) {
    this.sourceLoaderRef = sourceLoader;
    this.dump = dump;

    Pair<String[], String[]> paths = computeClassPath(scope);
    sources = paths.fst;
    libs = paths.snd == null || paths.snd.length < 1 ? null : paths.snd;

    this.exclusions = scope.getExclusions();
  }

  private static Pair<String[], String[]> computeClassPath(AnalysisScope scope) {
    List<String> sources = new ArrayList<>();
    List<String> libs = new ArrayList<>();
    for (ClassLoaderReference cl : scope.getLoaders()) {

      while (cl != null) {
        List<Module> modules = scope.getModules(cl);

        for (Module m : modules) {
          if (m instanceof JarFileModule) {
            JarFileModule jarFileModule = (JarFileModule) m;

            libs.add(jarFileModule.getAbsolutePath());
          } else if (m instanceof JarStreamModule) {
            try {
              File F = File.createTempFile("tmp", "jar");
              F.deleteOnExit();
              TemporaryFile.streamToFile(F, ((JarStreamModule) m));
              libs.add(F.getAbsolutePath());
            } catch (IOException e) {
              assert false : e;
            }
          } else if (m instanceof DirectoryTreeModule) {
            DirectoryTreeModule directoryTreeModule = (DirectoryTreeModule) m;

            sources.add(directoryTreeModule.getPath());
          } else {
            // Assertions.UNREACHABLE("Module entry is neither jar file nor directory");
          }
        }
        cl = cl.getParent();
      }
    }

    if (libs.isEmpty()) {
      for (String l : WalaProperties.getJ2SEJarFiles()) {
        libs.add(l);
      }
    }

    /*
     String cacheDir = System.getProperty("java.io.tmpdir");
     for(String l : libs) {
     	if (l.endsWith("jmod")) {
     		try (JarFile jmod = new JarFile(l)) {
     			new JarOutputStream(new FileOutputStream(cacheDir + "/" + new File(l).getName() + ".jar"))
    	Enumeration<JarEntry> clss = jmod.entries();
    	while (clss.hasMoreElements()) {
    		JarEntry je = clss.nextElement();
    		jmod.getInputStream(je);
    	}
    } catch (IOException e) {
    	assert false : e;
    }
     	}
     }
     */

    return Pair.make(sources.toArray(new String[0]), libs.toArray(new String[0]));
  }

  /*
   * Project -> AST code from org.eclipse.jdt.core.tests.performance
   */

  @Override
  public Set<CAstEntity> translate(Set<ModuleEntry> modules) {
    Set<CAstEntity> entities = HashSetFactory.make();
    loadAllSources(
        modules,
        (sourceMap) ->
            new FileASTRequestor() {
              @Override
              public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                entities.add(
                    new ECJJavaToCAstTranslator(sourceLoaderRef, ast, sourceFilePath, false, false)
                        .translateToCAst());
              }
            });
    return entities;
  }

  public void loadAllSources(
      Set<ModuleEntry> modules, Function<Map<String, ModuleEntry>, FileASTRequestor> xlator) {
    List<String> sources = new ArrayList<>();
    Map<String, ModuleEntry> sourceMap = HashMapFactory.make();
    for (ModuleEntry m : modules) {
      if (m.isSourceFile()) {
        SourceFileModule s = (SourceFileModule) m;
        sourceMap.put(s.getAbsolutePath(), s);
        sources.add(s.getAbsolutePath());
      }
    }

    String[] sourceFiles = sources.toArray(new String[0]);
    @SuppressWarnings("deprecation")
    final ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setResolveBindings(true);
    parser.setEnvironment(libs, this.sources, null, true);
    Hashtable<String, String> options = JavaCore.getOptions();
    options.put(JavaCore.COMPILER_SOURCE, "1.8");
    options.put(JavaCore.COMPILER_COMPLIANCE, "1.8");
    parser.setCompilerOptions(options);
    parser.createASTs(sourceFiles, null, new String[0], xlator.apply(sourceMap), null);
  }

  protected JDTJava2CAstTranslator<Position> makeCAstTranslator(
      CompilationUnit cu, String fullPath) {
    return new ECJJavaToCAstTranslator(sourceLoaderRef, cu, fullPath, false, dump);
  }
}
