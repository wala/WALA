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

import static org.jf.dexlib.Util.AccessFlags.ABSTRACT;
import static org.jf.dexlib.Util.AccessFlags.INTERFACE;
import static org.jf.dexlib.Util.AccessFlags.PRIVATE;
import static org.jf.dexlib.Util.AccessFlags.PUBLIC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jf.dexlib.AnnotationDirectoryItem;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.AnnotationVisibility;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedField;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.TypeListItem;

import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.ImmutableByteArray;

public class DexIClass extends BytecodeClass<IClassLoader> {

/**
     * Item which contains the class definitions.
     * (compute by DexFile, from the dexLib)
     */
    private final ClassDefItem classDef;

    /**
     * Bitfields of these flags are used to indicate the accessibility and overall properties of classes and class members.
     * i.e. public/private/abstract/interface.
     */
    private final int modifiers;

    private IMethod[] methods = null;

    //private int construcorId = -1;

    private int clinitId = -1;

    private final DexModuleEntry dexModuleEntry;
//    public IClassLoader loader;

    public DexIClass(IClassLoader loader, IClassHierarchy cha,
            final DexModuleEntry dexEntry) {
        super(loader, cha);
        this.dexModuleEntry = dexEntry;
        classDef = dexEntry.getClassDefItem();
//        this.loader = loader;

        // Set modifiers
        modifiers = classDef.getAccessFlags();

        //computerTypeReference()
        // Set typeReference
        typeReference = TypeReference.findOrCreate(loader.getReference(),
                dexEntry.getClassName());

        //set hashcode
        hashCode = 2161*getReference().hashCode();

        //computeSuperName()
        // Set Super Name;
        String descriptor = classDef.getSuperclass() != null? classDef.getSuperclass().getTypeDescriptor(): null;
        if (descriptor != null && descriptor.endsWith(";"))
            descriptor = descriptor.substring(0,descriptor.length()-1); //remove last ';'
        superName = descriptor != null? ImmutableByteArray.make(descriptor): null;

        //computeInterfaceNames()
        // Set interfaceNames
        final TypeListItem intfList = classDef.getInterfaces();
        int size = intfList == null ? 0 : intfList.getTypeCount();
        //if (size != 0)
        //  System.out.println(intfList.getTypes().get(0).getTypeDescriptor());


        interfaceNames = new ImmutableByteArray[size];
        for (int i = 0; i < size; i++) {
            TypeIdItem itf = intfList.getTypeIdItem(i);
            descriptor = itf.getTypeDescriptor();
            if (descriptor.endsWith(";"))
                descriptor = descriptor.substring(0, descriptor.length()-1);
            interfaceNames[i] = ImmutableByteArray
                    .make(descriptor);
        }

        //Load class data
        final ClassDataItem classData = classDef.getClassData();

        // Set direct instance fields
//      if (classData == null) {
//            throw new RuntimeException("DexIClass::DexIClass(): classData is null");
//      }
//      final EncodedField[] encInstFields = classData.getInstanceFields();
//      size = encInstFields==null?0:encInstFields.length;
//      instanceFields = new IField[size];
//      for (int i = 0; i < size; i++) {
//          //name of instance field.
//          //System.out.println(encInstFields[i].field.getFieldName().getStringValue());
//          //name of field type.
//          //System.out.println(encInstFields[i].field.getFieldType().getTypeDescriptor());
//          instanceFields[i] = new DexIField(encInstFields[i],this);
//      }
//
//      // Set direct static fields
//      final EncodedField[] encStatFields = classData.getStaticFields();
//      size = encStatFields==null?0:encStatFields.length;
//      staticFields = new IField[size];
//      for (int i = 0; i < size; i++) {
//          //name of static field
//          //System.out.println(encInstFields[i].field.getFieldName().getStringValue());
//          staticFields[i] = new DexIField(encStatFields[i],this);
//      }

        //computeFields()
        if (classData != null) {
            //final EncodedField[] encInstFields = classData.getInstanceFields();

        	final List<EncodedField> encInstFields = classData.getInstanceFields();
            instanceFields = new IField[encInstFields.size()];
        	for (int i = 0; i < encInstFields.size(); i++) {
        		instanceFields[i] = new DexIField(encInstFields.get(i),this);
        	}
        	
            // Set direct static fields
            final List<EncodedField> encStatFields = classData.getStaticFields();
            staticFields = new IField[encStatFields.size()];
            for (int i = 0; i < encStatFields.size(); i++) {
                staticFields[i] = new DexIField(encStatFields.get(i),this);
            }
        }
    }

