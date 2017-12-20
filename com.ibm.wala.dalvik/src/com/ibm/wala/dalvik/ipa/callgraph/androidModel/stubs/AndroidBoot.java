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
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.util.strings.Atom;

/**
 *  Create some Android-Environment.
 *
 *  Used by the AndroidModel to assign some fields in the analyzed Application if the
 *  settings instruct it to do so.
 *
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel
 *  @see    com.ibm.wala.dalvik.util.AndroidEntryPointManager
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-23
 */
public class AndroidBoot {
//    private static Logger logger = LoggerFactory.getLogger(AndroidBoot.class);

    public static enum BootAction {
        /**
         *  Create an instance of android.app.ContextImpl for the system.
         */
        CREATE_SYSTEM_CONTEXT,
        /**
         *  Crate an instance of android.app.ContextImpl for the apk.
         */
        CREATE_APK_CONTEXT,
    }
    //public static Set<BootAction> BOOT_ALL = EnumSet.allOf(BootAction); 

//    private final  MethodReference scope;
    private TypeSafeInstructionFactory instructionFactory;
//    private ParameterAccessor acc;
    private SSAValueManager pm;
    private VolatileMethodSummary body;


//    public AndroidBoot() {
//        this.scope = null;  // Place something here?
//    }

    private SSAValue mainThread = null;
    private SSAValue systemContext = null;
    private SSAValue packageContext = null;

    public void addBootCode(final TypeSafeInstructionFactory instructionFactory, final SSAValueManager pm,
            final VolatileMethodSummary body) {
        this.instructionFactory = instructionFactory;
//        this.acc = acc;
        this.pm = pm;
        this.body = body;

        mainThread = createMainThred();
        systemContext = createSystemContext(mainThread);
        packageContext = createPackageContext(mainThread);
    }

    public SSAValue getSystemContext() {
        if (systemContext == null) {
            throw new IllegalStateException("No value for systemContext - was addBootCode called?");
        }
        return systemContext;
    }

    public SSAValue getPackageContext() {
        if (packageContext == null) {
            throw new IllegalStateException("No value for packageContext - was addBootCode called?");
        }
        return packageContext;
    }


    public SSAValue getMainThread() {
        if (mainThread == null) {
            throw new IllegalStateException("No value for mainThread - was addBootCode called?");
        }
        return mainThread;
    }


