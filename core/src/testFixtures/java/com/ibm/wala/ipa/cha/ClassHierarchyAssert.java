package com.ibm.wala.ipa.cha;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

/** Custom assertions for {@link IClassHierarchy} instances, for use in WALA's own tests. */
public class ClassHierarchyAssert
    extends AbstractObjectAssert<ClassHierarchyAssert, IClassHierarchy> {

  public ClassHierarchyAssert(IClassHierarchy actual) {
    super(actual, ClassHierarchyAssert.class);
  }

  @CheckReturnValue
  public static ClassHierarchyAssert assertThat(IClassHierarchy actual) {
    return new ClassHierarchyAssert(actual);
  }

  public AbstractCollectionAssert<?, Collection<? extends IClass>, IClass, ObjectAssert<IClass>>
      immediateSubclassesOf(IClass klass) {
    isNotNull();
    return Assertions.assertThat(actual.getImmediateSubclasses(klass))
        .as("immediate subclasses of <%s>", klass);
  }

  public ObjectAssert<IClassLoader> loader(ClassLoaderReference loaderRef) {
    isNotNull();
    return Assertions.assertThat(actual.getLoader(loaderRef)).as("class loader");
  }

  public ObjectAssert<IClass> lookupClass(TypeReference typeReference) {
    isNotNull();
    return Assertions.assertThat(actual.lookupClass(typeReference))
        .as("class for <%s>", typeReference);
  }

  public AbstractIntegerAssert<?> numberOfClasses() {
    isNotNull();
    return Assertions.assertThat(actual.getNumberOfClasses())
        .as("number of classes in the hierarchy");
  }
}
