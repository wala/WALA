package com.ibm.wala.ipa.cha;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.types.*;

import java.util.*;

public interface IClassHierarchy extends Iterable<IClass> {

  public ClassLoaderFactory getFactory();

  public AnalysisScope getScope();

  public IClassLoader[] getLoaders();

  public IClassLoader getLoader(ClassLoaderReference loaderRef);


  public boolean addClass(IClass klass);


  public int getNumberOfClasses();

  public boolean isRootClass(IClass c);

  public IClass getRootClass();

  public int getNumber(IClass c);


  public Collection<IMethod> getPossibleTargets(MethodReference ref);


  public IMethod resolveMethod(MethodReference m);

  public IField resolveField(FieldReference f);

  public IField resolveField(IClass klass, FieldReference f);

  public IMethod resolveMethod(IClass receiverClass, Selector selector);

  public IClass lookupClass(TypeReference A);

  public boolean isSyntheticClass(IClass c);

  public boolean isInterface(TypeReference type);


  public IClass getLeastCommonSuperclass(IClass A, IClass B);

  public TypeReference getLeastCommonSuperclass(TypeReference A, TypeReference B);

  public boolean isSubclassOf(IClass c, IClass T);

  public boolean implementsInterface(IClass c, TypeReference T);

  public Collection<IClass> computeSubClasses(TypeReference type);

  public Collection<TypeReference> getJavaLangErrorTypes();

  public Set<IClass> getImplementors(TypeReference type);

  public int getNumberOfImmediateSubclasses(IClass klass);

  public Collection<IClass> getImmediateSubclasses(IClass klass);

  public boolean isAssignableFrom(IClass c1, IClass c2);

}
