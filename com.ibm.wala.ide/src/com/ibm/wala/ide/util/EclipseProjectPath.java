/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ide.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ide.classloader.EclipseSourceDirectoryTreeModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;

/**
 * Representation of an analysis scope from an Eclipse project.
 * 
 * We set up classloaders as follows:
 * <ul>
 * <li>The project being analyzed is in the Application Loader
 * <li>Frameworks, application libraries, and linked projects on which the main project depends are in the Extension loader
 * <li>System libraries are in the primordial loader.
 * <li>All source modules go in a special Source loader. This includes source from linked projects if
 * SOURCE_FOR_PROJ_AND_LINKED_PROJS is specified.
 * </ul>
 */
public abstract class EclipseProjectPath<E, P> {

  protected abstract P makeProject(IProject p);
  
  protected abstract E resolve(E entry);
  
  protected abstract void resolveClasspathEntry(P project, E entry, ILoader loader, boolean includeSource, boolean cpeFromMainProject);

  protected abstract void resolveProjectClasspathEntries(P project, boolean includeSource);


  public interface ILoader {
    ClassLoaderReference ref();
  }
  
  /**
   * Eclipse projects are modeled with 3 loaders, as described above.
   */
  public enum Loader implements ILoader {
    APPLICATION(ClassLoaderReference.Application), EXTENSION(ClassLoaderReference.Extension), PRIMORDIAL(
        ClassLoaderReference.Primordial);

    private ClassLoaderReference ref;

    Loader(ClassLoaderReference ref) {
      this.ref = ref;
    }

    @Override
    public ClassLoaderReference ref() {
      return ref;
    }
  }

  public enum AnalysisScopeType {
    NO_SOURCE, SOURCE_FOR_PROJ, SOURCE_FOR_PROJ_AND_LINKED_PROJS
  }

  /**
   * names of OSGi bundles already processed.
   */
  private final Set<String> bundlesProcessed = HashSetFactory.make();

  // SJF: Intentionally do not use HashMapFactory, since the Loader keys in the following must use
  // identityHashCode. TODO: fix this source of non-determinism?
  protected final Map<ILoader, List<Module>> modules = new HashMap<>();

  /**
   * Classpath entries that have already been resolved and added to the scope.
   */
  protected final Collection<E> alreadyResolved = HashSetFactory.make();

  /**
   * Which source files, if any, should be included in the analysis scope.
   */
  private final AnalysisScopeType scopeType;

  protected EclipseProjectPath(AnalysisScopeType scopeType) {
    this.scopeType = scopeType;
    for (ILoader loader : Loader.values()) {
      MapUtil.findOrCreateList(modules, loader);
    }
  }
    
  public EclipseProjectPath create(IProject project) throws CoreException, IOException {
    if (project == null) {
      throw new IllegalArgumentException("null project");
    }

    boolean includeSource = (scopeType != AnalysisScopeType.NO_SOURCE);
    
    resolveProjectClasspathEntries(makeProject(project), includeSource);
    if (isPluginProject(project)) {
      resolvePluginClassPath(project, includeSource);
    }
    
    return this;
  }


  protected void resolveLibraryPathEntry(ILoader loader, IPath p) {
    File file = makeAbsolute(p).toFile();
    JarFile j;
    try {
      j = new JarFile(file);
    } catch (ZipException z) {
      // a corrupted file. ignore it.
      return;
    } catch (IOException z) {
      // should ignore directories as well..
      return;
    }
    if (isPrimordialJarFile()) {
      List<Module> s = MapUtil.findOrCreateList(modules, loader);
      s.add(file.isDirectory() ? (Module) new BinaryDirectoryTreeModule(file) : (Module) new JarFileModule(j));
    }
  }

  protected void resolveSourcePathEntry(ILoader loader, boolean includeSource, boolean cpeFromMainProject, IPath p, IPath o, IPath[] excludePaths, String fileExtension) {
    if (includeSource) {
      List<Module> s = MapUtil.findOrCreateList(modules, loader);
      s.add(new EclipseSourceDirectoryTreeModule(p, excludePaths, fileExtension));
    } else if (o != null) {
      File output = makeAbsolute(o).toFile();
      List<Module> s = MapUtil.findOrCreateList(modules, cpeFromMainProject ? Loader.APPLICATION : loader);
      s.add(new BinaryDirectoryTreeModule(output));
    }
  }

