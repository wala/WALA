package com.ibm.wala.cast.test;

import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.Trace;

import com.ibm.wala.cast.ipa.callgraph.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.loader.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.SSAOptions.*;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;

public abstract class TestCAstTranslator extends WalaTestCase {

  protected static class TranslatorAssertions {
    private final Set classes = new HashSet();
    private final Map supers = new HashMap();
    private final Set instanceFields = new HashSet();
    private final Set staticFields = new HashSet();
    private final Map instanceMethods = new HashMap();
    private final Map staticMethods = new HashMap(); 

    private Set getClasses() { return classes; }
    private Map getSuperClasses() { return supers; }
    private Set getInstanceFields() { return instanceFields; }
    private Set getStaticFields() { return staticFields; }
    private Map getInstanceMethods() { return instanceMethods; }
    private Map getStaticMethods() { return staticMethods; }

    public TranslatorAssertions(Object[][] data) {
      for(int dataIndex = 0; dataIndex < data.length; dataIndex++) {
	Object[] entry = data[dataIndex];
	String clsName = (String)entry[0];
	this.classes.add( clsName );

	String superName = (String)entry[1];
	this.supers.put( clsName, superName );

	String[] instanceFields = (String[])entry[2];
	if (instanceFields != null) {
	  for(int i = 0; i < instanceFields.length; i++) {
	    this.instanceFields.add(Pair.make(clsName, instanceFields[i]));
	  }
	}

	String[] staticFields = (String[])entry[3];
	if (staticFields != null) {
	  for(int i = 0; i < staticFields.length; i++) {
	    this.staticFields.add(Pair.make(clsName, staticFields[i]));
	  }
	}

	Pair[] instanceMethods = (Pair[])entry[4];
	if (instanceMethods != null) {
	  for(int i = 0; i < instanceMethods.length; i++) {
	    this.instanceMethods.put(
	      Pair.make(clsName, instanceMethods[i].fst), 
	      instanceMethods[i].snd);
	  }
	}

	Pair[] staticMethods = (Pair[])entry[5];
	if (staticMethods != null) {
	  for(int i = 0; i < staticMethods.length; i++) {
	    this.staticMethods.put(
	      Pair.make(clsName, staticMethods[i].fst), 
	      staticMethods[i].snd);
	  }
	}
      }
    }	
  }

  protected abstract Language getLanguage();

  protected abstract String getTestPath();

  protected abstract SingleClassLoaderFactory getClassLoaderFactory();

  protected final IRFactory factory = AstIRFactory.makeDefaultFactory(false);
  protected final SSAOptions options = new SSAOptions();

  public ClassHierarchy runTranslator(SourceFileModule[] fileNames) 
      throws Exception 
  {
    SingleClassLoaderFactory loaders = getClassLoaderFactory();

    CAstAnalysisScope scope = new CAstAnalysisScope(fileNames, loaders);

    ClassHierarchy cha = 
      ClassHierarchy.make(
        scope,
	loaders,
	getLanguage());

    return cha;
  }

  protected void dump(ClassHierarchy cha) {
    for(Iterator clss = cha.iterator(); clss.hasNext(); ) {
      IClass cls = (IClass)clss.next();
      Trace.println("class " + cls);
      for(Iterator flds = cls.getDeclaredInstanceFields().iterator();
	  flds.hasNext(); )
      {
	IField fld = (IField)flds.next();
	Trace.println("instance field " + fld);
      }
      for(Iterator flds = cls.getDeclaredStaticFields().iterator(); 
	  flds.hasNext(); )
      {
	IField fld = (IField)flds.next();
	Trace.println("static field " + fld);
      }
      for(Iterator mths = cls.getDeclaredMethods().iterator();
	  mths.hasNext(); ) 
      {
	IMethod mth = (IMethod)mths.next();
	if (mth.isStatic()) Trace.print("static ");
	Trace.println("method " + mth);
	for(int i = 0; i < mth.getNumberOfParameters(); i++) {
	  Trace.println("param " + i + ": " + mth.getParameterType(i));
	}
	Trace.println(factory.makeIR(mth, Everywhere.EVERYWHERE, options));
      }
    }
  }

