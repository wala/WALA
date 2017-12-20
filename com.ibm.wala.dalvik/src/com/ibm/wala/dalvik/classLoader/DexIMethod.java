/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 * dexlib2 update: Julian Dolby (dolby@us.ibm.com)
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

import static org.jf.dexlib2.AccessFlags.ABSTRACT;
import static org.jf.dexlib2.AccessFlags.BRIDGE;
import static org.jf.dexlib2.AccessFlags.DECLARED_SYNCHRONIZED;
import static org.jf.dexlib2.AccessFlags.FINAL;
import static org.jf.dexlib2.AccessFlags.NATIVE;
import static org.jf.dexlib2.AccessFlags.PRIVATE;
import static org.jf.dexlib2.AccessFlags.PROTECTED;
import static org.jf.dexlib2.AccessFlags.PUBLIC;
import static org.jf.dexlib2.AccessFlags.STATIC;
import static org.jf.dexlib2.AccessFlags.VOLATILE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.analysis.ClassPathResolver;
import org.jf.dexlib2.analysis.MethodAnalyzer;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.SwitchPayload;
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction;
import org.jf.dexlib2.iface.instruction.formats.ArrayPayload;
import org.jf.dexlib2.iface.instruction.formats.Instruction10t;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction12x;
import org.jf.dexlib2.iface.instruction.formats.Instruction20t;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction21ih;
import org.jf.dexlib2.iface.instruction.formats.Instruction21lh;
import org.jf.dexlib2.iface.instruction.formats.Instruction21s;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22b;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22s;
import org.jf.dexlib2.iface.instruction.formats.Instruction22t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22x;
import org.jf.dexlib2.iface.instruction.formats.Instruction23x;
import org.jf.dexlib2.iface.instruction.formats.Instruction30t;
import org.jf.dexlib2.iface.instruction.formats.Instruction31c;
import org.jf.dexlib2.iface.instruction.formats.Instruction31i;
import org.jf.dexlib2.iface.instruction.formats.Instruction31t;
import org.jf.dexlib2.iface.instruction.formats.Instruction32x;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rc;
import org.jf.dexlib2.iface.instruction.formats.Instruction51l;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.value.ArrayEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.TypeEncodedValue;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.dalvik.dex.instructions.ArrayFill;
import com.ibm.wala.dalvik.dex.instructions.ArrayGet;
import com.ibm.wala.dalvik.dex.instructions.ArrayGet.Type;
import com.ibm.wala.dalvik.dex.instructions.ArrayLength;
import com.ibm.wala.dalvik.dex.instructions.ArrayPut;
import com.ibm.wala.dalvik.dex.instructions.BinaryLiteralOperation;
import com.ibm.wala.dalvik.dex.instructions.BinaryOperation;
import com.ibm.wala.dalvik.dex.instructions.Branch;
import com.ibm.wala.dalvik.dex.instructions.CheckCast;
import com.ibm.wala.dalvik.dex.instructions.Constant;
import com.ibm.wala.dalvik.dex.instructions.GetField;
import com.ibm.wala.dalvik.dex.instructions.Goto;
import com.ibm.wala.dalvik.dex.instructions.InstanceOf;
import com.ibm.wala.dalvik.dex.instructions.Instruction;
import com.ibm.wala.dalvik.dex.instructions.Invoke;
import com.ibm.wala.dalvik.dex.instructions.Monitor;
import com.ibm.wala.dalvik.dex.instructions.New;
import com.ibm.wala.dalvik.dex.instructions.NewArray;
import com.ibm.wala.dalvik.dex.instructions.NewArrayFilled;
import com.ibm.wala.dalvik.dex.instructions.PackedSwitchPad;
import com.ibm.wala.dalvik.dex.instructions.PutField;
import com.ibm.wala.dalvik.dex.instructions.Return;
import com.ibm.wala.dalvik.dex.instructions.SparseSwitchPad;
import com.ibm.wala.dalvik.dex.instructions.Switch;
import com.ibm.wala.dalvik.dex.instructions.Throw;
import com.ibm.wala.dalvik.dex.instructions.UnaryOperation;
import com.ibm.wala.dalvik.dex.instructions.UnaryOperation.OpID;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IndirectionData;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;


/**
 * A wrapper around a EncodedMethod object (from dexlib) that represents a method.
 */
public class DexIMethod implements IBytecodeMethod<Instruction> {

	/**
	 * The EncodedMethod object for which this DexIMethod is a wrapper.
	 */
	private final Method eMethod;

	/**
	 * The declaring class for this method.
	 */
	protected final DexIClass myClass;

	/**
	 * canonical MethodReference corresponding to this method,
	 * construct in the getReference method.
	 */
	private MethodReference methodReference;

	/**
	 * name of the return type for this method,
	 * construct in the get return type method.
	 */
	private TypeReference typeReference;

	private ExceptionHandler[][] handlers;
	
	protected InstructionArray instructions;



	private static int totalInsts = 0;

	public DexIMethod(Method encodedMethod, DexIClass klass) {
		eMethod = encodedMethod;
		myClass = klass;
	}

	public static int getTotalInsts() {
		return totalInsts;
	}

	//------------------------------------------
	// Specific methods
	//------------------------------------------

	/**
	 * @return the EncodedMethod object for which this DexIMethod is a wrapper.
	 */
	public Method toEncodedMethod() {
		return eMethod;
	}


	//-------------------------------------------
	// IMethod methods
	//-------------------------------------------

	@Override
	public TypeReference[] getDeclaredExceptions()
			throws UnsupportedOperationException {
/** BEGIN Custom change: Variable Names in synth. methods */
        if (myClass.getClassDefItem().getAnnotations() == null) {
            return null;
        }
        ArrayList<String> strings = new ArrayList<>();
        Set<? extends org.jf.dexlib2.iface.Annotation> annotationSet = eMethod.getAnnotations();
/** END Custom change: Variable Names in synth. methods */

        if (annotationSet != null) {
			for (org.jf.dexlib2.iface.Annotation annotationItem: annotationSet)
			{
				if (annotationItem.getType().contentEquals("Ldalvik/annotation/Throws;")) {
					for (AnnotationElement e : annotationItem.getElements()) {
						for (EncodedValue v : ((ArrayEncodedValue)e.getValue()).getValue()) {
							String tname = ((TypeEncodedValue)v).getValue();
							if (tname.endsWith(";"))
								tname = tname.substring(0,tname.length()-1);
							strings.add(tname);
						}
					}
				}
			}
		}

		if (strings.size() == 0)
			return null;

		ClassLoaderReference loader = getDeclaringClass().getClassLoader().getReference();

		TypeReference[] result = new TypeReference[strings.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = TypeReference.findOrCreate(loader, TypeName.findOrCreate(ImmutableByteArray.make(strings.get(i))));
		}
		return result;
	}


	@Override
	public String getLocalVariableName(int bcIndex, int localNumber) {
		throw new UnsupportedOperationException("getLocalVariableName not implemented");
	}

	/**
	 * XXX not fully about the + 2.
	 * @return the RegisterCount + 2 to make some room for the return and exception register
	 * @see com.ibm.wala.classLoader.ShrikeCTMethod#getMaxLocals()
	 */
	public int getMaxLocals() {
		return eMethod.getImplementation().getRegisterCount() + 2;
	}

	public int getReturnReg() {
		return eMethod.getImplementation().getRegisterCount();
	}

