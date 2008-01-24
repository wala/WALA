package com.ibm.wala.cast.loader;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

public abstract class CAstAbstractLoader implements IClassLoader {

  protected final Map<TypeName,IClass> types = HashMapFactory.make();

  protected final IClassHierarchy cha;

  protected final IClassLoader parent;

  public CAstAbstractLoader(IClassHierarchy cha, IClassLoader parent) {
    this.cha = cha;
    this.parent = parent;
  }

  public CAstAbstractLoader(IClassHierarchy cha) {
    this(cha, null);
  }

  public IClass lookupClass(String className, IClassHierarchy cha) {
    Assertions._assert(this.cha == cha);
    return (IClass) types.get(TypeName.string2TypeName(className));
  }

  public IClass lookupClass(TypeName className) {
    return (IClass) types.get(className);
  }

  public Iterator<IClass> iterateAllClasses() {
    return types.values().iterator();
  }

  public int getNumberOfClasses() {
    return types.size();
  }

  public Atom getName() {
    return getReference().getName();
  }

  public int getNumberOfMethods() {
    int i = 0;
    for (Iterator cls = types.values().iterator(); cls.hasNext();) {
      for (Iterator ms = ((IClass) cls.next()).getDeclaredMethods().iterator();
	   ms.hasNext(); )
      {
        i++;
        ms.next();
      }
    }

    return i;
  }

  public String getSourceFileName(IClass klass) {
    return klass.getSourceFileName();
  }

  public IClassLoader getParent() {
    assert parent != null;
    return parent;
  }

  public void removeAll(Collection toRemove) {
    Set<TypeName> keys = HashSetFactory.make();

    for (Iterator<Map.Entry<TypeName,IClass>> EE = types.entrySet().iterator(); EE.hasNext();) {
      Map.Entry<TypeName,IClass> E =  EE.next();
      if (toRemove.contains(E.getValue())) {
        keys.add(E.getKey());
      }
    }

    for (Iterator KK = keys.iterator(); KK.hasNext();) {
      types.remove(KK.next());
    }
  }

}