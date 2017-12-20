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
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.dalvik.ipa.callgraph.androidModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.FieldImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.SummarizedMethodWithNames;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.util.strings.Atom;

/**
 *  Encapsulates synthetic methods for modeling Androids lifecycle.
 *  
 *  In the generated code this class may be found as "Lcom/ibm/wala/AndroidModelClass"
 *
 *  @see    com.ibm.wala.ipa.callgraph.impl.FakeRootClass
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  TODO: Move this class into an other loader? Currently: Primordial
 */
public final /* singleton */ class AndroidModelClass extends SyntheticClass {
    private static Logger logger = LoggerFactory.getLogger(AndroidModelClass.class);

    public static final TypeReference ANDROID_MODEL_CLASS = TypeReference.findOrCreate(
            ClassLoaderReference.Primordial, TypeName.string2TypeName("Lcom/ibm/wala/AndroidModelClass"));
    private IClassHierarchy cha;

    public static AndroidModelClass getInstance(IClassHierarchy cha) {
        IClass android = cha.lookupClass(ANDROID_MODEL_CLASS);
        AndroidModelClass mClass;
        if (android == null) {
        	mClass = new AndroidModelClass(cha);
        } else if (!(android instanceof AndroidModelClass)) {
        	throw new IllegalArgumentException(String.format("android model class does not have expected type %s, but %s!", AndroidModelClass.class, android.getClass().toString()));
        } else {
        	mClass = (AndroidModelClass) android;
        }
        return mClass;
    }

    private AndroidModelClass(IClassHierarchy cha) {
        super(ANDROID_MODEL_CLASS, cha);
        this.addMethod(this.clinit());
        this.cha = cha;
        this.cha.addClass(this);
    }

    /**
     *  Generate clinit for AndroidModelClass.
     *
     *  clinit initializes AndroidComponents
     */
    private SummarizedMethod clinit() {
        final MethodReference clinitRef = MethodReference.findOrCreate(this.getReference(), MethodReference.clinitSelector);
        final VolatileMethodSummary clinit = new VolatileMethodSummary(new MethodSummary(clinitRef));
        clinit.setStatic(true);
        final TypeSafeInstructionFactory instructionFactory = new TypeSafeInstructionFactory(cha);
        
        final Set<TypeReference> components = AndroidEntryPointManager.getComponents();
        int ssaNo = 1;

        if (AndroidEntryPointManager.MANAGER.doFlatComponents()) {
            for (TypeReference component : components) {
                final SSAValue instance = new SSAValue(ssaNo++, component, clinitRef);
                { // New
                    final int pc = clinit.getNextProgramCounter();
                    final NewSiteReference nRef = NewSiteReference.make(pc, component);
                    final SSAInstruction instr = instructionFactory.NewInstruction(pc, instance, nRef);
                    clinit.addStatement(instr);
                }
                { // Call cTor
                    final int pc = clinit.getNextProgramCounter();
                    final MethodReference ctor = MethodReference.findOrCreate(component, MethodReference.initSelector);
                    final CallSiteReference site = CallSiteReference.make(pc, ctor, IInvokeInstruction.Dispatch.SPECIAL);
                    final SSAValue exception = new SSAValue(ssaNo++, TypeReference.JavaLangException, clinitRef);
                    final List<SSAValue> params = new ArrayList<>();
                    params.add(instance);
                    final SSAInstruction ctorCall = instructionFactory.InvokeInstruction(pc, params, exception, site);
                    clinit.addStatement(ctorCall);
                }
                { // Put into AndroidModelClass
                    final Atom fdName = component.getName().getClassName();
                    putField(fdName, component);
                    final int pc = clinit.getNextProgramCounter();
                    final FieldReference fdRef = FieldReference.findOrCreate(this.getReference(), fdName, component);
                    final SSAInstruction putInst = instructionFactory.PutInstruction(pc, instance, fdRef);
                    clinit.addStatement(putInst);
                }
            }
        }

        return new SummarizedMethodWithNames(clinitRef, clinit, this);
    }


    //
    //  Contents of the class: Methods
    //
    private IMethod macroModel = null;
//    private IMethod allActivitiesModel = null;
    private Map<Selector, IMethod> methods = HashMapFactory.make(); // does not contain macroModel

    public boolean containsMethod(Selector selector) {
        return (
                ((macroModel != null) && macroModel.getSelector().equals(selector)) ||
                methods.containsKey(selector));
    }

    @Override
    public IMethod getMethod(Selector selector) {
        //assert (macroModel != null) : "Macro Model was not set yet!";
        
        if ((macroModel != null) && (macroModel.getSelector().equals(selector))) {
            return macroModel;
        }
    
        if (methods.containsKey(selector)) {
            return methods.get(selector);
        }
        if (selector.equals(MethodReference.initSelector)) {
            logger.warn("AndroidModelClass is not intended to be initialized");
            return null;
        }
        throw new IllegalArgumentException("Could not resolve " + selector);
    }

    @Override
    public Collection<IMethod> getDeclaredMethods() {
        Set<IMethod> methods = HashSetFactory.make();
        if ( this.macroModel != null ) {
            methods.add(macroModel);
        }

        methods.addAll(this.methods.values());

        return Collections.unmodifiableCollection(methods);
    }
    
    @Override
    public Collection<IMethod> getAllMethods()  {
        return getDeclaredMethods();
    }

    /* package private */ void setMacroModel(IMethod model) {
        assert(this.macroModel == null);
        this.macroModel = model;
    }

    public void addMethod(IMethod method) {
        if (this.methods.containsKey(method.getSelector())) {
            // TODO: Check this matches on signature not on contents!
            // TODO: What on different Context versions
            throw new IllegalStateException("The AndroidModelClass already contains a Method called" + method.getName());
        }
        assert(this.methods != null);
        this.methods.put(method.getSelector(), method);
    }

    @Override
    public IMethod getClassInitializer()  {
        return getMethod(MethodReference.clinitSelector);
    }


    //
    //  Contents of the class: Fields
    //  We have none...
    //
    private Map<Atom, IField> fields = new HashMap<>();

    @Override
    public IField getField(Atom name) {
        if (fields.containsKey(name)) {
            return fields.get(name);
        } else {
            return null;
        }
    }

    public void putField(Atom name, TypeReference type) {
        final FieldReference fdRef = FieldReference.findOrCreate(this.getReference(), name, type);
        final int accessFlags = ClassConstants.ACC_STATIC | ClassConstants.ACC_PUBLIC;
        final IField field = new FieldImpl(this, fdRef, accessFlags, null, null); 

        this.fields.put(name, field);
    }

    /**
     *  This class does not contain any fields.
     */
    @Override
    public Collection<IField> getAllFields()  {
        return fields.values();
    }

    /**
     *  This class does not contain any fields.
     */
    @Override
    public Collection<IField> getDeclaredStaticFields() {
        return fields.values();
    }

    /**
     *  This class does not contain any fields.
     */
    @Override
    public Collection<IField> getAllStaticFields() {
        return fields.values();
    }

     /**
     *  This class does not contain any fields.
     */
    @Override
    public Collection<IField> getDeclaredInstanceFields() throws UnsupportedOperationException {
        return Collections.emptySet();
    }

    /**
     *  This class does not contain any fields.
     */
    @Override
    public Collection<IField> getAllInstanceFields()  {
        return Collections.emptySet();
    }



    //
    //  Class Modifiers
    //

    /**
     *  This is a public final class.
     */
    @Override
    public int getModifiers() {
        return  ClassConstants.ACC_PUBLIC |
                ClassConstants.ACC_FINAL;
    }
    @Override
    public boolean isPublic() {         return true;  }
    @Override
    public boolean isPrivate() {        return false; }
    @Override
    public boolean isInterface() {      return false; }
    @Override
    public boolean isAbstract() {       return false; }
    @Override
    public boolean isArrayClass () {    return false; }

    /**
     *  This is a subclass of the root class.
     */
    @Override
    public IClass getSuperclass() throws UnsupportedOperationException {
        return getClassHierarchy().getRootClass();
    }

    /**
     *  This class does not impement any interfaces.
     */
    @Override 
    public Collection<IClass> getAllImplementedInterfaces() {
        return Collections.emptySet();
    }

    @Override
    public Collection<IClass> getDirectInterfaces() {
        return Collections.emptySet();
    }

    //
    //  Misc
    //

    @Override
    public boolean isReferenceType() {
        return getReference().isReferenceType();
    }

}