    /**
     * @return The classDef Item associated with this class.
     */
    public ClassDefItem getClassDefItem(){
        return classDef;
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#isPublic()
     */
    @Override
    public boolean isPublic() {
        return (modifiers & PUBLIC.getValue()) != 0;
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#isPrivate()
     */
    @Override
    public boolean isPrivate() {
        return (modifiers & PRIVATE.getValue()) != 0;
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#isInterface()
     */
    @Override
    public boolean isInterface() {
        return (modifiers & INTERFACE.getValue()) != 0;

    }

    /*
     * @see com.ibm.wala.classLoader.IClass#isAbstract()
     */
    @Override
    public boolean isAbstract() {
        return (modifiers & ABSTRACT.getValue()) != 0;
    }


    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#getModifiers()
     */
    @Override
    public int getModifiers() throws UnsupportedOperationException {
        return modifiers;
    }


      /**
       * @see java.lang.Object#equals(Object)
       */
      @Override
      public boolean equals(Object obj) {
        // it's ok to use instanceof since this class is final
        // if (this.getClass().equals(obj.getClass())) {
        if (obj instanceof DexIClass) {
          return getReference().equals(((DexIClass) obj).getReference());
        } else {
          return false;
        }
      }
      
      @Override
      public int hashCode() {
        return hashCode;
      }

      Collection<Annotation> getAnnotations(Set<AnnotationVisibility> types) {
    	  Set<Annotation> result = HashSetFactory.make();
    	  AnnotationDirectoryItem d = dexModuleEntry.getClassDefItem().getAnnotations();
    	  if (d.getClassAnnotations() != null) {
    		  for(AnnotationItem a : d.getClassAnnotations().getAnnotations()) {
    			  if (types == null || types.contains(a.getVisibility())) {
    				  result.add(DexUtil.getAnnotation(a, getClassLoader().getReference()));
    			  }
    		  }
    	  }
    	  return result;
      }
      
      @Override
      public Collection<Annotation> getAnnotations() {
    	  return getAnnotations((Set<AnnotationVisibility>)null);
      }

      @Override
      public Collection<Annotation> getAnnotations(boolean runtimeInvisible) {
  		return getAnnotations(getTypes(runtimeInvisible));
      }

	static Set<AnnotationVisibility> getTypes(boolean runtimeInvisible) {
		Set<AnnotationVisibility> types = HashSetFactory.make();
  		types.add(AnnotationVisibility.SYSTEM);
  		if (runtimeInvisible) {
  			types.add(AnnotationVisibility.BUILD);
  		} else {
  			types.add(AnnotationVisibility.RUNTIME);
  		}
		return types;
	}

      List<AnnotationItem> getAnnotations(MethodIdItem m, Set<AnnotationVisibility> types) {
    	  List<AnnotationItem> result = new ArrayList<>();
    	  AnnotationDirectoryItem d = dexModuleEntry.getClassDefItem().getAnnotations();
    	  if (d != null && d.getMethodAnnotations(m) !=  null) {
    		  for(AnnotationItem a : d.getMethodAnnotations(m).getAnnotations()) {
        		  if (types == null || types.contains(a.getVisibility())) {
        			  result.add(a);
        		  }
    		  }
    	  }
    	  return result;
      }

      List<AnnotationItem> getAnnotations(FieldIdItem m) {
    	  List<AnnotationItem> result = new ArrayList<>();
    	  AnnotationDirectoryItem d = dexModuleEntry.getClassDefItem().getAnnotations();
    	  if (d != null) {
    		  for(AnnotationItem a : d.getFieldAnnotations(m).getAnnotations()) {
    			  result.add(a);
    		  }
    	  }
    	  return result;
      }

      Map<Integer,List<AnnotationItem>> getParameterAnnotations(MethodIdItem m) {
    	  Map<Integer,List<AnnotationItem>> result = HashMapFactory.make();
    	  AnnotationDirectoryItem d = dexModuleEntry.getClassDefItem().getAnnotations();
    	  if (d != null) {
    		  int i = 0;
    		  for(AnnotationSetItem as : d.getParameterAnnotations(m).getAnnotationSets()) {
    			  for(AnnotationItem a : as.getAnnotations()) {
    				  if (! result.containsKey(i)) {
    					  result.put(i, new ArrayList<AnnotationItem>());
    				  }
    				  result.get(i).add(a);
    			  }
    			  i++;
    		  }
    	  }
    	  return result;
      }

    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.BytecodeClass#computeDeclaredMethods()
     */
    @Override
    protected IMethod[] computeDeclaredMethods() {
    	ArrayList<IMethod> methodsAL = new ArrayList<>();
    	    	
        if (methods == null && classDef.getClassData() == null)
            methods = new IMethod[0];

        if (methods == null && classDef.getClassData() != null){
//            final EncodedMethod[] directMethods = classDef.getClassData().getDirectMethods();
//            final EncodedMethod[] virtualMethods = classDef.getClassData().getVirtualMethods();
            final List<EncodedMethod> directMethods = classDef.getClassData().getDirectMethods();
            final List<EncodedMethod> virtualMethods = classDef.getClassData().getVirtualMethods();

            //methods = new IMethod[dSize+vSize];

            // Create Direct methods (static, private, constructor)
            for (int i = 0; i < directMethods.size(); i++) {
                EncodedMethod dMethod = directMethods.get(i);
                //methods[i] = new DexIMethod(dMethod,this);
                methodsAL.add(new DexIMethod(dMethod,this));

                //Set construcorId
                //if ( (dMethod.accessFlags & CONSTRUCTOR.getValue()) != 0){
                //    construcorId = i;
                //}
                //Set clinitId             
                //if (methods[i].isClinit())
                if (methodsAL.get(i).isClinit()) {
                    clinitId = i;
                }
            }

            // Create virtual methods (other methods)
            for (int i = 0; i < virtualMethods.size(); i++) {
                //methods[dSize+i] = new DexIMethod(virtualMethods[i],this);
                methodsAL.add(new DexIMethod(virtualMethods.get(i),this));
                //is this enough to determine if the class is an activity?
                //maybe check superclass?  -- but that may also not be enough
                //may need to keep checking superclass of superclass, etc.
                
            }
        }
        
        if (methods == null)
        	methods = methodsAL.toArray(new IMethod[methodsAL.size()]);
        
        return methods;
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
     */
    @Override
    public IMethod getClassInitializer() {
        if (methods == null){
            computeDeclaredMethods();
        }
//      return construcorId!=-1?methods[construcorId]:null;
        return clinitId!=-1?methods[clinitId]:null;
    }

	@Override
	public Module getContainer() {
		return dexModuleEntry.asModule();
	}
}
