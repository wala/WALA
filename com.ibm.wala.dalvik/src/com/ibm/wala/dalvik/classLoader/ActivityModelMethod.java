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

import static org.jf.dexlib.ItemType.TYPE_CLASS_DEF_ITEM;

import java.io.File;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Section;
import org.jf.dexlib.Code.Opcode;

import com.ibm.wala.dalvik.dex.instructions.Invoke;
import com.ibm.wala.util.io.FileProvider;

/**
 *  @deprecated building the Android-Livecycle is done in the class AndroidModel now
 */
@Deprecated
public class ActivityModelMethod extends DexIMethod {
	private static EncodedMethod ActivityModelM;

	public ActivityModelMethod(EncodedMethod encodedMethod, DexIClass klass) {
		super(encodedMethod, klass);
	}

	public static void loadActivityModel() {
		if (ActivityModelM == null) {
			DexFile activityModelDF;
			ClassDefItem activityModelCDI = null;
			try {
				FileProvider fp = new FileProvider();
				File apkFile = fp.getFile("models/ActivityModel.apk", ActivityModelMethod.class.getClassLoader());
				activityModelDF = new DexFile(apkFile);
						/* new DexFile(new File(
						ActivityModelMethod.class.getClassLoader()
								.getResource("models/ActivityModel.apk")
								.toURI())); */
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
			Section<ClassDefItem> cldeff = activityModelDF.getSectionForType(TYPE_CLASS_DEF_ITEM);
			for (ClassDefItem cdefitems : cldeff.getItems()) {
				if (cdefitems.getClassType().getTypeDescriptor()
						.equals("Lactivity/model/ActivityModelActivity;")) {
					activityModelCDI = cdefitems;
				}
			}
			assert (activityModelCDI != null);
			// final EncodedMethod[] virtualMethods =
			// ActivityModelCDI.getClassData().getVirtualMethods();
			for (EncodedMethod virtualMethod : activityModelCDI.getClassData()
					.getVirtualMethods()) {
				if (virtualMethod.method.getMethodName().getStringValue()
						.equals("ActivityModel"))
					ActivityModelM = virtualMethod;
			}
			assert (ActivityModelM != null);
		}
	}

	public static EncodedMethod getActivityModel() {
		if (ActivityModelM == null) {
			loadActivityModel();
		}
		return ActivityModelM;
	}

	// @Override
	// public TypeReference[] getDeclaredExceptions() {
	// return null;
	// }
	//
	// @Override
	// public MethodReference getReference() {
	// if (methodReference == null) {
	// // Set method name
	// Atom name = Atom.findOrCreateUnicodeAtom("ActivityModel");
	//
	// // Set the descriptor
	// ImmutableByteArray desc = ImmutableByteArray.make("()V");
	// Descriptor D =
	// Descriptor.findOrCreate(myClass.getClassLoader().getLanguage(), desc);
	// methodReference = MethodReference.findOrCreate(myClass.getReference(),
	// name, D);
	// }
	//
	// return methodReference;
	// }
	//
	// @Override
	// public boolean hasExceptionHandler() {
	// return false;
	// }
	//
	// @Override
	// public boolean isAbstract() {
	// return false;
	// }
	//
	// @Override
	// public boolean isBridge() {
	// return false;
	// }
	//
	// @Override
	// public boolean isClinit() {
	// return false;
	// }
	//
	// @Override
	// public boolean isFinal() {
	// return false;
	// }
	//
	// @Override
	// public boolean isInit() {
	// return false;
	// }
	//
	// @Override
	// public boolean isNative() {
	// return false;
	// }
	//
	// @Override
	// public boolean isPrivate() {
	// return false;
	// }
	//
	// @Override
	// public boolean isProtected() {
	// return false;
	// }
	//
	// @Override
	// public boolean isPublic() {
	// return true;
	// }
	//
	// @Override
	// public boolean isSynchronized() {
	// return false;
	// }
	//
	// @Override
	// public boolean isSynthetic() {
	// return false;
	// }
	//
	// @Override
	// public boolean isStatic() {
	// return false;
	// }
	//
	// @Override
	// public boolean isVolatile() {
	// return false;
	// }
	//
	// @Override
	// public ExceptionHandler[][] getHandlers() throws
	// InvalidClassFileException {
	// this.handlers = new ExceptionHandler[instructions().size()][];
	// return handlers;
	// }
	//
	// @Override
	// public int getMaxLocals() {
	// return 12;
	// }
	//
	// @Override
	// public int getReturnReg() {
	// return 10;
	// }
	//
	// @Override
	// public int getExceptionReg() {
	// return 11;
	// }
	//
	// @Override
	// public int getNumberOfParameterRegisters() {
	// return 1;
	// }

	@Override
	protected void handleINVOKE_VIRTUAL(int instLoc, String cname,
			String mname, String pname, int[] args, Opcode opcode) {
		if (cname.equals("Lactivity/model/ActivityModelActivity")) {
			cname = myClass.getName().toString();
		}
		instructions.add(new Invoke.InvokeVirtual(instLoc, cname, mname, pname,
				args, opcode, this));

	}
}
