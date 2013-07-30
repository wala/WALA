package com.ibm.wala.ide.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.classLoader.FileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ide.classloader.EclipseSourceDirectoryTreeModule;
import com.ibm.wala.util.collections.MapUtil;

public class EclipseWebProjectPath extends JavaScriptEclipseProjectPath {

  public EclipseWebProjectPath(IJavaScriptProject p) throws IOException, CoreException {
    super(p);
  }

  @Override
  protected void resolveSourcePathEntry(com.ibm.wala.ide.util.EclipseProjectPath.ILoader loader, boolean includeSource, boolean cpeFromMainProject, IPath p, IPath o, String fileExtension) {
      List<Module> s = MapUtil.findOrCreateList(modules, loader);
      Iterator<FileModule> htmlPages = new EclipseSourceDirectoryTreeModule(p, "html").getEntries();
      while (htmlPages.hasNext()) {
        FileModule htmlPage = htmlPages.next();
        Set<MappedSourceModule> scripts;
        String urlString = "file://" + htmlPage.getAbsolutePath();
        try {
          scripts = WebUtil.extractScriptFromHTML(new URL(urlString)).fst;
          s.addAll(scripts);
        } catch (MalformedURLException e1) {
          assert false : "internal error constructing URL " + urlString;
        } catch (Error e1) {
          System.err.print("skipping " + htmlPage.getAbsolutePath() + ": " + e1.warning);
        } 
      }
  }
}
