package com.ibm.wala.cast.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warning;

public abstract class CAstAbstractLoader implements IClassLoader {

  protected final Map<TypeName,IClass> types = HashMapFactory.make();

  protected final IClassHierarchy cha;

  protected final IClassLoader parent;

  private final Map<ModuleEntry, Set<Warning>> errors = new HashMap<ModuleEntry, Set<Warning>>();
  
  public CAstAbstractLoader(IClassHierarchy cha, IClassLoader parent) {
    this.cha = cha;
    this.parent = parent;
  }

  public CAstAbstractLoader(IClassHierarchy cha) {
    this(cha, null);
  }

  public void addMessage(ModuleEntry module, Warning message) {
    if (! errors.containsKey(module)) {
      errors.put(module, new HashSet<Warning>());
    }
    
    errors.get(module).add(message);
  }
  
  private Iterator<ModuleEntry> getMessages(final byte severity) {
    return new MapIterator<Map.Entry<ModuleEntry,Set<Warning>>, ModuleEntry>(new FilterIterator<Map.Entry<ModuleEntry,Set<Warning>>>(errors.entrySet().iterator(), new Filter<Map.Entry<ModuleEntry,Set<Warning>>>()  {
      public boolean accepts(Entry<ModuleEntry, Set<Warning>> o) {
         for(Warning w : o.getValue()) {
           if (w.getLevel() == severity) {
             return true;
           }
         }
         return false;
      }
    }), new Function<Map.Entry<ModuleEntry,Set<Warning>>, ModuleEntry>() {
      public ModuleEntry apply(Entry<ModuleEntry, Set<Warning>> object) {
        return object.getKey();
      }      
    });
  }
  
  public Iterator<ModuleEntry> getModulesWithParseErrors() {
     return getMessages(Warning.SEVERE);
  }

  public Iterator<ModuleEntry> getModulesWithWarnings() {
    return getMessages(Warning.MILD);
  }

  public Set<Warning> getMessages(ModuleEntry m) {
    return errors.get(m);
  }
  
  public IClass lookupClass(String className, IClassHierarchy cha) {
    assert this.cha == cha;
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

  public String getSourceFileName(IMethod method, int bcOffset) {
    if (!(method instanceof AstMethod)){
      return null;
    }
    Position pos = ((AstMethod)method).getSourcePosition(bcOffset);
    if (null == pos){
      return null;
    } 
    return pos.getURL().getFile();
  }
  
  public String getSourceFileName(IClass klass) {
    return ((AstClass)klass).getSourcePosition().getURL().getFile();
  }
  
  public InputStream getSource(IClass klass) {
    try {
      return ((AstClass)klass).getSourcePosition().getInputStream();
    } catch (IOException e) {
      return null;
    }
  }

  public InputStream getSource(IMethod method, int bcOffset) {
    try {
      return ((AstMethod)method).getSourcePosition(bcOffset).getInputStream();
    } catch (IOException e) {
      return null;
    }
  }

  public IClassLoader getParent() {
    assert parent != null;
    return parent;
  }

  public void removeAll(Collection<IClass> toRemove) {
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