	public int getExceptionReg() {
		return eMethod.getImplementation().getRegisterCount()+1;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#getMaxStackHeight()
	 */
	public int getMaxStackHeight() {
		throw new UnsupportedOperationException("Dex Methods does not use a stack");
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#getDescriptor()
	 */
	@Override
	public Descriptor getDescriptor() {
		return getReference().getDescriptor();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#getNumberOfParameters()
	 */
	@Override
	public int getNumberOfParameters() {
		final int number;


		if (isStatic() || isClinit()) {
			number = getReference().getNumberOfParameters();
		} else {
			number = getReference().getNumberOfParameters() + 1;
		}

		return number;
	}

	public int getNumberOfParameterRegisters() {
		int number = isStatic() || isClinit() ? 0 : 1;

		for(int i = 0; i < getReference().getNumberOfParameters(); i++) {
			TypeReference ref = getReference().getParameterType(i);
			number += ref.equals(TypeReference.Double) || ref.equals(TypeReference.Long) ? 2 : 1;
		}
		
		return number;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#getParameterType(int)
	 */
	@Override
	public TypeReference getParameterType(int index) {
		if (!isStatic()) {
			if (index == 0) {
				return myClass.getReference();
			} else {
				return getReference().getParameterType(index - 1);
			}
		} else {
			return getReference().getParameterType(index);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#getReference()
	 */
	@Override
	public MethodReference getReference() {
		//Compute the method reference from the MethodIdItem
		if (methodReference == null) {
			// Set method name
			Atom name = Atom.findOrCreateUnicodeAtom(eMethod.getName());

			//            // Set the descriptor
			//            Descriptor descriptor = Descriptor.findOrCreateUTF8(eMethod.method.getPrototype().getPrototypeString());
			//            methodReference = MethodReference.findOrCreate(myClass.getReference(),name, descriptor);

			Descriptor D = Descriptor.findOrCreate(myClass.getClassLoader().getLanguage(), ImmutableByteArray.make(DexUtil.getSignature(eMethod)));
			methodReference = MethodReference.findOrCreate(myClass.getReference(), name, D);
		}

		return methodReference;
	}


	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#getReturnType()
	 */
	@Override
	public TypeReference getReturnType() {
		//compute the typeReference from the MethodIdItem
		if (typeReference == null) {
			typeReference = getReference().getReturnType();
		}

		return typeReference;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#getSelector()
	 */
	@Override
	public Selector getSelector() {
		return getReference().getSelector();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.classLoader.IMethod#getSignature()
	 */
	@Override
	public String getSignature() {
		return getReference().getSignature();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#hasExceptionHandler()
	 */
	@Override
	public boolean hasExceptionHandler() {
		List<? extends TryBlock<? extends org.jf.dexlib2.iface.ExceptionHandler>> tries = eMethod.getImplementation().getTryBlocks();
		return tries==null?false:tries.size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#hasLocalVariableTable()
	 */
	@Override
	public boolean hasLocalVariableTable() {
		throw new UnsupportedOperationException("DexIMethod: hasLocalVariableTable() not yet implemented");
		//TODO Compute the local variable name from the DebugInfo Item
		//eMethod.codeItem.getDebugInfo()
		//      return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isAbstract()
	 */
	@Override
	public boolean isAbstract() {
		return (eMethod.getAccessFlags() & ABSTRACT.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isClinit()
	 */
	@Override
	public boolean isClinit() {
		return eMethod.getName().equals(MethodReference.clinitName.toString());
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isFinal()
	 */
	@Override
	public boolean isFinal() {
		return (eMethod.getAccessFlags() & FINAL.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isInit()
	 */
	@Override
	public boolean isInit() {
		return eMethod.getName().equals(MethodReference.initAtom.toString());
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isNative()
	 */
	@Override
	public boolean isNative() {
		return (eMethod.getAccessFlags() & NATIVE.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isBridge()
	 */
	@Override
	public boolean isBridge() {
		return (eMethod.getAccessFlags() & BRIDGE.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isPrivate()
	 */
	@Override
	public boolean isPrivate() {
		return (eMethod.getAccessFlags() & PRIVATE.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isProtected()
	 */
	@Override
	public boolean isProtected() {
		return (eMethod.getAccessFlags() & PROTECTED.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.classLoader.IMethod#isPublic()
	 */
	@Override
	public boolean isPublic() {
		return (eMethod.getAccessFlags() & PUBLIC.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isSynchronized()
	 */
	@Override
	public boolean isSynchronized() {
		return (eMethod.getAccessFlags() & DECLARED_SYNCHRONIZED.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMethod#isSynthetic()
	 */
	@Override
	public boolean isSynthetic() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMember#isStatic()
	 */
	@Override
	public boolean isStatic() {
		return (eMethod.getAccessFlags() & STATIC.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMember#isVolatile()
	 */
	public boolean isVolatile() {
		return (eMethod.getAccessFlags() & VOLATILE.getValue()) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.classLoader.IMember#getDeclaringClass()
	 */
	@Override
	public IClass getDeclaringClass() {
		return myClass;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.ipa.cha.IClassHierarchyDweller#getClassHierarchy()
	 */
	@Override
	public IClassHierarchy getClassHierarchy() {
		return myClass.getClassHierarchy();
	}

	@Override
	public Atom getName() {
		return getReference().getName();
	}

	@Override
	public int getLineNumber(int bcIndex) {
		return getInstructionIndex(bcIndex);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getReference().toString();
	}

	/**
	 * @see java.lang.Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// instanceof is OK because this class is final.
		// if (this.getClass().equals(obj.getClass())) {
		if (obj instanceof DexIMethod) {
			DexIMethod that = (DexIMethod) obj;
			return (getDeclaringClass().equals(that.getDeclaringClass()) && getReference().equals(that.getReference()));
		} else {
			return false;
		}
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 9661 * getReference().hashCode();
	}



	//-------------------------------------------
	// IByteCodeMethod methods
	// (required to build the ShrikeCFG)
	//-------------------------------------------


	@Override
	public int getBytecodeIndex(int i) {
		// TODO Auto-generated method stub
		//      System.out.println("DexIMethod: getBytecodeIndex() possibly not implemented correctly");
		//      Integer.valueOf(eMethod.codeItem.getInstructions()[i].opcode.value);
		//      return i;
		return getAddressFromIndex(i);
		//      return -1;
	}


	@Override
	public ExceptionHandler[][] getHandlers() {

		if (handlers != null)
			return handlers;

		//ExceptionHandler[][] handlers = new ExceptionHandler[eMethod.codeItem.getInstructions().length][];

		List<? extends TryBlock<? extends org.jf.dexlib2.iface.ExceptionHandler>> tryBlocks = eMethod.getImplementation().getTryBlocks();

		//
		//      if (tries == null){
		//          return new ExceptionHandler[eMethod.codeItem.getInstructions().length][];
		//      }
		//      ExceptionHandler[][] handlers = new ExceptionHandler[tries.length][];
		//      for (int i = 0; i < tries.length; i++) {
		//          EncodedTypeAddrPair[] etaps = tries[i].encodedCatchHandler.handlers;
		//          handlers[i] = new ExceptionHandler[etaps.length];
		//          for (int j = 0; j < etaps.length; j++) {
		//              EncodedTypeAddrPair etap = etaps[j];
		//              handlers[i][j] = new ExceptionHandler(etap.getHandlerAddress(), etap.exceptionType.getTypeDescriptor());
		//          }
		//      }

		this.handlers = new ExceptionHandler[instructions().size()][];
		if (tryBlocks == null){
			//          return new ExceptionHandler[instructions.size()][];
			return handlers;
		}

		ArrayList<ArrayList<ExceptionHandler>> temp_array = new ArrayList<>();
		for (int i = 0; i < instructions().size(); i++) {
			temp_array.add(new ArrayList<ExceptionHandler>());
		}

		for (TryBlock<? extends org.jf.dexlib2.iface.ExceptionHandler> tryItem: tryBlocks) {
			int startAddress = tryItem.getStartCodeAddress();
			int endAddress = tryItem.getStartCodeAddress() + tryItem.getCodeUnitCount();
			/**
			 * The end address points to the address immediately after the end of the last
			 * instruction that the try block covers. We want the .catch directive and end_try
			 * label to be associated with the last covered instruction, so we need to get
			 * the address for that instruction
			 */


			int startInst = getInstructionIndex(startAddress);
			int endInst;
			/**
			 * The try block can extend to the last instruction in the method.  If this is the
			 * case then endAddress will be the address immediately following the last instruction.
			 * Check to make sure this is the case.
			 */
			if (endAddress > getAddressFromIndex(instructions().size()-1)) {
				endInst = instructions().size()-1;
				int endSize = 0;
				for(org.jf.dexlib2.iface.instruction.Instruction inst : eMethod.getImplementation().getInstructions()) {
					endSize = inst.getCodeUnits();
				}
				if (endAddress != (getAddressFromIndex(endInst) + endSize))
					throw new RuntimeException("Invalid code offset " + endAddress + " for the try block end address");
			}
			else {
				endInst = getInstructionIndex(endAddress) - 1;
			}

			for (int i = startInst; i <= endInst; i++) {
				//add the rest of the handlers
				for (org.jf.dexlib2.iface.ExceptionHandler etaps: tryItem.getExceptionHandlers()) {
					temp_array.get(i).add(new ExceptionHandler( getInstructionIndex(etaps.getHandlerCodeAddress()), etaps.getExceptionType() ));
				}
			}
		}


		for (int i = 0; i < instructions().size(); i++) {
			handlers[i] = temp_array.get(i).toArray(new ExceptionHandler[temp_array.get(i).size()]);
		
			/*
			System.out.println("i: " + i);
			for (int j = 0; j < handlers[i].length; j++) {
				System.out.println("\t j: " + j);
				System.out.println("\t\t Handler: " +  handlers[i][j].getHandler());
				System.out.println("\t\t Catch Class: " + handlers[i][j].getCatchClass());
			}
			*/
		}

		return handlers;
	}



	@Override
	public Instruction[] getInstructions() {
		if (instructions == null)
			parseBytecode();

		return instructions.toArray(new Instruction[ instructions.size() ]);
	}

	private boolean odexMethod() {
		for(org.jf.dexlib2.iface.instruction.Instruction inst : eMethod.getImplementation().getInstructions()) {
			if (inst.getOpcode().odexOnly()) {
				return true;
			}
		}
		
		return false;
	}

	Iterable<? extends org.jf.dexlib2.iface.instruction.Instruction> deodex() {
		try {
			DexFileModule m = myClass.getContainer();

			ClassPathResolver path = 
					new ClassPathResolver(Collections.singletonList(m.getFile().getParent() + "/"),
							Collections.<String>emptyList(),
							m.getDexFile());

			ClassPath cp = new ClassPath(path.getResolvedClassProviders(), false, m.getDexFile().getOpcodes().artVersion);

			MethodAnalyzer analyzer = new MethodAnalyzer(cp, eMethod, null, false);

			return analyzer.getInstructions();
		} catch (Exception e) {
			assert false : e;
		    return eMethod.getImplementation().getInstructions();
		}
	}
	
	protected void parseBytecode() {

		Iterable<? extends org.jf.dexlib2.iface.instruction.Instruction> instrucs = 
			odexMethod()?
			    deodex():
				eMethod.getImplementation().getInstructions();

		//      for (org.jfmethod.getInstructionIndex(.dexlib.Code.Instruction inst: instrucs)
		//      {
		//          switch (inst.getFormat())
		//          {
		//
		//          case Format10t:
		//              instructions.add(new IInstruction10t((Instruction10t)inst, this));
		//              break;
		//          case Format10x:
		//              instructions.add(new IInstruction10x((Instruction10x)inst, this));
		//              break;
		//          case Format11n:
		//              instructions.add(new IInstruction11n((Instruction11n)inst, this));
		//              break;
		//          case Format11x:
		//              instructions.add(new IInstruction11x((Instruction11x)inst, this));
		//              break;
		//          case Format12x:
		//              instructions.add(new IInstruction12x((Instruction12x)inst, this));
		//              break;
		//          case Format20t:
		//              instructions.add(new IInstruction20t((Instruction20t)inst, this));
		//              break;
		//          case Format21c:
		//              instructions.add(new IInstruction21c((Instruction21c)inst, this));
		//              break;
		//          case Format21h:
		//              instructions.add(new IInstruction21h((Instruction21h)inst, this));
		//              break;
		//          case Format21s:
		//              instructions.add(new IInstruction21s((Instruction21s)inst, this));
		//              break;
		//          case Format21t:
		//              instructions.add(new IInstruction21t((Instruction21t)inst, this));
		//              break;
		//          case Format22b:
		//              instructions.add(new IInstruction22b((Instruction22b)inst, this));
		//              break;
		//          case Format22c:
		//              instructions.add(new IInstruction22c((Instruction22c)inst, this));
		//              break;
		//          case Format22cs:
		//              instructions.add(new IInstruction22cs((Instruction22cs)inst, this));
		//              break;
		//          case Format22s:
		//              instructions.add(new IInstruction22s((Instruction22s)inst, this));
		//              break;
		//          case Format22t:
		//              instructions.add(new IInstruction22t((Instruction22t)inst, this));
		//              break;
		//          case Format22x:
		//              instructions.add(new IInstruction22x((Instruction22x)inst, this));
		//              break;
		//          case Format23x:
		//              instructions.add(new IInstruction23x((Instruction23x)inst, this));
		//              break;
		//          case Format30t:
		//              instructions.add(new IInstruction30t((Instruction30t)inst, this));
		//              break;
		//          case Format31c:
		//              instructions.add(new IInstruction31c((Instruction31c)inst, this));
		//              break;
		//          case Format31i:
		//              instructions.add(new IInstruction31i((Instruction31i)inst, this));
		//              break;
		//          case Format31t:
		//              instructions.add(new IInstruction31t((Instruction31t)inst, this));
		//              break;
		//          case Format32x:
		//              instructions.add(new IInstruction32x((Instruction32x)inst, this));
		//              break;
		//          case Format35c:
		//              instructions.add(new IInstruction35c((Instruction35c)inst, this));
		//              break;
		//          case Format35ms:
		//              instructions.add(new IInstruction35ms((Instruction35ms)inst, this));
		//              break;
		//          case Format35s:
		//              instructions.add(new IInstruction35s((Instruction35s)inst, this));
		//              break;
		//          case Format3rc:
		//              instructions.add(new IInstruction3rc((Instruction3rc)inst, this));
		//              break;
		//          case Format3rms:
		//              instructions.add(new IInstruction3rms((Instruction3rms)inst, this));
		//              break;
		//          case Format51l:
		//              instructions.add(new IInstruction51l((Instruction51l)inst, this));
		//              break;
		//          case ArrayData:
		//              instructions.add(new IArrayDataPseudoInstruction((ArrayDataPseudoInstruction)inst, this));
		//              break;
		//          case PackedSwitchData:
		//              instructions.add(new IPackedSwitchDataPseudoInstruction((PackedSwitchDataPseudoInstruction)inst, this));
		//              break;
		//          case SparseSwitchData:
		//              instructions.add(new ISparseSwitchDataPseudoInstruction((SparseSwitchDataPseudoInstruction)inst, this));
		//              break;
		//          case UnresolvedOdexInstruction:
		//              instructions.add(new IUnresolvedOdexInstruction((UnresolvedOdexInstruction)inst, this));
		//              break;
		//
		//          }
		//      }

		//if (eMethod.method.getMethodString().contentEquals("Lorg/xbill/DNS/Name;-><init>([B)V") && instrucs.length == 4)
		//  System.out.println("debug here");



		instructions = new InstructionArray();
		int instLoc = 0;
		int instCounter = -1;
		//int pc = 0;
		int currentCodeAddress = 0;
		for (org.jf.dexlib2.iface.instruction.Instruction inst: instrucs)
		{
			totalInsts++;
			instCounter++;
			//          instLoc = pc - instCounter;
			instLoc = currentCodeAddress;
			//pc += inst.getFormat().size;
			switch(inst.getOpcode())
			{
			case NOP:
			case ARRAY_PAYLOAD:
			case PACKED_SWITCH_PAYLOAD:
			case SPARSE_SWITCH_PAYLOAD:
				switch (inst.getOpcode().format)
				{
				case ArrayPayload:
				{
					for (int i = 0; i < instructions.size(); i++)
					{
						if (instructions.getFromId(i) instanceof ArrayFill)
							if (instLoc == (((ArrayFill)getInstructionFromIndex(i)).tableAddressOffset + getAddressFromIndex(i)))
							{
								((ArrayFill)getInstructionFromIndex(i)).setArrayDataTable((ArrayPayload)inst);



								//                              Iterator<ArrayElement> b = (((ArrayDataPseudoInstruction)inst).getElements());
								//                              while (b.hasNext())
									//                              {
								//                                  int ElementWidth = ((ArrayDataPseudoInstruction)inst).getElementWidth();
								//                                  byte[] temp_byte = new byte[ElementWidth];
								//
								//                                  ArrayElement t = (ArrayElement)b.next();
								//                                  for (int j = 0; j < ElementWidth; j++)
								//                                      temp_byte[j] = t.buffer[t.bufferIndex+(ElementWidth-1)-j];
								//
								//                                  ByteBuffer byte_buffer = ByteBuffer.wrap(temp_byte);
								//
								//                                  System.out.println("Index: " + t.bufferIndex + ", Width: " + t.elementWidth + ", Value: " +  byte_buffer.getChar());
								//                              }


								break;
							}
					}
					break;
				}
				case PackedSwitchPayload:
					for (int i = 0; i < instructions.size(); i++)
					{
						if (instructions.getFromId(i) instanceof Switch)
							if (instLoc == (((Switch)getInstructionFromIndex(i)).tableAddressOffset + getAddressFromIndex(i)))
							{
								((Switch)getInstructionFromIndex(i))
								   .setSwitchPad(new PackedSwitchPad(
										   (SwitchPayload)inst,
										   getAddressFromIndex(i+1) - getAddressFromIndex(i)));
								break;
							}
					}
					break;
				case SparseSwitchPayload:
				{
					for (int i = 0; i < instructions.size(); i++)
					{
						if (instructions.getFromId(i) instanceof Switch)
							if (instLoc == (((Switch)getInstructionFromIndex(i)).tableAddressOffset + getAddressFromIndex(i)))
							{
								((Switch)getInstructionFromIndex(i)).setSwitchPad(
										new SparseSwitchPad(
											(SwitchPayload)inst,
											getAddressFromIndex(i+1) - getAddressFromIndex(i)));
								break;
							}
					}
					//                  System.out.println("Targets: " + ((SparseSwitchDataPseudoInstruction)inst).getTargetCount());
					//                  Iterator<SparseSwitchTarget> i = (((SparseSwitchDataPseudoInstruction)inst).iterateKeysAndTargets());
					//                  while (i.hasNext())
						//                  {
						//                      SparseSwitchTarget t = (SparseSwitchTarget)i.next();
						//                      System.out.println("Key: " + t.key + ", TargetAddressOffset: " + t.targetAddressOffset);
					//                  }
					break;
				}
				default:
					class NOPInstruction extends Instruction {
						private NOPInstruction(int pc, Opcode op, DexIMethod method) {
							super(pc, op, method);
						}

						@Override
						public void visit(Visitor visitor) {
							// no op
						} 
					}
					
					instructions.add(new NOPInstruction(currentCodeAddress, Opcode.NOP, this));
					break;
				}
				break;
			case MOVE:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_FROM16:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE, ((Instruction22x)inst).getRegisterA(),
						((Instruction22x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_16:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE, ((Instruction32x)inst).getRegisterA(),
						((Instruction32x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_WIDE:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE_WIDE,
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_WIDE_FROM16:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE_WIDE,
						((Instruction22x)inst).getRegisterA(), ((Instruction22x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_WIDE_16:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE_WIDE,
						((Instruction32x)inst).getRegisterA(), ((Instruction32x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_OBJECT:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_OBJECT_FROM16:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE, ((Instruction22x)inst).getRegisterA(),
						((Instruction22x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_OBJECT_16:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE, ((Instruction32x)inst).getRegisterA(),
						((Instruction32x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MOVE_RESULT:
				//register b set as return register, -1;
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE, ((Instruction11x)inst).getRegisterA(), getReturnReg(), inst.getOpcode(), this));
				break;
			case MOVE_RESULT_WIDE:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE_WIDE,
						((Instruction11x)inst).getRegisterA(), getReturnReg(), inst.getOpcode(), this));
				break;
			case MOVE_RESULT_OBJECT:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE, ((Instruction11x)inst).getRegisterA(),
						getReturnReg(), inst.getOpcode(), this));
				break;
			case MOVE_EXCEPTION:
				instructions.add(new UnaryOperation(instLoc,
						UnaryOperation.OpID.MOVE_EXCEPTION, ((Instruction11x)inst).getRegisterA(),
						getExceptionReg(), inst.getOpcode(), this));
				break;
			case RETURN_VOID:
			case RETURN_VOID_NO_BARRIER:
				instructions.add(new Return.ReturnVoid(instLoc, inst.getOpcode(), this));
				break;
			case RETURN:
				//I think only primitives call return, and objects call return-object
				instructions.add(new Return.ReturnSingle(instLoc,
						((Instruction11x)inst).getRegisterA(), true, inst.getOpcode(), this));
				break;
			case RETURN_WIDE:
				//+1 to second parameter okay?
				instructions.add(new Return.ReturnDouble(instLoc,
						((Instruction11x)inst).getRegisterA(), ((Instruction11x)inst).getRegisterA()+1, inst.getOpcode(), this));
				break;
			case RETURN_OBJECT:
				instructions.add(new Return.ReturnSingle(instLoc,
						((Instruction11x)inst).getRegisterA(), false, inst.getOpcode(), this));
				break;
			case CONST_4: {
				instructions.add(new Constant.IntConstant(instLoc,
						((Instruction11n)inst).getNarrowLiteral(),((Instruction11n)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			}
			case CONST_16:
				instructions.add(new Constant.IntConstant(instLoc,
						((Instruction21s)inst).getNarrowLiteral(), ((Instruction21s)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST:
				instructions.add(new Constant.IntConstant(instLoc,
						((Instruction31i)inst).getNarrowLiteral(), ((Instruction31i)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST_HIGH16:
				instructions.add(new Constant.IntConstant(instLoc,
						((Instruction21ih)inst).getHatLiteral() << 16, ((Instruction21ih)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST_WIDE_16:
				instructions.add(new Constant.LongConstant(instLoc,
						((Instruction21s)inst).getWideLiteral(), ((Instruction21s)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST_WIDE_32:
				instructions.add(new Constant.LongConstant(instLoc,
						((Instruction31i)inst).getWideLiteral(), ((Instruction31i)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST_WIDE:
				instructions.add(new Constant.LongConstant(instLoc,
						((Instruction51l)inst).getWideLiteral(), ((Instruction51l)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST_WIDE_HIGH16:
				instructions.add(new Constant.LongConstant(instLoc,
						((Instruction21lh)inst).getWideLiteral() << 16, ((Instruction21lh)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST_STRING:

				instructions.add(new Constant.StringConstant(instLoc,
						((StringReference)((Instruction21c)inst).getReference()).getString(),
						((Instruction21c)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST_STRING_JUMBO:
				instructions.add(new Constant.StringConstant(instLoc,
						((StringReference)((Instruction31c)inst).getReference()).getString(),
						((Instruction31c)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CONST_CLASS: {
				String cname = ((org.jf.dexlib2.iface.reference.TypeReference)((Instruction21c)inst).getReference()).getType();
				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);
				
				//IClass ic = this.myClass.loader.lookupClass(TypeName.findOrCreate(cname));
				TypeReference typeRef = TypeReference.findOrCreate(myClass.getClassLoader().getReference(), cname);
				
				instructions.add(new Constant.ClassConstant(instLoc,
						typeRef, ((Instruction21c)inst).getRegisterA(), inst.getOpcode(), this));
				//logger.debug("myClass found name: " + this.myClass.loader.lookupClass(TypeName.findOrCreate(cname)).toString());
				break;
			}
			case MONITOR_ENTER:
				instructions.add(new Monitor(instLoc, true, ((Instruction11x)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case MONITOR_EXIT:
				instructions.add(new Monitor(instLoc, false, ((Instruction11x)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case CHECK_CAST: {
				String cname = ((org.jf.dexlib2.iface.reference.TypeReference)((Instruction21c)inst).getReference()).getType();
				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);


				//retrieving type reference correctly?
				instructions.add(new CheckCast(instLoc,
						TypeReference.findOrCreate(myClass.getClassLoader().getReference(), cname),
						((Instruction21c)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			}
			case INSTANCE_OF: {
				String cname = ((org.jf.dexlib2.iface.reference.TypeReference)((Instruction22c)inst).getReference()).getType();
				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);
				instructions.add(new InstanceOf(instLoc,
						((Instruction22c)inst).getRegisterA(),
						TypeReference.findOrCreate(myClass.getClassLoader().getReference(),
								cname),
								((Instruction22c)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			}
			case ARRAY_LENGTH:
				instructions.add(new ArrayLength(instLoc,
						((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case NEW_INSTANCE: {
				//newsitereference use instLoc or pc?
				String cname = ((org.jf.dexlib2.iface.reference.TypeReference)((Instruction21c)inst).getReference()).getType();
				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);

				instructions.add(new New(instLoc,
						((Instruction21c)inst).getRegisterA(),
						NewSiteReference.make(instLoc, TypeReference.findOrCreate(myClass.getClassLoader().getReference(),
								cname)), inst.getOpcode(), this));
				break;
			}
			case NEW_ARRAY:
			{
				int[] params = new int[1];
				params[0] = ((Instruction22c)inst).getRegisterB();
				//              MyLogger.log(LogLevel.INFO, "Type: " +((TypeIdItem)((Instruction22c)inst).getReferencedItem()).getTypeDescriptor());

				String cname = ((org.jf.dexlib2.iface.reference.TypeReference)((Instruction22c)inst).getReference()).getType();
				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);

				instructions.add(new NewArray(instLoc,
						((Instruction22c)inst).getRegisterA(),
						NewSiteReference.make(instLoc, TypeReference.findOrCreate(myClass.getClassLoader().getReference(),
								cname)),
								params, inst.getOpcode(), this));
				break;
			}
			//TODO: FILLED ARRAYS
			case FILLED_NEW_ARRAY: {
				int registerCount = ((Instruction35c)inst).getRegisterCount();
				int[] params = new int[1];
				params[0] = registerCount;
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
				{
					switch(i) {
					case 0:
						args[0] = ((Instruction35c)inst).getRegisterD();
						break;
					case 1:
						args[1] = ((Instruction35c)inst).getRegisterE();
						break;
					case 2:
						args[2] = ((Instruction35c)inst).getRegisterF();
						break;
					case 3:
						args[3] = ((Instruction35c)inst).getRegisterG();
						break;
					case 4:
						args[4] = ((Instruction35c)inst).getRegisterC();
						break;
					default:
						throw new RuntimeException("Illegal instruction at "
								+ instLoc + ": bad register count");
					}
				}

				String cname = ((org.jf.dexlib2.iface.reference.TypeReference)((Instruction35c)inst).getReference()).getType();
				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);

				NewSiteReference newSiteRef = NewSiteReference.make(instLoc, TypeReference.findOrCreate(myClass.getClassLoader().getReference(),
						cname));
				TypeReference myTypeRef = TypeReference.findOrCreate(myClass.getClassLoader().getReference(), newSiteRef.getDeclaredType().getArrayElementType().getName().toString());

				instructions.add(new NewArrayFilled(instLoc, getReturnReg(),
						newSiteRef, myTypeRef, params, args, inst.getOpcode(), this));
				break;
			}
			case FILLED_NEW_ARRAY_RANGE: {
				int registerCount = ((Instruction3rc)inst).getRegisterCount();
				int[] params = new int[1];
				params[0] = registerCount;
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
					args[i] = ((Instruction3rc)inst).getStartRegister() + i;

				String cname = ((org.jf.dexlib2.iface.reference.TypeReference)((Instruction3rc)inst).getReference()).getType();
				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);


				NewSiteReference newSiteRef = NewSiteReference.make(instLoc, TypeReference.findOrCreate(myClass.getClassLoader().getReference(),
						cname));
				TypeReference myTypeRef = TypeReference.findOrCreate(myClass.getClassLoader().getReference(), newSiteRef.getDeclaredType().getArrayElementType().getName().toString());


				instructions.add(new NewArrayFilled(instLoc, getReturnReg(), newSiteRef, myTypeRef, params, args, inst.getOpcode(), this));
				break;
			}
			case FILL_ARRAY_DATA:
				// System.out.println("Array Reference: " + ((Instruction31t)inst).getRegisterA());
				// System.out.println("Table Address Offset: " + ((Instruction31t)inst).getCodeOffset());

				TypeReference arrayElementType = findOutArrayElementType(inst, instructions.toArray(new Instruction[0]), instCounter);
				instructions.add(new ArrayFill(instLoc, ((Instruction31t)inst).getRegisterA(), ((Instruction31t)inst).getCodeOffset(),
						TypeReference.findOrCreate(myClass.getClassLoader().getReference(), arrayElementType.getName().toString()), inst.getOpcode(), this));
				break;
			case THROW:
				instructions.add(new Throw(instLoc, ((Instruction11x)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case GOTO:
				instructions.add(new Goto(instLoc, ((Instruction10t)inst).getCodeOffset(), inst.getOpcode(), this));
				break;
			case GOTO_16:
				instructions.add(new Goto(instLoc, ((Instruction20t)inst).getCodeOffset(), inst.getOpcode(), this));
				break;
			case GOTO_32:
				instructions.add(new Goto(instLoc, ((Instruction30t)inst).getCodeOffset(), inst.getOpcode(), this));
				break;

			case PACKED_SWITCH:
			case SPARSE_SWITCH:
				instructions.add(new Switch(instLoc, ((Instruction31t)inst).getRegisterA(), ((Instruction31t)inst).getCodeOffset(), inst.getOpcode(), this));
				break;

			case CMPL_FLOAT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.CMPL_FLOAT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case CMPG_FLOAT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.CMPG_FLOAT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case CMPL_DOUBLE:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.CMPL_DOUBLE, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case CMPG_DOUBLE:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.CMPG_DOUBLE, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case CMP_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.CMPL_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case IF_EQ:
				instructions.add(new Branch.BinaryBranch(instLoc, ((Instruction22t)inst).getCodeOffset(),
						Branch.BinaryBranch.CompareOp.EQ, ((Instruction22t)inst).getRegisterA(),
						((Instruction22t)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case IF_NE:
				instructions.add(new Branch.BinaryBranch(instLoc, ((Instruction22t)inst).getCodeOffset(),
						Branch.BinaryBranch.CompareOp.NE, ((Instruction22t)inst).getRegisterA(),
						((Instruction22t)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case IF_LT:
				instructions.add(new Branch.BinaryBranch(instLoc, ((Instruction22t)inst).getCodeOffset(),
						Branch.BinaryBranch.CompareOp.LT, ((Instruction22t)inst).getRegisterA(),
						((Instruction22t)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case IF_GE:
				instructions.add(new Branch.BinaryBranch(instLoc, ((Instruction22t)inst).getCodeOffset(),
						Branch.BinaryBranch.CompareOp.GE, ((Instruction22t)inst).getRegisterA(),
						((Instruction22t)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case IF_GT:
				instructions.add(new Branch.BinaryBranch(instLoc, ((Instruction22t)inst).getCodeOffset(),
						Branch.BinaryBranch.CompareOp.GT, ((Instruction22t)inst).getRegisterA(),
						((Instruction22t)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case IF_LE:
				instructions.add(new Branch.BinaryBranch(instLoc, ((Instruction22t)inst).getCodeOffset(),
						Branch.BinaryBranch.CompareOp.LE, ((Instruction22t)inst).getRegisterA(),
						((Instruction22t)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case IF_EQZ:
				instructions.add(new Branch.UnaryBranch(instLoc,
						((Instruction21t)inst).getCodeOffset(), Branch.UnaryBranch.CompareOp.EQZ,
						((Instruction21t)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case IF_NEZ:
				instructions.add(new Branch.UnaryBranch(instLoc,
						((Instruction21t)inst).getCodeOffset(), Branch.UnaryBranch.CompareOp.NEZ,
						((Instruction21t)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case IF_LTZ:
				instructions.add(new Branch.UnaryBranch(instLoc,
						((Instruction21t)inst).getCodeOffset(), Branch.UnaryBranch.CompareOp.LTZ,
						((Instruction21t)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case IF_GEZ:
				instructions.add(new Branch.UnaryBranch(instLoc,
						((Instruction21t)inst).getCodeOffset(), Branch.UnaryBranch.CompareOp.GEZ,
						((Instruction21t)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case IF_GTZ:
				instructions.add(new Branch.UnaryBranch(instLoc,
						((Instruction21t)inst).getCodeOffset(), Branch.UnaryBranch.CompareOp.GTZ,
						((Instruction21t)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case IF_LEZ:
				instructions.add(new Branch.UnaryBranch(instLoc,
						((Instruction21t)inst).getCodeOffset(), Branch.UnaryBranch.CompareOp.LEZ,
						((Instruction21t)inst).getRegisterA(), inst.getOpcode(), this));
				break;
			case AGET:
				instructions.add(new ArrayGet(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_int, inst.getOpcode(), this));
				break;
			case AGET_WIDE:
				instructions.add(new ArrayGet(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_wide, inst.getOpcode(), this));
				break;
			case AGET_OBJECT:
				instructions.add(new ArrayGet(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_object, inst.getOpcode(), this));
				break;
			case AGET_BOOLEAN:
				instructions.add(new ArrayGet(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_boolean, inst.getOpcode(), this));
				break;
			case AGET_BYTE:
				instructions.add(new ArrayGet(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_byte, inst.getOpcode(), this));
				break;
			case AGET_CHAR:
				instructions.add(new ArrayGet(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_char, inst.getOpcode(), this));
				break;
			case AGET_SHORT:
				instructions.add(new ArrayGet(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_short, inst.getOpcode(), this));
				break;
			case APUT:
				instructions.add(new ArrayPut(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_int, inst.getOpcode(), this));
				break;
			case APUT_WIDE:
				instructions.add(new ArrayPut(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_wide, inst.getOpcode(), this));
				break;
			case APUT_OBJECT:
				instructions.add(new ArrayPut(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_object, inst.getOpcode(), this));
				break;
			case APUT_BOOLEAN:
				instructions.add(new ArrayPut(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_boolean, inst.getOpcode(), this));
				break;
			case APUT_BYTE:
				instructions.add(new ArrayPut(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_byte, inst.getOpcode(), this));
				break;
			case APUT_CHAR:
				instructions.add(new ArrayPut(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_char, inst.getOpcode(), this));
				break;
			case APUT_SHORT:
				instructions.add(new ArrayPut(instLoc, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(), ((Instruction23x)inst).getRegisterC(), Type.t_short, inst.getOpcode(), this));
				break;
			case IGET:
			case IGET_WIDE:
			case IGET_OBJECT:
			case IGET_BOOLEAN:
			case IGET_BYTE:
			case IGET_CHAR:
			case IGET_SHORT: {
				String cname = ((FieldReference)((Instruction22c)inst).getReference()).getDefiningClass();
				String fname = ((FieldReference)((Instruction22c)inst).getReference()).getName();
				String ftname = ((FieldReference)((Instruction22c)inst).getReference()).getType();

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);
				if (fname.endsWith(";"))
					fname = fname.substring(0,fname.length()-1);
				if (ftname.endsWith(";"))
					ftname = ftname.substring(0,ftname.length()-1);

				instructions.add(new GetField.GetInstanceField(
						instLoc, ((Instruction22c)inst).getRegisterA(), ((Instruction22c)inst).getRegisterB(),
						cname, fname, ftname, inst.getOpcode(), this));
				break;
			}
			case IPUT:
			case IPUT_WIDE:
			case IPUT_OBJECT:
			case IPUT_BOOLEAN:
			case IPUT_BYTE:
			case IPUT_CHAR:
			case IPUT_SHORT: {
				String cname = ((FieldReference)((Instruction22c)inst).getReference()).getDefiningClass();
				String fname = ((FieldReference)((Instruction22c)inst).getReference()).getName();
				String ftname = ((FieldReference)((Instruction22c)inst).getReference()).getType();

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);
				if (fname.endsWith(";"))
					fname = fname.substring(0,fname.length()-1);
				if (ftname.endsWith(";"))
					ftname = ftname.substring(0,ftname.length()-1);

				instructions.add(new PutField.PutInstanceField(
						instLoc, ((TwoRegisterInstruction)inst).getRegisterA(), ((TwoRegisterInstruction)inst).getRegisterB(),
						cname, fname, ftname, inst.getOpcode(), this));
				break;
			}
			case SGET:
			case SGET_WIDE:
			case SGET_OBJECT:
			case SGET_BOOLEAN:
			case SGET_BYTE:
			case SGET_CHAR:
			case SGET_SHORT: {
				String cname = ((FieldReference)((Instruction21c)inst).getReference()).getDefiningClass();
				String fname = ((FieldReference)((Instruction21c)inst).getReference()).getName();
				String ftname = ((FieldReference)((Instruction21c)inst).getReference()).getType();

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);
				if (fname.endsWith(";"))
					fname = fname.substring(0,fname.length()-1);
				if (ftname.endsWith(";"))
					ftname = ftname.substring(0,ftname.length()-1);

				instructions.add(new GetField.GetStaticField(instLoc,
						((Instruction21c)inst).getRegisterA(), cname, fname, ftname, inst.getOpcode(), this));
				break;
			}
			case SPUT:
			case SPUT_WIDE:
			case SPUT_OBJECT:
			case SPUT_BOOLEAN:
			case SPUT_BYTE:
			case SPUT_CHAR:
			case SPUT_SHORT: {
				String cname = ((FieldReference)((Instruction21c)inst).getReference()).getDefiningClass();
				String fname = ((FieldReference)((Instruction21c)inst).getReference()).getName();
				String ftname = ((FieldReference)((Instruction21c)inst).getReference()).getType();

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);
				if (fname.endsWith(";"))
					fname = fname.substring(0,fname.length()-1);
				if (ftname.endsWith(";"))
					ftname = ftname.substring(0,ftname.length()-1);

				instructions.add(new PutField.PutStaticField(instLoc,
						((Instruction21c)inst).getRegisterA(), cname, fname, ftname, inst.getOpcode(), this));
				break;
			}
			case INVOKE_VIRTUAL: {
				int registerCount = ((Instruction35c)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
				{
					switch(i) {
					case 0:
						args[0] = ((Instruction35c)inst).getRegisterC();
						break;
					case 1:
						args[1] = ((Instruction35c)inst).getRegisterD();
						break;
					case 2:
						args[2] = ((Instruction35c)inst).getRegisterE();
						break;
					case 3:
						args[3] = ((Instruction35c)inst).getRegisterF();
						break;
					case 4:
						args[4] = ((Instruction35c)inst).getRegisterG();
						break;
					default:
						throw new RuntimeException("Illegal instruction at "
								+ instLoc + ": bad register count");
					}
				}

				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);
				//              if (mname.endsWith(";"))
				//                  mname = mname.substring(0,mname.length()-1);
				//              if (pname.endsWith(";"))
				//                  pname = pname.substring(0,pname.length()-1);

				//              for (IMethod m: this.myClass.loader.lookupClass(TypeName.findOrCreate(cname)).getDeclaredMethods())
				//                  System.out.println(m.getDescriptor().toString());

				handleINVOKE_VIRTUAL(instLoc, cname, mname, pname, args, inst.getOpcode());
				//instructions.add(new Invoke.InvokeVirtual(instLoc, cname, mname, pname, args, inst.opcode, this));
				//logger.debug("\t" + inst.opcode.toString() + " class: "+ cname + ", method name: " + mname + ", prototype string: " + pname);

				break;
			}
			case INVOKE_SUPER: {
				int registerCount = ((Instruction35c)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
				{
					switch(i) {
					case 0:
						args[0] = ((Instruction35c)inst).getRegisterC();
						break;
					case 1:
						args[1] = ((Instruction35c)inst).getRegisterD();
						break;
					case 2:
						args[2] = ((Instruction35c)inst).getRegisterE();
						break;
					case 3:
						args[3] = ((Instruction35c)inst).getRegisterF();
						break;
					case 4:
						args[4] = ((Instruction35c)inst).getRegisterG();
						break;
					default:
						throw new RuntimeException("Illegal instruction at "
								+ instLoc + ": bad register count");
					}
				}

				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);


				instructions.add(new Invoke.InvokeSuper(instLoc,
						cname, mname, pname, args, inst.getOpcode(), this));
				break;
			}
			case INVOKE_DIRECT: {
				int registerCount = ((Instruction35c)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
				{
					switch(i) {
					case 0:
						args[0] = ((Instruction35c)inst).getRegisterC();
						break;
					case 1:
						args[1] = ((Instruction35c)inst).getRegisterD();
						break;
					case 2:
						args[2] = ((Instruction35c)inst).getRegisterE();
						break;
					case 3:
						args[3] = ((Instruction35c)inst).getRegisterF();
						break;
					case 4:
						args[4] = ((Instruction35c)inst).getRegisterG();
						break;
					default:
						throw new RuntimeException("Illegal instruction at "
								+ instLoc + ": bad register count");
					}
				}

				//              logger.debug(inst.opcode.toString() + " class: "+((MethodIdItem)((Instruction35c)inst).getReferencedItem()).getContainingClass().getTypeDescriptor() + ", method name: " + ((MethodIdItem)((Instruction35c)inst).getReferencedItem()).getMethodName().getStringValue() + ", prototype string: " + ((MethodIdItem)((Instruction35c)inst).getReferencedItem()).getPrototype().getPrototypeString());
				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);


				instructions.add(new Invoke.InvokeDirect(instLoc,
						cname, mname, pname, args, inst.getOpcode(), this));

				break;
			}
			case INVOKE_STATIC: {
				int registerCount = ((Instruction35c)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
				{
					switch(i) {
					case 0:
						args[0] = ((Instruction35c)inst).getRegisterC();
						break;
					case 1:
						args[1] = ((Instruction35c)inst).getRegisterD();
						break;
					case 2:
						args[2] = ((Instruction35c)inst).getRegisterE();
						break;
					case 3:
						args[3] = ((Instruction35c)inst).getRegisterF();
						break;
					case 4:
						args[4] = ((Instruction35c)inst).getRegisterG();
						break;
					default:
						throw new RuntimeException("Illegal instruction at "
								+ instLoc + ": bad register count");
					}
				}

				//logger.debug(inst.opcode.toString() + " class: "+((MethodIdItem)((Instruction35c)inst).getReferencedItem()).getContainingClass().getTypeDescriptor() + ", method name: " + ((MethodIdItem)((Instruction35c)inst).getReferencedItem()).getMethodName().getStringValue() + ", prototype string: " + ((MethodIdItem)((Instruction35c)inst).getReferencedItem()).getPrototype().getPrototypeString());
				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);



				instructions.add(new Invoke.InvokeStatic(instLoc, cname, mname, pname, args, inst.getOpcode(), this));

				break;
			}
			case INVOKE_INTERFACE: {
				int registerCount = ((Instruction35c)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
				{
					switch(i) {
					case 0:
						args[0] = ((Instruction35c)inst).getRegisterC();
						break;
					case 1:
						args[1] = ((Instruction35c)inst).getRegisterD();
						break;
					case 2:
						args[2] = ((Instruction35c)inst).getRegisterE();
						break;
					case 3:
						args[3] = ((Instruction35c)inst).getRegisterF();
						break;
					case 4:
						args[4] = ((Instruction35c)inst).getRegisterG();
						break;
					default:
						throw new RuntimeException("Illegal instruction at "
								+ instLoc + ": bad register count");
					}
				}

				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction35c)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);


				instructions.add(new Invoke.InvokeInterface(instLoc,
						cname, mname, pname, args, inst.getOpcode(), this));
				break;
			}
			case INVOKE_VIRTUAL_RANGE: {
				int registerCount = ((Instruction3rc)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
					args[i] = ((Instruction3rc)inst).getStartRegister() + i;

				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);


				instructions.add(new Invoke.InvokeVirtual(instLoc,
						cname, mname, pname, args, inst.getOpcode(), this));
				break;
			}
			case INVOKE_SUPER_RANGE: {
				int registerCount = ((Instruction3rc)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
					args[i] = ((Instruction3rc)inst).getStartRegister() + i;

				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getName();
				String pname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getReturnType();

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);

				instructions.add(new Invoke.InvokeSuper(instLoc,
						cname, mname, pname, args, inst.getOpcode(), this));
				break;
			}
			case INVOKE_DIRECT_RANGE: {
				int registerCount = ((Instruction3rc)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
					args[i] = ((Instruction3rc)inst).getStartRegister() + i;

				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);

				instructions.add(new Invoke.InvokeDirect(instLoc,
						cname, mname, pname, args, inst.getOpcode(), this));
				break;
			}
			case INVOKE_STATIC_RANGE: {
				int registerCount = ((Instruction3rc)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
					args[i] = ((Instruction3rc)inst).getStartRegister() + i;

				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);


				instructions.add(new Invoke.InvokeStatic(instLoc, cname, mname, pname, args, inst.getOpcode(), this));

				break;
			}
			case INVOKE_INTERFACE_RANGE: {
				int registerCount = ((Instruction3rc)inst).getRegisterCount();
				int[] args = new int[registerCount];

				for (int i = 0; i < registerCount; i++)
					args[i] = ((Instruction3rc)inst).getStartRegister() + i;

				String cname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getDefiningClass();
				String mname = ((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference()).getName();
				String pname = DexUtil.getSignature((org.jf.dexlib2.iface.reference.MethodReference)((Instruction3rc)inst).getReference());

				if (cname.endsWith(";"))
					cname = cname.substring(0,cname.length()-1);

				instructions.add(new Invoke.InvokeInterface(instLoc,
						cname, mname, pname, args, inst.getOpcode(), this));
				break;
			}
			case NEG_INT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.NEGINT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case NOT_INT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.NOTINT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case NEG_LONG:
				instructions.add(new UnaryOperation(instLoc,
						OpID.NEGLONG, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case NOT_LONG:
				instructions.add(new UnaryOperation(instLoc,
						OpID.NOTLONG, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case NEG_FLOAT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.NEGFLOAT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case NEG_DOUBLE:
				instructions.add(new UnaryOperation(instLoc,
						OpID.NEGDOUBLE, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case INT_TO_LONG:
				instructions.add(new UnaryOperation(instLoc,
						OpID.INTTOLONG, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case INT_TO_FLOAT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.INTTOFLOAT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case INT_TO_DOUBLE:
				instructions.add(new UnaryOperation(instLoc,
						OpID.INTTODOUBLE, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case LONG_TO_INT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.LONGTOINT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case LONG_TO_FLOAT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.LONGTOFLOAT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case LONG_TO_DOUBLE:
				instructions.add(new UnaryOperation(instLoc,
						OpID.LONGTODOUBLE, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case FLOAT_TO_INT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.FLOATTOINT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case FLOAT_TO_LONG:
				instructions.add(new UnaryOperation(instLoc,
						OpID.FLOATTOLONG, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case FLOAT_TO_DOUBLE:
				instructions.add(new UnaryOperation(instLoc,
						OpID.FLOATTODOUBLE, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case DOUBLE_TO_INT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.DOUBLETOINT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case DOUBLE_TO_LONG:
				instructions.add(new UnaryOperation(instLoc,
						OpID.DOUBLETOLONG, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case DOUBLE_TO_FLOAT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.DOUBLETOFLOAT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case INT_TO_BYTE:
				instructions.add(new UnaryOperation(instLoc,
						OpID.INTTOBYTE, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case INT_TO_CHAR:
				instructions.add(new UnaryOperation(instLoc,
						OpID.INTTOCHAR, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case INT_TO_SHORT:
				instructions.add(new UnaryOperation(instLoc,
						OpID.INTTOSHORT, ((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case ADD_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.ADD_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case SUB_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SUB_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case MUL_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.MUL_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case DIV_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.DIV_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case REM_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.REM_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case AND_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.AND_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case OR_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.OR_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case XOR_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.XOR_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case SHL_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SHL_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case SHR_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SHR_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case USHR_INT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.USHR_INT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case ADD_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.ADD_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case SUB_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SUB_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case MUL_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.MUL_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case DIV_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.DIV_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case REM_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.REM_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case AND_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.AND_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case OR_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.OR_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case XOR_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.XOR_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case SHL_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SHL_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case SHR_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SHR_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case USHR_LONG:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.USHR_LONG, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case ADD_FLOAT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.ADD_FLOAT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case SUB_FLOAT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SUB_FLOAT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case MUL_FLOAT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.MUL_FLOAT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case DIV_FLOAT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.DIV_FLOAT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case REM_FLOAT:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.REM_FLOAT, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case ADD_DOUBLE:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.ADD_DOUBLE, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case SUB_DOUBLE:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SUB_DOUBLE, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case MUL_DOUBLE:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.MUL_DOUBLE, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case DIV_DOUBLE:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.DIV_DOUBLE, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case REM_DOUBLE:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.REM_DOUBLE, ((Instruction23x)inst).getRegisterA(),
						((Instruction23x)inst).getRegisterB(),((Instruction23x)inst).getRegisterC(), inst.getOpcode(), this));
				break;
			case ADD_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.ADD_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case SUB_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SUB_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MUL_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.MUL_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case DIV_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.DIV_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case REM_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.REM_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case AND_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.AND_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case OR_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.OR_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case XOR_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.XOR_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case SHL_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SHL_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case SHR_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SHR_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case USHR_INT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.USHR_INT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case ADD_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.ADD_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case SUB_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SUB_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MUL_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.MUL_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case DIV_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.DIV_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case REM_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.REM_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case AND_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.AND_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case OR_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.OR_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case XOR_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.XOR_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case SHL_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SHL_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case SHR_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SHR_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case USHR_LONG_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.USHR_LONG, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case ADD_FLOAT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.ADD_FLOAT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case SUB_FLOAT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SUB_FLOAT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MUL_FLOAT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.MUL_FLOAT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case DIV_FLOAT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.DIV_FLOAT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case REM_FLOAT_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.REM_FLOAT, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case ADD_DOUBLE_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.ADD_DOUBLE, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case SUB_DOUBLE_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.SUB_DOUBLE, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case MUL_DOUBLE_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.MUL_DOUBLE, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case DIV_DOUBLE_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.DIV_DOUBLE, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case REM_DOUBLE_2ADDR:
				instructions.add(new BinaryOperation(instLoc,
						BinaryOperation.OpID.REM_DOUBLE, ((Instruction12x)inst).getRegisterA(),
						((Instruction12x)inst).getRegisterA(), ((Instruction12x)inst).getRegisterB(), inst.getOpcode(), this));
				break;
			case ADD_INT_LIT16: {
				Literal lit = new Literal.LongLiteral(((Instruction22s)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.ADD_INT,
						((Instruction22s)inst).getRegisterA(), ((Instruction22s)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case RSUB_INT: {
				Literal lit = new Literal.LongLiteral(((Instruction22s)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.RSUB_INT,
						((Instruction22s)inst).getRegisterA(), ((Instruction22s)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case MUL_INT_LIT16: {
				Literal lit = new Literal.LongLiteral(((Instruction22s)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.MUL_INT,
						((Instruction22s)inst).getRegisterA(), ((Instruction22s)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case DIV_INT_LIT16: {
				Literal lit = new Literal.LongLiteral(((Instruction22s)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.DIV_INT,
						((Instruction22s)inst).getRegisterA(), ((Instruction22s)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case REM_INT_LIT16: {
				Literal lit = new Literal.LongLiteral(((Instruction22s)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.REM_INT,
						((Instruction22s)inst).getRegisterA(), ((Instruction22s)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case AND_INT_LIT16: {
				Literal lit = new Literal.LongLiteral(((Instruction22s)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.AND_INT,
						((Instruction22s)inst).getRegisterA(), ((Instruction22s)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case OR_INT_LIT16: {
				Literal lit = new Literal.LongLiteral(((Instruction22s)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.OR_INT,
						((Instruction22s)inst).getRegisterA(), ((Instruction22s)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case XOR_INT_LIT16: {
				Literal lit = new Literal.LongLiteral(((Instruction22s)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.XOR_INT,
						((Instruction22s)inst).getRegisterA(), ((Instruction22s)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case ADD_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.ADD_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case RSUB_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.RSUB_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case MUL_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.MUL_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case DIV_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.DIV_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case REM_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.REM_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case AND_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.AND_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case OR_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.OR_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case XOR_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.XOR_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case SHL_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.SHL_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case SHR_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.SHR_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			case USHR_INT_LIT8: {
				Literal lit = new Literal.LongLiteral(((Instruction22b)inst).getWideLiteral());
				instructions.add(new BinaryLiteralOperation(instLoc, BinaryLiteralOperation.OpID.USHR_INT,
						((Instruction22b)inst).getRegisterA(), ((Instruction22b)inst).getRegisterB(), lit, inst.getOpcode(), this));
				break;
			}
			default:
				throw new RuntimeException("not implemented instruction: 0x"
						+ inst.getOpcode().toString() + " in " + eMethod.getDefiningClass() + ":" + eMethod.getName());

			}
			currentCodeAddress += inst.getCodeUnits();
		}

		//// comment out start
		////        Instruction[] iinstructions = new Instruction[instrucs.length];
		//      instructions = new InsructionArray();
		//
		//
		//      for (int i = 0; i < instrucs.length; i++) {
		//          org.jf.dexlib.Code.Instruction instruction = instrucs[i];
		//
		//
		//          System.out.println(instruction.toString());
		//          switch (instruction.getFormat()) {
		//
		//  /*
		//-         Format10t(Instruction10t.Factory, 2),
		//-         Format10x(Instruction10x.Factory, 2),
		//-         Format11n(Instruction11n.Factory, 2),
		//-         Format11x(Instruction11x.Factory, 2),
		//-         Format12x(Instruction12x.Factory, 2),
		//-         Format20t(Instruction20t.Factory, 4),
		//-         Format21c(Instruction21c.Factory, 4),
		//          Format21h(Instruction21h.Factory, 4),
		//          Format21s(Instruction21s.Factory, 4),
		//          Format21t(Instruction21t.Factory, 4),
		//          Format22b(Instruction22b.Factory, 4),
		//          Format22c(Instruction22c.Factory, 4),
		//          Format22cs(Instruction22cs.Factory, 4),
		//          Format22s(Instruction22s.Factory, 4),
		//          Format22t(Instruction22t.Factory, 4),
		//          Format22x(Instruction22x.Factory, 4),
		//          Format23x(Instruction23x.Factory, 4),
		//-         Format30t(Instruction30t.Factory, 6),
		//          Format31c(Instruction31c.Factory, 6),
		//          Format31i(Instruction31i.Factory, 6),
		//          Format31t(Instruction31t.Factory, 6),
		//          Format32x(Instruction32x.Factory, 6),
		//          Format35c(Instruction35c.Factory, 6),
		//          Format35s(Instruction35s.Factory, 6),
		//          Format35ms(Instruction35ms.Factory, 6),
		//          Format3rc(Instruction3rc.Factory, 6),
		//          Format3rms(Instruction3rms.Factory, 6),
		//          Format51l(Instruction51l.Factory, 10),
		//          ArrayData(null, -1, true),
		//          PackedSwitchData(null, -1, true),
		//          SparseSwitchData(null, -1, true),
		//          UnresolvedOdexInstruction(null, -1, false),
		//*/
		//          case Format10t: { //goto
		//              
		//
		//              Instruction10t dInst = (Instruction10t)instruction;
		//
		//              int offset = dInst.getCodeOffset();
		//              instructions.add(new Goto(i,offset));
		//
		//              break;
		//          }
		//          case Format10x: {
		//              
		//              switch(instruction.opcode) {
		//              case RETURN_VOID: {
		//                  instructions.add(new Return.ReturnVoid(i));
		//                  break;
		//              }
		//              default:
		//                  break;
		//              }
		//
		//              break;
		//          }
		//
		//          case Format11n: {
		//              
		//              Instruction11n dInst = (Instruction11n) instruction;
		//
		//              System.out.println("here1");
		//              int a = (int)dInst.getLiteral();
		//              System.out.println("here2");
		//              Register b = regBank.get(dInst.getRegisterA());
		//              System.out.println("here3");
		//
		//              Constant.IntConstant c = new Constant.IntConstant(1, 2, b);
		//              //instructions.add(c);
		//              System.out.println("here5");
		//              instructions.add(new Constant.IntConstant(i,
		//                      (int)dInst.getLiteral(), regBank.get(dInst.getRegisterA())));
		//              System.out.println("here4");
		//              break;
		//          }
		//
		//          case Format11x: {
		//              /*
		//               *     MOVE_RESULT((byte)0x0a, "move-result", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//    MOVE_RESULT_WIDE((byte)0x0b, "move-result-wide", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//    MOVE_RESULT_OBJECT((byte)0x0c, "move-result-object", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//    MOVE_EXCEPTION((byte)0x0d, "move-exception", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//    RETURN((byte)0x0f, "return", ReferenceType.none, Format.Format11x),
		//    RETURN_WIDE((byte)0x10, "return-wide", ReferenceType.none, Format.Format11x),
		//    RETURN_OBJECT((byte)0x11, "return-object", ReferenceType.none, Format.Format11x),
		//    MONITOR_ENTER((byte)0x1d, "monitor-enter", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//    MONITOR_EXIT((byte)0x1e, "monitor-exit", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//    THROW((byte)0x27, "throw", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW),
		//               */
		//              
		//              Instruction11x dInst = (Instruction11x) instruction;
		//
		//              switch (dInst.opcode) {
		//              case MOVE_RESULT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          UnaryOperation.OpID.MOVE, regBank.get(dInst.getRegisterA()),
		//                          regBank.getReturnReg()));
		//                  break;
		//              }
		//              case MOVE_RESULT_WIDE: {
		//                  instructions.add(new UnaryOperation(i,
		//                          UnaryOperation.OpID.MOVE_WIDE,
		//                          regBank.get(dInst.getRegisterA()), regBank.getReturnReg()));
		//                  break;
		//              }
		//              case MOVE_RESULT_OBJECT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          UnaryOperation.OpID.MOVE, regBank.get(dInst.getRegisterA()),
		//                          regBank.getReturnReg()));
		//                  break;
		//              }
		//              case MOVE_EXCEPTION: {
		//                  instructions.add(new UnaryOperation(i,
		//                          UnaryOperation.OpID.MOVE, regBank.get(dInst.getRegisterA()),
		//                          regBank.getReturnExceptionReg()));
		//                  break;
		//              }
		//              case RETURN: {
		//                  instructions.add(new Return.ReturnSingle(i,
		//                          regBank.get(dInst.getRegisterA())));
		//                  break;
		//              }
		//              case RETURN_WIDE: {
		//                  instructions.add(new Return.ReturnDouble(i,
		//                          regBank.get(dInst.getRegisterA()), regBank.get(dInst.getRegisterA() + 1)));
		//                  break;
		//              }
		//              case RETURN_OBJECT: {
		//                  instructions.add(new Return.ReturnSingle(i,
		//                          regBank.get(dInst.getRegisterA())));
		//                  break;
		//              }
		//              case MONITOR_ENTER: {
		//                  instructions.add(new Monitor(i, true, regBank
		//                          .get(dInst.getRegisterA())));
		//                  break;
		//              }
		//              case MONITOR_EXIT: {
		//                  instructions.add(new Monitor(i, false, regBank
		//                          .get(dInst.getRegisterA())));
		//                  break;
		//              }
		//              case THROW: {
		//                  instructions.add(new Throw(i, regBank
		//                          .get(dInst.getRegisterA())));
		//                  break;
		//              }
		//              default:
		//                  break;
		//              }
		//              break;
		//          }
		//
		//          case Format12x: {
		//              
		//              Instruction12x dInst = (Instruction12x) instruction;
		//              int destination = dInst.getRegisterA();
		//              int source = dInst.getRegisterB();
		//
		//              switch(dInst.opcode) {
		//              //     MOVE((byte)0x01, "move", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case MOVE: {
		//                  instructions.add(new UnaryOperation(i,
		//                          UnaryOperation.OpID.MOVE, regBank.get(destination),
		//                          regBank.get(source)));
		//                  break;
		//              }
		//              //    MOVE_WIDE((byte)0x04, "move-wide", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case MOVE_WIDE: {
		//                  instructions.add(new UnaryOperation(i,
		//                          UnaryOperation.OpID.MOVE_WIDE,
		//                          regBank.get(destination), regBank.get(source)));
		//                  break;
		//              }
		//              //    MOVE_OBJECT((byte)0x07, "move-object", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case MOVE_OBJECT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          UnaryOperation.OpID.MOVE, regBank.get(destination),
		//                          regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    ARRAY_LENGTH((byte)0x21, "array-length", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case ARRAY_LENGTH: {
		//                  instructions.add(new ArrayLength(i, regBank
		//                          .get(destination), regBank.get(source)));
		//                  break;
		//              }
		//              //    NEG_INT((byte)0x7b, "neg-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case NEG_INT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.NEGINT, regBank.get(destination), regBank.get(source)));
		//                  break;
		//              }
		//              //    NOT_INT((byte)0x7c, "not-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case NOT_INT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.NOTINT, regBank.get(destination), regBank.get(source)));
		//                  break;
		//              }
		//              //    NEG_LONG((byte)0x7d, "neg-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case NEG_LONG: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.NEGLONG, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    NOT_LONG((byte)0x7e, "not-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case NOT_LONG: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.NOTLONG, regBank.get(destination), regBank.get(source)));
		//                  break;
		//              }
		//              //    NEG_FLOAT((byte)0x7f, "neg-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case NEG_FLOAT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.NEGFLOAT, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//
		//              }
		//              //    NEG_DOUBLE((byte)0x80, "neg-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case NEG_DOUBLE: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.NEGDOUBLE, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    INT_TO_LONG((byte)0x81, "int-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case INT_TO_LONG: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.INTTOLONG, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    INT_TO_FLOAT((byte)0x82, "int-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case INT_TO_FLOAT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.INTTOFLOAT, regBank.get(destination), regBank.get(source)));
		//                  break;
		//              }
		//              //    INT_TO_DOUBLE((byte)0x83, "int-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case INT_TO_DOUBLE: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.INTTODOUBLE, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    LONG_TO_INT((byte)0x84, "long-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case LONG_TO_INT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.LONGTOINT, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    LONG_TO_FLOAT((byte)0x85, "long-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case LONG_TO_FLOAT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.LONGTOFLOAT, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    LONG_TO_DOUBLE((byte)0x86, "long-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case LONG_TO_DOUBLE: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.LONGTODOUBLE, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    FLOAT_TO_INT((byte)0x87, "float-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case FLOAT_TO_INT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.FLOATTOINT, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    FLOAT_TO_LONG((byte)0x88, "float-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case FLOAT_TO_LONG: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.FLOATTOLONG, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    FLOAT_TO_DOUBLE((byte)0x89, "float-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case FLOAT_TO_DOUBLE: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.FLOATTODOUBLE, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    DOUBLE_TO_INT((byte)0x8a, "double-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case DOUBLE_TO_INT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.DOUBLETOINT, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    DOUBLE_TO_LONG((byte)0x8b, "double-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case DOUBLE_TO_LONG: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.DOUBLETOLONG, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    DOUBLE_TO_FLOAT((byte)0x8c, "double-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case DOUBLE_TO_FLOAT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.DOUBLETOFLOAT, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    INT_TO_BYTE((byte)0x8d, "int-to-byte", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case INT_TO_BYTE: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.INTTOBYTE, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    INT_TO_CHAR((byte)0x8e, "int-to-char", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case INT_TO_CHAR: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.INTTOCHAR, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    INT_TO_SHORT((byte)0x8f, "int-to-short", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case INT_TO_SHORT: {
		//                  instructions.add(new UnaryOperation(i,
		//                          OpID.INTTOSHORT, regBank.get(destination), regBank.get(source)));
		//
		//                  break;
		//              }
		//              //    ADD_INT_2ADDR((byte)0xb0, "add-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case ADD_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.ADD_INT, d, d, s));
		//                  break;
		//              }
		//              //    SUB_INT_2ADDR((byte)0xb1, "sub-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SUB_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.SUB_INT, d, d, s));
		//                  break;
		//              }
		//              //    MUL_INT_2ADDR((byte)0xb2, "mul-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case MUL_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.MUL_INT, d, d, s));
		//                  break;
		//              }
		//              //    DIV_INT_2ADDR((byte)0xb3, "div-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case DIV_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.DIV_INT, d, d, s));
		//                  break;
		//              }
		//              //    REM_INT_2ADDR((byte)0xb4, "rem-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case REM_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.REM_INT, d, d, s));
		//                  break;
		//              }
		//              //    AND_INT_2ADDR((byte)0xb5, "and-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case AND_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.AND_INT, d, d, s));
		//                  break;
		//              }
		//              //    OR_INT_2ADDR((byte)0xb6, "or-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case OR_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.OR_INT, d, d, s));
		//                  break;
		//              }
		//              //    XOR_INT_2ADDR((byte)0xb7, "xor-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case XOR_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.XOR_INT, d, d, s));
		//                  break;
		//              }
		//              //    SHL_INT_2ADDR((byte)0xb8, "shl-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SHL_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.SHL_INT, d, d, s));
		//                  break;
		//              }
		//              //    SHR_INT_2ADDR((byte)0xb9, "shr-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SHR_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.SHR_INT, d, d, s));
		//                  break;
		//              }
		//              //    USHR_INT_2ADDR((byte)0xba, "ushr-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case USHR_INT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.USHR_INT, d, d, s));
		//                  break;
		//              }
		//              //    ADD_LONG_2ADDR((byte)0xbb, "add-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case ADD_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.ADD_LONG, d, d, s));
		//                  break;
		//              }
		//              //    SUB_LONG_2ADDR((byte)0xbc, "sub-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case SUB_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.SUB_LONG, d, d, s));
		//                  break;
		//              }
		//              //    MUL_LONG_2ADDR((byte)0xbd, "mul-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case MUL_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.MUL_LONG, d, d, s));
		//                  break;
		//              }
		//              //    DIV_LONG_2ADDR((byte)0xbe, "div-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case DIV_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.DIV_LONG, d, d, s));
		//                  break;
		//              }
		//              //    REM_LONG_2ADDR((byte)0xbf, "rem-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case REM_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.REM_LONG, d, d, s));
		//                  break;
		//              }
		//              //    AND_LONG_2ADDR((byte)0xc0, "and-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case AND_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.AND_LONG, d, d, s));
		//                  break;
		//              }
		//              //    OR_LONG_2ADDR((byte)0xc1, "or-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case OR_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.OR_LONG, d, d, s));
		//                  break;
		//              }
		//              //    XOR_LONG_2ADDR((byte)0xc2, "xor-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case XOR_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.XOR_LONG, d, d, s));
		//                  break;
		//              }
		//              //    SHL_LONG_2ADDR((byte)0xc3, "shl-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case SHL_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.SHL_LONG, d, d, s));
		//                  break;
		//              }
		//              //    SHR_LONG_2ADDR((byte)0xc4, "shr-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case SHR_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.SHR_LONG, d, d, s));
		//                  break;
		//              }
		//              //    USHR_LONG_2ADDR((byte)0xc5, "ushr-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case USHR_LONG_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.USHR_LONG, d, d, s));
		//                  break;
		//              }
		//              //    ADD_FLOAT_2ADDR((byte)0xc6, "add-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case ADD_FLOAT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.ADD_FLOAT, d, d, s));
		//                  break;
		//              }
		//              //    SUB_FLOAT_2ADDR((byte)0xc7, "sub-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SUB_FLOAT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.SUB_FLOAT, d, d, s));
		//                  break;
		//              }
		//              //    MUL_FLOAT_2ADDR((byte)0xc8, "mul-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case MUL_FLOAT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.MUL_FLOAT, d, d, s));
		//                  break;
		//              }
		//              //    DIV_FLOAT_2ADDR((byte)0xc9, "div-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case DIV_FLOAT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.DIV_FLOAT, d, d, s));
		//                  break;
		//              }
		//              //    REM_FLOAT_2ADDR((byte)0xca, "rem-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case REM_FLOAT_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.REM_FLOAT, d, d, s));
		//                  break;
		//              }
		//              //    ADD_DOUBLE_2ADDR((byte)0xcb, "add-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case ADD_DOUBLE_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.ADD_DOUBLE, d, d, s));
		//                  break;
		//              }
		//              //    SUB_DOUBLE_2ADDR((byte)0xcc, "sub-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case SUB_DOUBLE_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.SUB_DOUBLE, d, d, s));
		//                  break;
		//              }
		//              //    MUL_DOUBLE_2ADDR((byte)0xcd, "mul-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case MUL_DOUBLE_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.MUL_DOUBLE, d, d, s));
		//                  break;
		//              }
		//              //    DIV_DOUBLE_2ADDR((byte)0xce, "div-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case DIV_DOUBLE_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.DIV_DOUBLE, d, d, s));
		//                  break;
		//              }
		//              //    REM_DOUBLE_2ADDR((byte)0xcf, "rem-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case REM_DOUBLE_2ADDR: {
		//                  Register d = regBank.get(destination);
		//                  Register s = regBank.get(source);
		//                  instructions.add(new BinaryOperation(i,
		//                          BinaryOperation.OpID.REM_DOUBLE, d, d, s));
		//                  break;
		//              }
		//              default:
		//                  break;
		//              }
		//
		//          break;
		//          }
		//
		//          case Format20t: { //goto/16
		//              
		//
		//              Instruction20t dInst = (Instruction20t)instruction;
		//
		//              int offset = dInst.getCodeOffset();
		//              instructions.add(new Goto(i,offset));
		//              break;
		//          }
		//          case Format30t: { //goto/32
		//              
		//
		//              Instruction30t dInst = (Instruction30t)instruction;
		//
		//              int offset = dInst.getCodeOffset();
		//              instructions.add(new Goto(i,offset));
		//              break;
		//          }
		//
		//          case Format21c: {
		//              Instruction21c dInst = (Instruction21c) instruction;
		//              
		//
		//              switch (dInst.opcode) {
		//              //CONST_STRING((byte)0x1a, "const-string", ReferenceType.string, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case CONST_STRING: {
		//                  int destination = dInst.getRegisterA();
		//                  instructions.add(new Constant.StringConstant(i,
		//                          dInst.getReferencedItem().getConciseIdentity(), regBank.get(destination)));
		//
		//                  break;
		//              }
		//              //    CONST_CLASS((byte)0x1c, "const-class", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case CONST_CLASS: {
		//                  int destination = dInst.getRegisterA();
		//                  IClass value = this.myClass.getClassLoader().lookupClass(TypeName.findOrCreate(dInst.getReferencedItem().getConciseIdentity()));
		//
		//                  instructions.add(new Constant.ClassConstant(i,
		//                      value, regBank.get(destination)));
		//                  break;
		//              }
		//              //    CHECK_CAST((byte)0x1f, "check-cast", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case CHECK_CAST: {
		//                  int val = dInst.getRegisterA();
		//                  String type = dInst.getReferencedItem().getConciseIdentity();
		//                  instructions.add(new CheckCast(i, TypeReference
		//                          .findOrCreate(this.myClass.getClassLoader().getReference(), type), regBank
		//                          .get(val)));
		//
		//                  break;
		//              }
		//              //    NEW_INSTANCE((byte)0x22, "new-instance", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case NEW_INSTANCE: {
		//                  int destination = dInst.getRegisterA();
		//                  String type = dInst.getReferencedItem().getConciseIdentity();
		//                  instructions.add(new New(i, regBank
		//                          .get(destination), NewSiteReference.make(i,
		//                          TypeReference
		//                                  .findOrCreate(this.myClass.getClassLoader().getReference(), type))));
		//                  break;
		//              }
		//              //    SGET((byte)0x60, "sget", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SGET:
		//              //    SGET_WIDE((byte)0x61, "sget-wide", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              case SGET_WIDE:
		//              //    SGET_OBJECT((byte)0x62, "sget-object", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SGET_OBJECT:
		//              //    SGET_BOOLEAN((byte)0x63, "sget-boolean", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SGET_BOOLEAN:
		//              //    SGET_BYTE((byte)0x64, "sget-byte", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SGET_BYTE:
		//              //    SGET_CHAR((byte)0x65, "sget-char", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SGET_CHAR:
		//              //    SGET_SHORT((byte)0x66, "sget-short", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              case SGET_SHORT: {
		//
		//                  /*  TODO
		//                  Register destination = regBank.get(dInst.getRegisterA());
		//                  Register source = regBank.get(dInst.getReferencedItem().getOffset());
		//                  int fieldIndex = codes[pc++];
		//                  this.eMethod
		//                  String clazzName = fieldClasses[fieldIndex];
		//                  String fieldName = fieldNames[fieldIndex];
		//                  String fieldType = fieldTypes[fieldIndex];
		//                  // getField(false, frame, source, fieldIndex, destination);
		//                  instructions.add(new GetField.GetInstanceField(
		//                          instLoc, destination, source, clazzName, fieldName,
		//                          fieldType));
		//*/
		//                  break;
		//              }
		//              //    SPUT((byte)0x67, "sput", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SPUT_WIDE((byte)0x68, "sput-wide", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SPUT_OBJECT((byte)0x69, "sput-object", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SPUT_BOOLEAN((byte)0x6a, "sput-boolean", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SPUT_BYTE((byte)0x6b, "sput-byte", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SPUT_CHAR((byte)0x6c, "sput-char", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SPUT_SHORT((byte)0x6d, "sput-short", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SGET_VOLATILE((byte)0xe5, "sget-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              //    SPUT_VOLATILE((byte)0xe6, "sput-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SGET_WIDE_VOLATILE((byte)0xea, "sget-wide-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
		//              //    SPUT_WIDE_VOLATILE((byte)0xeb, "sput-wide-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
		//              //    SGET_OBJECT_VOLATILE((byte)0xfd, "sget-object-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
		//              //    SPUT_OBJECT_VOLATILE((byte)0xfe, "sput-object-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE);
		//              default:
		//                  break;
		//              }
		//              break;
		//          }
		//
		//          case Format21t: {
		//
		//              Instruction21t dInst = (Instruction21t)instruction;
		//
		//              Register oper1 = regBank.get(dInst.getRegisterA());
		//              int offset = dInst.getCodeOffset();
		//
		//              switch(dInst.opcode) {
		//              case IF_EQZ:
		//                  instructions.add(new Branch.UnaryBranch(i,
		//                          offset, Branch.UnaryBranch.CompareOp.EQZ, oper1));
		//                  break;
		//              case IF_NEZ:
		//                  instructions.add(new Branch.UnaryBranch(i,
		//                          offset, Branch.UnaryBranch.CompareOp.NEZ, oper1));
		//                  break;
		//              case IF_LTZ:
		//                  instructions.add(new Branch.UnaryBranch(i,
		//                          offset, Branch.UnaryBranch.CompareOp.LTZ, oper1));
		//                  break;
		//              case IF_GEZ:
		//                  instructions.add(new Branch.UnaryBranch(i,
		//                          offset, Branch.UnaryBranch.CompareOp.GEZ, oper1));
		//                  break;
		//              case IF_GTZ:
		//                  instructions.add(new Branch.UnaryBranch(i,
		//                          offset, Branch.UnaryBranch.CompareOp.GTZ, oper1));
		//                  break;
		//              case IF_LEZ:
		//                  instructions.add(new Branch.UnaryBranch(i,
		//                          offset, Branch.UnaryBranch.CompareOp.LEZ, oper1));
		//                  break;
		//              default:
		//                  logger.debug(instruction.opcode.name + " - " + instruction.getFormat().toString());
		//                  break;
		//              }
		//              /*
		//  IF_EQZ((byte)0x38, "if-eqz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
		//    IF_NEZ((byte)0x39, "if-nez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
		//    IF_LTZ((byte)0x3a, "if-ltz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
		//    IF_GEZ((byte)0x3b, "if-gez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
		//    IF_GTZ((byte)0x3c, "if-gtz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
		//    IF_LEZ((byte)0x3d, "if-lez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
		//
		//               */
		//          break;
		//          }
		//          case Format22t: {
		//              Instruction22t dInst = (Instruction22t)instruction;
		//
		//
		//              Register oper1 = regBank.get(dInst.getRegisterA());
		//              Register oper2 = regBank.get(dInst.getRegisterB());
		//              int offset = dInst.getTargetAddressOffset();
		//
		//              switch(dInst.opcode) {
		//              case IF_EQ:
		//                  instructions.add(new Branch.BinaryBranch(i, offset,
		//                          Branch.BinaryBranch.CompareOp.EQ, oper1, oper2));
		//                  break;
		//              case IF_NE:
		//                  instructions.add(new Branch.BinaryBranch(i, offset,
		//                          Branch.BinaryBranch.CompareOp.NE, oper1, oper2));
		//                  break;
		//              case IF_LT:
		//                  instructions.add(new Branch.BinaryBranch(i, offset,
		//                          Branch.BinaryBranch.CompareOp.LT, oper1, oper2));
		//                  break;
		//              case IF_GE:
		//                  instructions.add(new Branch.BinaryBranch(i, offset,
		//                          Branch.BinaryBranch.CompareOp.GE, oper1, oper2));
		//                  break;
		//              case IF_GT:
		//                  instructions.add(new Branch.BinaryBranch(i, offset,
		//                          Branch.BinaryBranch.CompareOp.GT, oper1, oper2));
		//                  break;
		//              case IF_LE:
		//                  instructions.add(new Branch.BinaryBranch(i, offset,
		//                          Branch.BinaryBranch.CompareOp.LE, oper1, oper2));
		//                  break;
		//              default:
		//                  logger.debug(instruction.opcode.name + " - " + instruction.getFormat().toString());
		//                  break;
		//              }
		///*                IF_EQ((byte)0x32, "if-eq", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
		//              IF_NE((byte)0x33, "if-ne", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
		//              IF_LT((byte)0x34, "if-lt", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
		//              IF_GE((byte)0x35, "if-ge", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
		//              IF_GT((byte)0x36, "if-gt", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
		//              IF_LE((byte)0x37, "if-le", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
		//*/
		//              break;
		//          }
		//          case Format31t: {
		//              /*    PACKED_SWITCH((byte)0x2b, "packed-switch", ReferenceType.none, Format.Format31t, Opcode.CAN_CONTINUE),
		//    SPARSE_SWITCH((byte)0x2c, "sparse-switch", ReferenceType.none, Format.Format31t, Opcode.CAN_CONTINUE),
		//*/
		//              Instruction31t dInst = (Instruction31t)instruction;
		//              Register val = regBank.get(dInst.getRegisterA());
		//              int offset = dInst.getTargetAddressOffset();
		//  //          instructions.add(new Switch(i,
		//  //                  getSparseSwitchPad(offset, codes, 3), val));
		//              break;
		//          }
		//
		//
		//
		//          case Format35c: {
		//              
		//              // = invoke virtual
		//              //iinstructions[i] = new IInstruction35c((Instruction35c)instruction, this);
		//              break;
		//          }
		//
		//
		//          default:
		//              logger.debug(instruction.opcode.name + " - " + instruction.getFormat().toString());
		//              break;
		//          }
		//
		//
		//      }
		//
		//      //comment out stop
	}

	private static TypeReference findOutArrayElementType(
			org.jf.dexlib2.iface.instruction.Instruction inst, Instruction[] walaInstructions, int instCounter) {
		if (instCounter < 0) {
			throw new IllegalArgumentException();
		} else if (instCounter == 0) {
			throw new UnsupportedOperationException("fill-array-data as first instruction is not supported!");
		}
						
		Instruction31t arrayFill = (Instruction31t)inst;
		int interestingRegister = arrayFill.getRegisterA();
		int curCounter = instCounter - 1;

		while (curCounter >= 0) {
			Instruction curInst = walaInstructions[curCounter];
			// do we have a 'new-array'-instruction, where the destination register coincides with the current interesting register?
			// then we return the element type of that array
			if (curInst.getOpcode() == Opcode.NEW_ARRAY) {
				NewArray newArray = (NewArray) walaInstructions[curCounter];
				if (newArray.destination == interestingRegister) {
					return newArray.newSiteRef.getDeclaredType().getArrayElementType();
				}
			} else if (curInst.getOpcode() == Opcode.MOVE_OBJECT || curInst.getOpcode() == Opcode.MOVE_OBJECT_16 || curInst.getOpcode() == Opcode.MOVE_OBJECT_FROM16) {
				TwoRegisterInstruction tri = (TwoRegisterInstruction) curInst;
				int regA = tri.getRegisterA();
				int regB = tri.getRegisterB();
				if (regA == interestingRegister) {
					interestingRegister = regB;
				}
			}
			// all other instructions are ignored
			curCounter--;
		}

		throw new UnsupportedOperationException("found a fill-array-data instruction without a corresponding new-array instruction. This should not happen!");
	}

	protected void handleINVOKE_VIRTUAL(int instLoc, String cname, String mname, String pname, int[] args, Opcode opcode ) {
		instructions.add(new Invoke.InvokeVirtual(instLoc, cname, mname, pname, args, opcode, this));
	}

	public Instruction[] getDexInstructions() {
		return instructions().toArray(new Instruction[instructions().size()]);
	}



	protected InstructionArray instructions(){
		if (instructions == null)
			parseBytecode();
		return instructions;
	}

	public int getAddressFromIndex(int index) {
		return instructions().getPcFromIndex(index);
	}

	@Override
	public int getInstructionIndex(int bytecodeindex) {
		return instructions().getIndexFromPc(bytecodeindex);
	}

	public Instruction getInstructionFromIndex(int instructionIndex) {
		return instructions().getFromId(instructionIndex);
	}

	private static final IndirectionData NO_INDIRECTIONS = new IndirectionData() {

		private final int[] NOTHING = new int[0];

		@Override
		public int[] indirectlyReadLocals(int instructionIndex) {
			return NOTHING;
		}

		@Override
		public int[] indirectlyWrittenLocals(int instructionIndex) {
			return NOTHING;
		}

	};


	@Override
	public IndirectionData getIndirectionData() {
		return NO_INDIRECTIONS;
	}

	//-------------------------------------------
	// MethodAnnotationIteratorDelegate Methods
	//-------------------------------------------


//	/**
//	 * Delegate called by the class in order to parse the method annotations.
//	 */
//	public void processMethodAnnotations(MethodIdItem mIdItem,
//			AnnotationSetItem anoSet) {
//		//System.out.println("DexIMethod: processMethodAnnotations()");
//		if ( mIdItem.equals(eMethod.method) ){
//			AnnotationItem[] items = anoSet.getAnnotations();
//			for (AnnotationItem item : items) {
//				
//			}
//
//		}
//	}

    /**
     *
     * @throws UnsupportedOperationException
     *
     * TODO: Review this implementation - it may be horribly wrong!
     */
	@Override
	public Collection<CallSiteReference> getCallSites() {
        Collection<CallSiteReference> empty = Collections.emptySet();
        if (isNative()) {
            return empty;
        }

        // assert(false) : "Please review getCallSites-Implementation before use!";        // TODO

        ArrayList<CallSiteReference> csites = new ArrayList<>();
        // XXX The call Sites in this method or to this method?!!!
        for (Instruction inst: instructions()) {
            if (inst instanceof Invoke) {
                // Locate the Target
            	MethodReference target = MethodReference.findOrCreate(
                    getDeclaringClass().getClassLoader().getReference(),    // XXX: Is this the correct class loader?
                    ((Invoke)inst).clazzName,
                    ((Invoke)inst).methodName,
                    ((Invoke)inst).descriptor );

                csites.add(
                    CallSiteReference.make(
                        inst.pc,    // programCounter
                        target,     // declaredTarget
                        ((Invoke)inst).getInvocationCode() // invocationCode
                        ));
            }
        }
        return Collections.unmodifiableCollection(csites);
	}

	@Override
	public SourcePosition getSourcePosition(int instructionIndex) {
		return null;
	}

	@Override
	public SourcePosition getParameterSourcePosition(int paramNum) {
		return null;
	}

	@Override
	public Collection<Annotation> getAnnotations() {
		return myClass.getAnnotations(eMethod, null);
	}

	@Override
	public Collection<Annotation> getAnnotations(boolean runtimeInvisible) {
		return myClass.getAnnotations(eMethod, DexIClass.getTypes(runtimeInvisible));
	}

	@Override
	public Collection<Annotation>[] getParameterAnnotations() {
		Map<Integer, List<Annotation>> raw = myClass.getParameterAnnotations(eMethod);
		@SuppressWarnings("unchecked")
		Collection<Annotation>[] result = new Collection[ getReference().getNumberOfParameters() ];
		for(Map.Entry<Integer, List<Annotation>> x : raw.entrySet()) {
			result[x.getKey()] = x.getValue();
		}
		return result;
	}

	
}
