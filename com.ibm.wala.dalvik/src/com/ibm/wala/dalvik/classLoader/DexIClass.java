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

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedField;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.TypeListItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.ImmutableByteArray;

public class DexIClass extends BytecodeClass<IClassLoader> {
	private static final Logger logger = LoggerFactory.getLogger(DexIClass.class);

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

//    public IClassLoader loader;

    public DexIClass(IClassLoader loader, IClassHierarchy cha,
            final DexModuleEntry dexEntry) {
        super(loader, cha);
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
        String descriptor = classDef.getSuperclass().getTypeDescriptor();
        if (descriptor.endsWith(";"))
            descriptor = descriptor.substring(0,descriptor.length()-1); //remove last ';'
        superName = ImmutableByteArray.make(descriptor);

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
    public boolean isPublic() {
        return (modifiers & PUBLIC.getValue()) != 0;
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#isPrivate()
     */
    public boolean isPrivate() {
        return (modifiers & PRIVATE.getValue()) != 0;
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#isInterface()
     */
    public boolean isInterface() {
        return (modifiers & INTERFACE.getValue()) != 0;

    }

    /*
     * @see com.ibm.wala.classLoader.IClass#isAbstract()
     */
    public boolean isAbstract() {
        return (modifiers & ABSTRACT.getValue()) != 0;
    }


    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#getModifiers()
     */
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


    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.BytecodeClass#computeDeclaredMethods()
     */
    @Override
    protected IMethod[] computeDeclaredMethods() throws InvalidClassFileException {
    	boolean isActivity = false;
    	ArrayList<IMethod> methodsAL = new ArrayList<IMethod>();
    	
    	logger.debug("class: " + classDef.getClassType().getTypeDescriptor());
    	logger.debug("superclass: " + classDef.getSuperclass().getTypeDescriptor());

    	if (classDef.getSuperclass().getTypeDescriptor() == "Landroid/app/Activity;")
    		isActivity = true;
    	
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
                logger.debug("direct method info: " + dMethod.method.getMethodString());
                logger.debug("direct method name: " + dMethod.method.getMethodName().getStringValue());
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
                    logger.debug("Clinit id: " + i);
                }
            }

            // Create virtual methods (other methods)
            for (int i = 0; i < virtualMethods.size(); i++) {
                logger.debug("virtual method info: " + virtualMethods.get(i).method.getMethodString());
                logger.debug("virtual method name: " + virtualMethods.get(i).method.getMethodName().getStringValue());
                logger.debug("virtual method prototype name: " + virtualMethods.get(i).method.getPrototype().getPrototypeString());
                logger.debug("virtual method return type: " + virtualMethods.get(i).method.getPrototype().getReturnType().getTypeDescriptor());
                //methods[dSize+i] = new DexIMethod(virtualMethods[i],this);
                methodsAL.add(new DexIMethod(virtualMethods.get(i),this));
                //is this enough to determine if the class is an activity?
                //maybe check superclass?  -- but that may also not be enough
                //may need to keep checking superclass of superclass, etc.
                if (virtualMethods.get(i).method.getMethodName().getStringValue().equals("onCreate") 
                		&& virtualMethods.get(i).method.getPrototype().getPrototypeString().equals("(Landroid/os/Bundle;)V"))
                	isActivity = true;
            }
        }
        
        if (methods == null && methodsAL.size() > 0 && isActivity) {
            logger.debug("Activity Found, adding ActivityModelMethod to class: " + this.getName().toString());
        	methodsAL.add(new ActivityModelMethod(ActivityModelMethod.getActivityModel(),this));
        }
        
        if (methods == null)
        	methods = methodsAL.toArray(new IMethod[methodsAL.size()]);
        
        return methods;
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
     */
    public IMethod getClassInitializer() {
        if (methods == null){
            try {
                computeDeclaredMethods();
            } catch (InvalidClassFileException e) {
            }
        }
//      return construcorId!=-1?methods[construcorId]:null;
        return clinitId!=-1?methods[clinitId]:null;
    }

	@Override
	public Collection<Annotation> getAnnotations() {
		throw new UnsupportedOperationException();
	}
}
