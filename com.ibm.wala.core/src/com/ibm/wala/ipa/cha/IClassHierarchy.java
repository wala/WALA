package com.ibm.wala.ipa.cha;

import java.util.Collection;
import java.util.Set;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

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
