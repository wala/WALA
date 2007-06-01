package com.ibm.wala.cast.ipa.cha;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.*;
import com.ibm.wala.util.warnings.*;

import java.util.*;

public class CrossLanguageClassHierarchy implements IClassHierarchy {

  private final ClassLoaderFactory loaderFactory;
    
  private final AnalysisScope analysisScope;

  private final Map hierarchies;

  public ClassLoaderFactory getFactory()  {
    return loaderFactory;
  }

  public AnalysisScope getScope() {
    return analysisScope;
  }

  private IClassHierarchy getHierarchy(Atom language) {
    return (IClassHierarchy) hierarchies.get(language);
  }

  private IClassHierarchy getHierarchy(IClassLoader loader) {
    return getHierarchy(loader.getLanguage().getName());
  }

  private IClassHierarchy getHierarchy(ClassLoaderReference loader) {
    return getHierarchy(loader.getLanguage());
  }

  private IClassHierarchy getHierarchy(IClass cls) {
    return getHierarchy(cls.getClassLoader());
  }

  private IClassHierarchy getHierarchy(TypeReference cls) {
    return getHierarchy(cls.getClassLoader());
  }

  private IClassHierarchy getHierarchy(MethodReference ref) {
    return getHierarchy(ref.getDeclaringClass());
  }

  private IClassHierarchy getHierarchy(FieldReference ref) {
    return getHierarchy(ref.getDeclaringClass());
  }

  public IClassLoader[] getLoaders() {
    Set loaders = new HashSet();
    for(Iterator ldrs = analysisScope.getLoaders().iterator(); 
	ldrs.hasNext(); )
    {
      loaders.add(getLoader((ClassLoaderReference)ldrs.next()));
    }

    return (IClassLoader[])loaders.toArray(new IClassLoader[loaders.size()]);
  }

  public IClassLoader getLoader(ClassLoaderReference loaderRef) {
    return getHierarchy(loaderRef).getLoader(loaderRef);
  }

  public boolean addClass(IClass klass) {
    return getHierarchy(klass).addClass(klass);
  }

  public int getNumberOfClasses() {
    int total = 0;
    for(Iterator ldrs = analysisScope.getLoaders().iterator(); 
	ldrs.hasNext(); )
    {
      total += 
	getLoader((ClassLoaderReference)ldrs.next()).getNumberOfClasses();
    }
    
    return total;
  }

  public boolean isRootClass(IClass c) {
    return getHierarchy(c).isRootClass(c);
  }

  public IClass getRootClass() {
    Assertions.UNREACHABLE();
    return null;
  }

  public int getNumber(IClass c) {
    return getHierarchy(c).getNumber(c);
  }

  public Collection<IMethod> getPossibleTargets(MethodReference ref) {
    return getHierarchy(ref).getPossibleTargets(ref);
  }

  public IMethod resolveMethod(MethodReference m) {
    return getHierarchy(m).resolveMethod(m);
  }

  public IField resolveField(FieldReference f) {
    return getHierarchy(f).resolveField(f);
  }

  public IField resolveField(IClass klass, FieldReference f) {
    return getHierarchy(klass).resolveField(klass, f);
  }

  public IMethod resolveMethod(IClass receiver, Selector selector) {
    return getHierarchy(receiver).resolveMethod(receiver, selector);
  }

  public IClass lookupClass(TypeReference A) {
    return getHierarchy(A).lookupClass(A);
  }

  public boolean isSyntheticClass(IClass c) {
    return getHierarchy(c).isSyntheticClass(c);
  }

  public boolean isInterface(TypeReference type) {
    return getHierarchy(type).isInterface(type);
  }

  public IClass getLeastCommonSuperclass(IClass A, IClass B) {
    return getHierarchy(A).getLeastCommonSuperclass(A, B);
  }

  public TypeReference 
    getLeastCommonSuperclass(TypeReference A, TypeReference B) 
  {
    return getHierarchy(A).getLeastCommonSuperclass(A, B);
  }

  public boolean isSubclassOf(IClass c, IClass T) {
    return getHierarchy(c).isSubclassOf(c, T);
  }

  public boolean implementsInterface(IClass c, TypeReference T) {
    return getHierarchy(c).implementsInterface(c, T);
  }

  public Collection<IClass> computeSubClasses(TypeReference type) {
    return getHierarchy(type).computeSubClasses(type);
  }

  public Collection<TypeReference> getJavaLangErrorTypes() {
    return getHierarchy(TypeReference.JavaLangError).getJavaLangErrorTypes();
  }

  public Set<IClass> getImplementors(TypeReference type) {
    return getHierarchy(type).getImplementors(type);
  }

  public int getNumberOfImmediateSubclasses(IClass klass) {
    return getHierarchy(klass).getNumberOfImmediateSubclasses(klass);
  }

  public Collection<IClass> getImmediateSubclasses(IClass klass) {
    return getHierarchy(klass).getImmediateSubclasses(klass);
  }

  public boolean isAssignableFrom(IClass c1, IClass c2) {
    return getHierarchy(c1).isAssignableFrom(c1,c2);
  }

  public Iterator<IClass> iterator() {
    return new ComposedIterator(analysisScope.getLoaders().iterator()) {
      public Iterator makeInner(Object o) {  
	IClassLoader ldr = getLoader( (ClassLoaderReference) o );
	return ldr.iterateAllClasses();
      }
    };
  }

  private CrossLanguageClassHierarchy(
	   AnalysisScope scope,
	   ClassLoaderFactory factory,
	   Map hierarchies)
  {
    this.analysisScope = scope;
    this.loaderFactory = factory;
    this.hierarchies = hierarchies;
  }

  public static CrossLanguageClassHierarchy 
      make(AnalysisScope scope,
	   ClassLoaderFactory factory,
	   WarningSet warnings,
	   Map languageMap)
      throws ClassHierarchyException
  {
    Set languages = new HashSet();
    for(Iterator ldrs = scope.getLoaders().iterator(); 
	ldrs.hasNext(); )
    {
      languages.add(((ClassLoaderReference)ldrs.next()).getLanguage());
    }

    Map hierarchies = new HashMap();
    for(Iterator ls = languages.iterator(); ls.hasNext(); ) {
      Atom l = (Atom) ls.next();
      Language L = (Language) languageMap.get(l);
      assert L != null : l.toString();
      hierarchies.put(l, ClassHierarchy.make(scope, factory, warnings, L));
    }

    return new CrossLanguageClassHierarchy(scope, factory, hierarchies);
  }
    
}