  public void 
    checkAssertions(ClassHierarchy cha, TranslatorAssertions assertions) 
  {
    Set classes = assertions.getClasses();
    Map supers = assertions.getSuperClasses();
    Set instanceFields = assertions.getInstanceFields();
    Set staticFields = assertions.getStaticFields();
    Map instanceMethods = assertions.getInstanceMethods();
    Map staticMethods = assertions.getStaticMethods();

    int clsCount = 0;
    for(Iterator clss = cha.iterator(); clss.hasNext(); ) {
      IClass cls = (IClass)clss.next();
      clsCount++;
      Assert.assertTrue(
        "found class " + cls.getName().toString(),
	classes.contains( cls.getName().toString() ) );
      
      try {
	if (cls.getSuperclass() == null) {
	  Assert.assertTrue( 
	    cls.getName() + " has no superclass",
	    supers.get( cls.getName() ) == null  );
	} else {
	  Assert.assertTrue(
	    "super of "+cls.getName()+" is "+cls.getSuperclass().getName(),
	    supers.get(cls.getName().toString() )
	      .equals(cls.getSuperclass().getName().toString()));
	}
      } catch (ClassHierarchyException e) {
	Assert.assertTrue(false);
      }

      for(Iterator flds = cls.getDeclaredInstanceFields().iterator();
	  flds.hasNext(); )
      {
	IField fld = (IField)flds.next();
	Assert.assertTrue(
	  cls.getName() + " has field " + fld.getName(),
	  instanceFields.contains(
	    Pair.make(cls.getName().toString(), fld.getName().toString())));
      }

      for(Iterator flds = cls.getDeclaredStaticFields().iterator(); 
	  flds.hasNext(); )
      {
	IField fld = (IField)flds.next();
	Assert.assertTrue(
	  cls.getName() + " has static field " + fld.getName(),
	  staticFields.contains(
	    Pair.make(cls.getName().toString(), fld.getName().toString())));
      }

      for(Iterator mths = cls.getDeclaredMethods().iterator(); 
	  mths.hasNext(); ) 
      {
	IMethod mth = (IMethod)mths.next();
	Integer np = new Integer(mth.getNumberOfParameters());
	Pair key =
	  Pair.make(cls.getName().toString(), mth.getName().toString());

	if (mth.isStatic()) {
	  Assert.assertTrue(
	    cls.getName() + " has static method " + mth.getName(),
	    staticMethods.containsKey(key));
	  Assert.assertTrue(
	    cls.getName()+"::"+mth.getName() + " has " + np + " parameters",
	    staticMethods.get(key).equals(np));
	} else {
	  Assert.assertTrue(
	    cls.getName() + " has method " + mth.getName(),
	    instanceMethods.containsKey(key));
	  Assert.assertTrue(
	    cls.getName()+"::"+mth.getName() + " has " + np + " parameters",
	    instanceMethods.get(key).equals(np));
	}
      }
    }
    
    Assert.assertTrue(
      "want " + classes.size() + " classes",
      clsCount == classes.size());
  }

  protected void testInternal(String[] args, TranslatorAssertions assertions)
      throws Exception
  {
    String testPath = getTestPath();
    SourceFileModule[] fileNames = new SourceFileModule[ args.length ];
    for(int i = 0; i < args.length; i++) {
      if (new File(args[i]).exists()) {
	fileNames[i] = 
	  Util.makeSourceModule(new URL("file:"+args[i]), args[i]);
      } else if (new File(testPath + args[i]).exists()) {
	fileNames[i] = 
	  Util.makeSourceModule(new URL("file:"+testPath+args[i]), args[i]);
      } else {
	URL url = getClass().getClassLoader().getResource(args[i]);
	fileNames[i] = Util.makeSourceModule(url, args[i]);
      }
      Assert.assertTrue(args[i], fileNames[i] != null);
    }
    
    ClassHierarchy cha = runTranslator( fileNames );
    dump( cha ); 
    if (assertions != null) {
      checkAssertions( cha, assertions );
    } else {
      Trace.println("WARNING: no assertions for " + getClass());
    }
  }

  protected void testInternal(String arg, TranslatorAssertions assertions) {
    try {
      testInternal(new String[]{ arg }, assertions);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.assertTrue(e.toString(), false);
    }
  }
}
