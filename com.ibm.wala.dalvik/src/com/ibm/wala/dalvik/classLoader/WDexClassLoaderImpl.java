/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Jonathan Bardin     <astrosus@gmail.com>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.classLoader;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.ClassLoaderImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 * ClassLoader for Java & Dalvik.
 *
 */
public class WDexClassLoaderImpl extends ClassLoaderImpl {
    private IClassLoader lParent;

    private final SetOfClasses exclusions;
    
    //Commented out until IBM fixes ClassLoaderFactoryImpl "protected IClassLoader makeNewClassLoader"
    
//    public WDexClassLoaderImpl(ClassLoaderReference loader,
//            ArrayClassLoader arrayClassLoader, IClassLoader parent,
//            SetOfClasses exclusions, IClassHierarchy cha) {
//        super(loader, arrayClassLoader, parent, exclusions, cha);
//        lParent = parent;
//        lExclusions = exclusions;
//        //DEBUG_LEVEL = 0;
//    }
    
    public WDexClassLoaderImpl(ClassLoaderReference loader,IClassLoader parent,
            SetOfClasses exclusions, IClassHierarchy cha) {
        super(loader, cha.getScope().getArrayClassLoader(), parent, exclusions, cha);
        lParent = parent;
        this.exclusions = exclusions;
        //DEBUG_LEVEL = 0;
    }
    
    @Override
    public void init(List<Module> modules) throws IOException {
    	super.init(modules);
        // module are loaded according to the given order (same as in Java VM)
        Set<ModuleEntry> classModuleEntries = HashSetFactory.make();
        
        for (Module archive : modules) {
            Set<ModuleEntry> classFiles = getDexFiles(archive);
            
            removeClassFiles(classFiles, classModuleEntries);
            loadAllDexClasses(classFiles);
            
            for (ModuleEntry file : classFiles) {
            	classModuleEntries.add(file);
            }
        }                       
    }
    

    /**
     * Remove from s any class file module entries which already are in t
     */
    private static void removeClassFiles(Set<ModuleEntry> s, Set<ModuleEntry> t) {
    	Set<String> old = HashSetFactory.make();
    	for (ModuleEntry m : t) {
    		old.add(m.getClassName());
    	}
    	HashSet<ModuleEntry> toRemove = HashSetFactory.make();
    	for (ModuleEntry m : s) {
    		if (old.contains(m.getClassName())) {
    			toRemove.add(m);
    		}
    	}
    	s.removeAll(toRemove);
    }
    
    private static Set<ModuleEntry> getDexFiles(Module M) {
    	HashSet<ModuleEntry> result = HashSetFactory.make();
    	for (ModuleEntry entry : Iterator2Iterable.make(M.getEntries())) {
    		if (entry instanceof DexModuleEntry) {    		
    			result.add(entry);
    		} 
    	}
    	return result;
    }
    
    
    @SuppressWarnings("unused")
	private void loadAllDexClasses(Collection<ModuleEntry> moduleEntries) {
    	
    	for (ModuleEntry entry : moduleEntries) {
    		// Dalvik class
    		if (entry instanceof DexModuleEntry) {
    			DexModuleEntry dexEntry = ((DexModuleEntry) entry);
    			String className = dexEntry.getClassName();
				TypeName tName = TypeName.string2TypeName(className);

    			//if (DEBUG_LEVEL > 0) {
    			//  System.err.println("Consider dex class: " + tName);
    			//}

    			//System.out.println("Typename: " + tName.toString());
    			//System.out.println(tName.getClassName());
    			if (loadedClasses.get(tName) != null) {
    				Warnings.add(MultipleDexImplementationsWarning
    						.create(className));
    			} else if (lParent != null && lParent.lookupClass(tName) != null) {
    				Warnings.add(MultipleDexImplementationsWarning
    						.create(className));
    			}
    			//if the class is empty, ie an interface
    			//                  else if (dexEntry.getClassDefItem().getClassData() == null) {
    			//                      System.out.println("Jumping over (classdata null): "+dexEntry.getClassName());
    			//                      Warnings.add(MultipleDexImplementationsWarning
    			//                              .create(dexEntry.getClassName()));
    			//                  }
    			else {
    				IClass iClass = new DexIClass(this, cha, dexEntry);
    				if (iClass.getReference().getName().equals(tName)) {
    					
    					// className is a descriptor, so strip the 'L'
    					if (exclusions != null && exclusions.contains(className.substring(1))) {
    						if (DEBUG_LEVEL > 0) {
    							System.err.println("Excluding " + className);
    						}
    						continue;
    					}
 
    					loadedClasses.put(tName, iClass);
    				} else {
    					Warnings.add(InvalidDexFile.create(className));
    				}
    			}
    		}
    	}
    }


    /**
     * @return the IClassHierarchy of this classLoader.
     */
    public IClassHierarchy getClassHierarcy() {
        return cha;
    }

  /**
   * A warning when we find more than one implementation of a given class name
   */
  private static class MultipleDexImplementationsWarning extends Warning {

    final String className;

    MultipleDexImplementationsWarning(String className) {
      super(Warning.SEVERE);
      this.className = className;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + className;
    }

    public static MultipleDexImplementationsWarning create(String className) {
      return new MultipleDexImplementationsWarning(className);
    }
  }

  /**
   * A warning when we encounter InvalidClassFileException
   */
  private static class InvalidDexFile extends Warning {

    final String className;

    InvalidDexFile(String className) {
      super(Warning.SEVERE);
      this.className = className;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + className;
    }
    public static InvalidDexFile create(String className) {
      return new InvalidDexFile(className);
    }
  }
}