    /**
     *  Create the main-thread as activity-thread.
     */
    private SSAValue createMainThred() {
        final SSAValue mainThread = this.pm.getUnmanaged(AndroidTypes.ActivityThread, "mMainThred");
        { // New-Site
            final int pc = this.body.getNextProgramCounter();
            final NewSiteReference nRef = NewSiteReference.make(pc, AndroidTypes.ActivityThread);
            final SSAInstruction newInstr = this.instructionFactory.NewInstruction(pc, mainThread, nRef);
            body.addStatement(newInstr);
        }
        /*{ // clinit
            final int pc = this.body.getNextProgramCounter();
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.ActivityThread, MethodReference.clinitSelector);
            final SSAValue exception = new SSAValue(this.pm.getUnmanaged(), TypeReference.JavaLangException, this.scope, "ctor_exc" ); 
            final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.SPECIAL);
            final List<SSAValue> params = new ArrayList<SSAValue>(1);
            params.add(mainThread);
            final SSAInstruction ctorCall = instructionFactory.InvokeInstruction(pc, params, exception, site);
            body.addStatement(ctorCall);
        }*/
        { // CTor-Call
            final int pc = this.body.getNextProgramCounter();
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.ActivityThread, MethodReference.initSelector);
            final SSAValue exception = this.pm.getUnmanaged(TypeReference.JavaLangException, "ctor_exc" ); 
            final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.SPECIAL);
            final List<SSAValue> params = new ArrayList<>(1);
            params.add(mainThread);
            final SSAInstruction ctorCall = instructionFactory.InvokeInstruction(pc, params, exception, site);
            body.addStatement(ctorCall);
        }
        return mainThread;
    }

    /**
     *  Create an instance of android.app.ContextImpl for the system.
     *
     *  @see    android.app.ContextImpl.createPackageContextAsUser
     */
	private SSAValue createSystemContext(SSAValue mainThread) {
        final SSAValue systemContext = this.pm.getUnmanaged(AndroidTypes.ContextImpl, "systemContextImpl");
        { // Call ContextImpl.getSystemContext()
            final int pc = this.body.getNextProgramCounter();
            final Descriptor desc = Descriptor.findOrCreate(new TypeName[0], AndroidTypes.ContextImplName);
            final Selector mSel = new Selector(Atom.findOrCreateAsciiAtom("getSystemContext"), desc); 
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.ActivityThread, mSel);
            final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
            final SSAValue exception = this.pm.getException();
            final List<SSAValue> params = new ArrayList<>(1);
            params.add(mainThread);
            final SSAInstruction call = instructionFactory.InvokeInstruction(pc, systemContext, params, exception, site);
            body.addStatement(call);
        }
        { // putting mRestricted = false
            final SSAValue falseConst = this.pm.getUnmanaged(TypeReference.Boolean, "falseConst");
            this.body.addConstant(falseConst.getNumber(), new ConstantValue(false));
            falseConst.setAssigned();
            final int pc = this.body.getNextProgramCounter();
            final FieldReference mRestricted = FieldReference.findOrCreate(AndroidTypes.ContextImpl, Atom.findOrCreateAsciiAtom("mRestricted"),
                    TypeReference.Boolean);
            final SSAInstruction putInst = instructionFactory.PutInstruction(pc, systemContext, falseConst, mRestricted); 
            body.addStatement(putInst);
        }
        return systemContext;
    }

    /**
     *  Create an instance of android.app.ContextImpl for the apk.
     *
     *  @see    android.app.ContextImpl.createPackageContextAsUser
     */
	private SSAValue createPackageContext(final SSAValue mainThread) {
        final SSAValue packageContext = this.pm.getUnmanaged(AndroidTypes.ContextImpl, "packageContextImpl");
        { // New-Site
            final int pc = this.body.getNextProgramCounter();
            final NewSiteReference nRef = NewSiteReference.make(pc, AndroidTypes.ContextImpl);
            final SSAInstruction newInstr = this.instructionFactory.NewInstruction(pc, packageContext, nRef);
            body.addStatement(newInstr);
        }
        /*{ // clnint
            final int pc = this.body.getNextProgramCounter();
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.ContextImpl, MethodReference.clinitSelector);
            final SSAValue exception = new SSAValue(this.pm.getUnmanaged(), TypeReference.JavaLangException, this.scope, "ctor_exc" ); 
            final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.SPECIAL);
            final List<SSAValue> params = new ArrayList<SSAValue>(1);
            params.add(packageContext);
            final SSAInstruction ctorCall = instructionFactory.InvokeInstruction(pc, params, exception, site);
            body.addStatement(ctorCall);
        }*/
        { // CTor-Call
            final int pc = this.body.getNextProgramCounter();
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.ContextImpl, MethodReference.initSelector);
            final SSAValue exception = this.pm.getUnmanaged(TypeReference.JavaLangException, "ctor_exc" ); 
            final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.SPECIAL);
            final List<SSAValue> params = new ArrayList<>(1);
            params.add(packageContext);
            final SSAInstruction ctorCall = instructionFactory.InvokeInstruction(pc, params, exception, site);
            body.addStatement(ctorCall);
        }
        { // putting mRestricted = false
            final SSAValue falseConst = this.pm.getUnmanaged(TypeReference.Boolean, "falseConst");
            this.body.addConstant(falseConst.getNumber(), new ConstantValue(false));
            falseConst.setAssigned();
            final int pc = this.body.getNextProgramCounter();
            final FieldReference mRestricted = FieldReference.findOrCreate(AndroidTypes.ContextImpl, Atom.findOrCreateAsciiAtom("mRestricted"),
                    TypeReference.Boolean);
            final SSAInstruction putInst = instructionFactory.PutInstruction(pc, packageContext, falseConst, mRestricted); 
            body.addStatement(putInst);
        }
        final SSAValue packageName;
        { // Generating pacakge name
            packageName = this.pm.getUnmanaged(TypeReference.JavaLangString, "packageName");
            this.body.addConstant(packageName.getNumber(), new ConstantValue("foo"));   // TODO: Fetch name
            packageName.setAssigned();
        }
        final SSAValue uid = this.pm.getUnmanaged(AndroidTypes.UserHandle, "uid");
        { // New UserHandle
            final int pc = this.body.getNextProgramCounter();
            final NewSiteReference nRef = NewSiteReference.make(pc, AndroidTypes.UserHandle);
            final SSAInstruction newInstr = this.instructionFactory.NewInstruction(pc, uid, nRef);
            body.addStatement(newInstr);
        }
        /*{ // UserHandle(1000) // TODO: seems android-subs do not contain this
            final SSAValue nrUid = new SSAValue(this.pm.getUnmanaged(), TypeReference.Int, this.scope, "nrUid");
            this.body.addConstant(nrUid.getNumber(), new ConstantValue(1000));  // First regular linux user
            nrUid.setAssigned();
            final int pc = this.body.getNextProgramCounter();
            final Descriptor descr = Descriptor.findOrCreate(new TypeName[] { TypeReference.IntName }, TypeReference.VoidName);
            final Selector mSel = new Selector(MethodReference.initAtom, descr);
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.UserHandle, mSel);
            final SSAValue exception = new SSAValue(this.pm.getUnmanaged(), TypeReference.JavaLangException, this.scope, "ctor_exc" ); 
            final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.SPECIAL);
            final List<SSAValue> params = new ArrayList<SSAValue>(2);
            params.add(uid);
            params.add(nrUid);
            final SSAInstruction ctorCall = instructionFactory.InvokeInstruction(pc, params, exception, site);
            body.addStatement(ctorCall);
        } // */
        /*{ // packageContext.init(pi = null, null, mMainThread, mResources = null, mBasePackageName, user); TODO: Not in android stubs
            final SSAValue nullApk = new SSAValue(this.pm.getUnmanaged(), AndroidTypes.LoadedApk, this.scope, "nullApk");   // TODO
            this.body.addConstant(nullApk.getNumber(), new ConstantValue(null));
            nullApk.setAssigned();

            final SSAValue nullIBinder = new SSAValue(this.pm.getUnmanaged(), AndroidTypes.IBinder, this.scope, "nullBinder");
            this.body.addConstant(nullIBinder.getNumber(), new ConstantValue(null));
            nullIBinder.setAssigned();

            final SSAValue nullResources = new SSAValue(this.pm.getUnmanaged(), AndroidTypes.Resources, this.scope, "nullResources"); // TODO
            this.body.addConstant(nullResources.getNumber(), new ConstantValue(null));
            nullResources.setAssigned();

            final int pc = this.body.getNextProgramCounter();
            final Descriptor desc = Descriptor.findOrCreate(new TypeName[] {
                    AndroidTypes.LoadedApkName,
                    AndroidTypes.IBinderName,
                    AndroidTypes.ActivityThreadName,
                    AndroidTypes.ResourcesName,
                    TypeName.string2TypeName("Ljava/lang/String"), // Private?! TypeReference.JavaLangStringName,
                    AndroidTypes.UserHandleName}, TypeReference.VoidName);
            final Selector mSel = new Selector(Atom.findOrCreateAsciiAtom("init"), desc); // the name of the function is actually init 
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.ContextImpl, mSel);
            final SSAValue exception = new SSAValue(this.pm.getUnmanaged(), TypeReference.JavaLangException, this.scope, "init_exc" );
            final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
            final List<SSAValue> params = new ArrayList<SSAValue>(7);
            params.add(packageContext);
            params.add(nullApk);        // TODO: This would contain a Context too?
            params.add(nullIBinder);     // OK: is null in Android-Sources too
            params.add(mainThread);
            params.add(nullResources); // TODO
            params.add(packageName);
            params.add(uid);
            final SSAInstruction call = instructionFactory.InvokeInstruction(pc, params, exception, site);
            body.addStatement(call);
        } // */
        { // XXX: ALTERNATIVE FROM THE ABOVE, THAT IS IN STUBS!!
        //  init(Landroid/app/LoadedApk;Landroid/os/IBinder;Landroid/app/ActivityThread;)V
            final SSAValue nullApk = this.pm.getUnmanaged(AndroidTypes.LoadedApk, "nullApk");   // TODO
            this.body.addConstant(nullApk.getNumber(), new ConstantValue(null));
            nullApk.setAssigned();

            final SSAValue nullIBinder = this.pm.getUnmanaged(AndroidTypes.IBinder, "nullBinder");
            this.body.addConstant(nullIBinder.getNumber(), new ConstantValue(null));
            nullIBinder.setAssigned();

            final int pc = this.body.getNextProgramCounter();
            final Descriptor desc = Descriptor.findOrCreate(new TypeName[] {
                    AndroidTypes.LoadedApkName,
                    AndroidTypes.IBinderName,
                    AndroidTypes.ActivityThreadName,
                    }, TypeReference.VoidName);
            final Selector mSel = new Selector(Atom.findOrCreateAsciiAtom("init"), desc); // the name of the function is actually init 
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.ContextImpl, mSel);
            final SSAValue exception = this.pm.getException();
            final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
            final List<SSAValue> params = new ArrayList<>(7);
            params.add(packageContext);
            params.add(nullApk);        // TODO: This would contain a Context too?
            params.add(nullIBinder);     // OK: is null in Android-Sources too
            params.add(mainThread);
            final SSAInstruction call = instructionFactory.InvokeInstruction(pc, params, exception, site);
            body.addStatement(call);
        }
        return packageContext;
    }

//    private SSAValue createApplicationContext(final SSAValue mainThread, final SSAValue systemContext) {
//        return null; // TODO
//    }
}