  protected void resolveProjectPathEntry(boolean includeSource, IPath p) {
    IPath projectPath = makeAbsolute(p);
    IWorkspace ws = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = ws.getRoot();
    IProject project = (IProject) root.getContainerForLocation(projectPath);
    try {
      P javaProject = makeProject(project);
      if (javaProject != null) {
        if (isPluginProject(project)) {
          resolvePluginClassPath(project, includeSource);
        }
        resolveProjectClasspathEntries(javaProject, scopeType == AnalysisScopeType.SOURCE_FOR_PROJ_AND_LINKED_PROJS ? includeSource : false);
      }
    } catch (CoreException e1) {
      e1.printStackTrace();
      Assertions.UNREACHABLE();
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  /**
   * traverse the bundle description for an Eclipse project and populate the analysis scope accordingly
   */
  private void resolvePluginClassPath(IProject p, boolean includeSource) throws CoreException, IOException {
    IPluginModelBase model = findModel(p);
    if (!model.isInSync() || model.isDisposed()) {
      model.load();
    }

    BundleDescription bd = model.getBundleDescription();

    if (bd == null) {
      // temporary debugging code; remove once we figure out what the heck is going on here --MS
      System.err.println("model.isDisposed(): " + model.isDisposed());
      System.err.println("model.isInSync(): " + model.isInSync());
      System.err.println("model.isEnabled(): " + model.isEnabled());
      System.err.println("model.isLoaded(): " + model.isLoaded());
      System.err.println("model.isValid(): " + model.isValid());
    }
    for (int i = 0; i < 3 && bd == null; i++) {
      // Uh oh. bd is null. Go to sleep, cross your fingers, and try again.
      // This is horrible. We can't figure out the race condition yet which causes this to happen.
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        // whatever.
      }
      bd = findModel(p).getBundleDescription();
    }

    if (bd == null) {
      throw new IllegalStateException("bundle description was null for " + p);
    }
    resolveBundleDescriptionClassPath(makeProject(p), bd, Loader.APPLICATION, includeSource);
  }

  /**
   * traverse a bundle description and populate the analysis scope accordingly
   */
  private void resolveBundleDescriptionClassPath(P project, BundleDescription bd, Loader loader, boolean includeSource) throws CoreException,
      IOException {
    assert bd != null;
    if (alreadyProcessed(bd)) {
      return;
    }
    bundlesProcessed.add(bd.getName());

    // handle the classpath entries for bd
    ArrayList<IClasspathEntry> l = new ArrayList<>();
    ClasspathUtilCore.addLibraries(findModel(bd), l);
    resolveClasspathEntries(project, l, loader, includeSource, false);

    // recurse to handle dependencies. put these in the Extension loader
    for (ImportPackageSpecification b : bd.getImportPackages()) {
      resolveBundleDescriptionClassPath(project, b.getBundle(), Loader.EXTENSION, includeSource);
    }
    for (BundleDescription b : bd.getResolvedRequires()) {
      resolveBundleDescriptionClassPath(project, b, Loader.EXTENSION, includeSource);
    }
    for (BundleDescription b : bd.getFragments()) {
      resolveBundleDescriptionClassPath(project, b, Loader.EXTENSION, includeSource);
    }
  }

  /**
   * have we already processed a particular bundle description?
   */
  private boolean alreadyProcessed(BundleDescription bd) {
    return bundlesProcessed.contains(bd.getName());
  }

  /**
   * Is javaProject a plugin project?
   */
  private static boolean isPluginProject(IProject project) {
    IPluginModelBase model = findModel(project);
    if (model == null) {
      return false;
    }
    if (model.getPluginBase().getId() == null) {
      return false;
    }
    return true;
  }

  /**
   * @return true if the given jar file should be handled by the Primordial loader. If false, other provisions should be made to add
   *         the jar file to the appropriate component of the AnalysisScope. Subclasses can override this method.
   */
  protected boolean isPrimordialJarFile() {
    return true;
  }

  @SuppressWarnings("unchecked")
  protected void resolveClasspathEntries(P project, List l, ILoader loader, boolean includeSource, boolean entriesFromTopLevelProject) {
    for (int i = 0; i < l.size(); i++) {
      resolveClasspathEntry(project, resolve((E)l.get(i)), loader, includeSource, entriesFromTopLevelProject);
    }
  }

  public static IPath makeAbsolute(IPath p) {
    IPath absolutePath = p;
    if (p.toFile().exists()) {
      return p;
    }

    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(p);
    if (resource != null && resource.exists()) {
      absolutePath = resource.getLocation();
    }
    return absolutePath;
  }
  
  /**
   * Convert this path to a WALA analysis scope
   * 
   * @throws IOException
   */
  public AnalysisScope toAnalysisScope(ClassLoader classLoader, File exclusionsFile) throws IOException {
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(AbstractAnalysisEngine.SYNTHETIC_J2SE_MODEL, exclusionsFile,
        classLoader);
    return toAnalysisScope(scope);
  }

  public AnalysisScope toAnalysisScope(AnalysisScope scope) {

    for (ILoader loader : modules.keySet()) {
      for (Module m : modules.get(loader)) {
        scope.addToScope(loader.ref(), m);
      }
    }
    return scope;

  }

  public AnalysisScope toAnalysisScope(final File exclusionsFile) throws IOException {
    return toAnalysisScope(getClass().getClassLoader(), exclusionsFile);
  }

  public AnalysisScope toAnalysisScope() throws IOException {
    return toAnalysisScope(getClass().getClassLoader(), null);
  }

  public Collection<Module> getModules(ILoader loader) {
    return Collections.unmodifiableCollection(modules.get(loader));
  }

  @Override
  public String toString() {
    try {
      return toAnalysisScope((File) null).toString();
    } catch (IOException e) {
      e.printStackTrace();
      return "Error in toString()";
    }
  }

  private static IPluginModelBase findModel(IProject p) {
    // PluginRegistry is specific to Eclipse 3.3+. Use PDECore for compatibility with 3.2
    // return PluginRegistry.findModel(p);
    return PDECore.getDefault().getModelManager().findModel(p);
  }

  private static IPluginModelBase findModel(BundleDescription bd) {
    // PluginRegistry is specific to Eclipse 3.3+. Use PDECore for compatibility with 3.2
    // return PluginRegistry.findModel(bd);
    return PDECore.getDefault().getModelManager().findModel(bd);
  }
}
