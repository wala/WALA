package com.ibm.wala.util.config;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.properties.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

public class AnalysisScopeReader {

  private static final ClassLoader MY_CLASSLOADER = 
    AnalysisScopeReader.class.getClassLoader();

  private static final String BASIC_FILE =
    "J2SESyntheticModel.txt";

  public static AnalysisScope 
    read(String scopeFileName, String exclusionsFile, ClassLoader javaLoader) 
  {
    AnalysisScope scope = 
      AnalysisScope.createAnalysisScope(Collections.singleton(Language.JAVA));

    return read(scope, scopeFileName, exclusionsFile, javaLoader);
  }

  public static AnalysisScope 
    read(AnalysisScope scope,
	 String scopeFileName, 
	 String exclusionsFile, 
	 ClassLoader javaLoader) 
  {
    try {
      File scopeFile = FileProvider.getFile(scopeFileName, javaLoader);
      assert scopeFile.exists();

      String line;
      BufferedReader r = new BufferedReader(new FileReader(scopeFile));
      while ((line = r.readLine()) != null) {
	StringTokenizer toks = new StringTokenizer(line, "\n,");

	Atom loaderName = Atom.findOrCreateUnicodeAtom(toks.nextToken());
	Atom languageName = Atom.findOrCreateUnicodeAtom(toks.nextToken());
	ClassLoaderReference walaLoader =
	  new ClassLoaderReference(loaderName, languageName);

	String entryType = toks.nextToken();
	String entryPathname = toks.nextToken();
	if ("classFile".equals(entryType)) {
	  File cf = FileProvider.getFile(entryPathname, javaLoader);
	  scope.addClassFileToScope(walaLoader, cf);
	} else if ("sourceFile".equals(entryType)) {
	  File sf = FileProvider.getFile(entryPathname, javaLoader);
	  scope.addSourceFileToScope(walaLoader, sf, entryPathname);
	} else if ("binaryDir".equals(entryType)) {
	  File bd = FileProvider.getFile(entryPathname, javaLoader);
	  assert bd.isDirectory();
	  scope.addToScope(walaLoader, new BinaryDirectoryTreeModule(bd));
	} else if ("sourceDir".equals(entryType)) {
	  File sd = FileProvider.getFile(entryPathname, javaLoader);
	  assert sd.isDirectory();
	  scope.addToScope(walaLoader, new SourceDirectoryTreeModule(sd));
	} else if ("jarFile".equals(entryType)) {
	  Module M = FileProvider.getJarFileModule(entryPathname, javaLoader);
	  scope.addToScope(walaLoader, M);
	} else if ("loaderImpl".equals(entryType)) {
	  scope.setLoaderImpl(walaLoader, entryPathname);
	} else if ("stdlib".equals(entryType)) {
	  String[] stdlibs = WalaProperties.getJ2SEJarFiles();
	  for(int i = 0; i < stdlibs.length; i++) {
	    scope.addToScope(walaLoader, new JarFile(stdlibs[i]));
	  }
	} else {
	    Assertions.UNREACHABLE();
	}
      }
      
      if (exclusionsFile != null) {
	scope.setExclusions(new FileOfClasses(exclusionsFile, javaLoader));
      }

    } catch (IOException e) {
	Assertions.UNREACHABLE(e.toString());
    }

    return scope;
  }

  public static AnalysisScope makePrimordialScope(String exclusionsFile) {
    return read(BASIC_FILE, exclusionsFile, MY_CLASSLOADER);
  }

  public static AnalysisScope 
    makeJavaBinaryAnalysisScope(String classPath, String exclusionsFile) 
  {
    AnalysisScope scope = makePrimordialScope(exclusionsFile);
    ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);

    try {
      StringTokenizer paths = 
	new StringTokenizer(classPath, File.pathSeparator);
      while (paths.hasMoreTokens()) {
	String path = paths.nextToken();
	if (path.endsWith(".jar")) {
	  scope.addToScope(loader, new JarFile(path));
	} else {
	  File f = new File(path);
	  if (f.isDirectory()) {
	    scope.addToScope(loader, new BinaryDirectoryTreeModule(f));
	  } else {
	    scope.addClassFileToScope(loader, f);
	  }
	}
      }
    } catch (IOException e) {
      Assertions.UNREACHABLE(e.toString());
    }

    return scope;
  }
}
