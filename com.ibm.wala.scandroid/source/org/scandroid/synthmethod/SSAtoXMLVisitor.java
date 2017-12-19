/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, 
 *                Rogan Creswick <creswick@galois.com>, 
 *                Adam Foltzer <acfoltzer@galois.com>)
 *  Steve Suh    <suhsteve@gmail.com>
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
package org.scandroid.synthmethod;

import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.Atom;

public class SSAtoXMLVisitor implements SSAInstruction.IVisitor {
    /**
     * A counter to use for generating unique local definition names.
     */
    private int defCounter = 0;

    /**
     * Map the known defNum to local def names.
     */
    private Map<Integer, String> localDefs = HashMapFactory.make();

    /**
     * XML document to use for creating elements.
     */
    private final Document doc;

    /**
     * XML elements that represent the ssa instructions
     */
    private final List<Element> summary = new ArrayList<>();

    public SSAtoXMLVisitor(Document doc, int argCount) {
        this.doc = doc;
        for (int i=0; i < argCount; i++) {
            localDefs.put(i+1, "arg"+i);
        }
    }

    @Override
    public void visitGoto(SSAGotoInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    /**
     * Load from an array ref, at specified index, and store in def.
     * 
     *   <aaload ref="x" index="0" def="y" />
     */
    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
        try {
            Element elt = doc.createElement(XMLSummaryWriter.E_AALOAD);
            
            String refStr = getLocalName(instruction.getArrayRef());
            elt.setAttribute(XMLSummaryWriter.A_REF, refStr);
            
            String defStr = getLocalName(instruction.getDef());
            elt.setAttribute(XMLSummaryWriter.A_VALUE, defStr);
            
            elt.setAttribute(XMLSummaryWriter.A_INDEX, ""+instruction.getIndex());
            summary.add(elt);
        } catch (Exception e) {
            throw new SSASerializationException(e);
        }
    }

    /**
     *    <aastore ref="x" value="y" index="0" />
     */
    @Override
    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
        try {
            Element elt = doc.createElement(XMLSummaryWriter.E_AASTORE);
            
            String refStr = getLocalName(instruction.getArrayRef());
            elt.setAttribute(XMLSummaryWriter.A_REF, refStr);
            
            String valueStr = getLocalName(instruction.getValue());
            elt.setAttribute(XMLSummaryWriter.A_VALUE, valueStr);
            
            elt.setAttribute(XMLSummaryWriter.A_INDEX, ""+instruction.getIndex());
            summary.add(elt);
        } catch (Exception e) {
            throw new SSASerializationException(e);
        }
    }

    @Override
    public void visitBinaryOp(SSABinaryOpInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitConversion(SSAConversionInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitComparison(SSAComparisonInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitConditionalBranch(
            SSAConditionalBranchInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitSwitch(SSASwitchInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitReturn(SSAReturnInstruction instruction) {
        try {
        	Element elt = doc.createElement(XMLSummaryWriter.E_RETURN);
            if (!instruction.returnsVoid()) {
            	String localName = getLocalName(instruction.getResult());
            	elt.setAttribute(XMLSummaryWriter.A_VALUE, localName);
            }
            summary.add(elt);
        } catch (Exception e) {
            throw new SSASerializationException(e);
        }
    }

    /**
     * eg:
     * 
     * <getfield class="Ljava/lang/Thread" field="runnable"
     * fieldType="Ljava/lang/Runnable" def="x" ref="arg0" />
     * 
     * I think the get statics look like this:
     * 1007g 9.1g  12m S 237.9  0.9   4:27.32 java
     * <getstatic class="Ljava/lang/Thread" field="runnable"
     * fieldType="Ljava/lang/Runnable" def="x" />
     */
    @Override
    public void visitGet(SSAGetInstruction instruction) {
        try {
            String eltName;

            if (instruction.isStatic()) {
                eltName = XMLSummaryWriter.E_GETSTATIC;
            } else {
                eltName = XMLSummaryWriter.E_GETFIELD;
            }
            Element elt = doc.createElement(eltName);

            if (!instruction.isStatic()) {
                String refName = getLocalName(instruction.getRef());
                elt.setAttribute(XMLSummaryWriter.A_REF, refName);
            }

            String def = newLocalDef(instruction.getDef());
            TypeReference fieldType = instruction.getDeclaredFieldType();
            TypeReference classType = instruction.getDeclaredField()
                    .getDeclaringClass();

            String fieldName = instruction.getDeclaredField().getName()
                    .toUnicodeString();

            elt.setAttribute(XMLSummaryWriter.A_CLASS, classType.getName().toUnicodeString());
            elt.setAttribute(XMLSummaryWriter.A_FIELD, fieldName);
            elt.setAttribute(XMLSummaryWriter.A_FIELD_TYPE,
                    fieldType.getName().toUnicodeString());
            elt.setAttribute(XMLSummaryWriter.A_DEF, def);

            summary.add(elt);
        } catch (Exception e) {
            throw new SSASerializationException(e);
        }
    }

    /**
     * <putstatic class="Ljava/lang/System" field="security"
     * fieldType="Ljava/lang/SecurityManager" value="secure" />
     * 
     * <putfield class="Ljava/lang/Thread" field="runnable"
     * fieldType="Ljava/lang/Runnable" ref="arg0" value="arg0" />
     * 
     */
    @Override
    public void visitPut(SSAPutInstruction instruction) {
        try {
            String eltName;

            if (instruction.isStatic()) {
                eltName = XMLSummaryWriter.E_PUTSTATIC;
            } else {
                eltName = XMLSummaryWriter.E_PUTFIELD;
            }
            Element elt = doc.createElement(eltName);

            if (!instruction.isStatic()) {
                String refName = getLocalName(instruction.getRef());
                elt.setAttribute(XMLSummaryWriter.A_REF, refName);
            }

            String value = getLocalName(instruction.getVal());
            TypeReference fieldType = instruction.getDeclaredFieldType();
            TypeReference classType = instruction.getDeclaredField()
                    .getDeclaringClass();

            String fieldName = instruction.getDeclaredField().getName()
                    .toUnicodeString();

            elt.setAttribute(XMLSummaryWriter.A_CLASS, classType.getName().toUnicodeString());
            elt.setAttribute(XMLSummaryWriter.A_FIELD, fieldName);
            elt.setAttribute(XMLSummaryWriter.A_FIELD_TYPE,
                    fieldType.getName().toUnicodeString());
            elt.setAttribute(XMLSummaryWriter.A_VALUE, value);

            summary.add(elt);
        } catch (Exception e) {
            throw new SSASerializationException(e);
        }

    }

    /**
     * 	  <call type="virtual" name="put"
	 *          class="Ljava/util/Hashtable"
	 *          descriptor="(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
	 *          arg0="x" arg1="key" arg2="value" def="local_def" />
     */
    @Override
    public void visitInvoke(SSAInvokeInstruction instruction) {
        try {
            Element elt = doc.createElement(XMLSummaryWriter.E_CALL);
            
            MethodReference callee = instruction.getDeclaredTarget();
            
            String descString = callee.getDescriptor().toUnicodeString();
            elt.setAttribute(XMLSummaryWriter.A_DESCRIPTOR, descString);
            
            String typeString = 
                instruction.getCallSite().getInvocationString();
            elt.setAttribute(XMLSummaryWriter.A_TYPE, typeString);
            
            String nameString = callee.getName().toUnicodeString();
            elt.setAttribute(XMLSummaryWriter.A_NAME, nameString);
            
            String classString = instruction.getDeclaredTarget().getDeclaringClass().getName().toUnicodeString();
            elt.setAttribute(XMLSummaryWriter.A_CLASS, classString);

            if (! instruction.getDeclaredResultType().equals(TypeReference.Void) ) {
                int defNum = instruction.getDef();
                String localName = newLocalDef(defNum);
                elt.setAttribute(XMLSummaryWriter.A_DEF, localName);
            }

            int paramCount = instruction.getNumberOfParameters();
            for (int i=0; i < paramCount; i++) {
            	String argName = getLocalName(instruction.getUse(i));
            	elt.setAttribute(XMLSummaryWriter.A_ARG+i, argName);
            }
            
            summary.add(elt);
        } catch (Exception e) {
            throw new SSASerializationException(e);
        }
        
    }

    @Override
    public void visitNew(SSANewInstruction instruction) {
        try {
            int defNum = instruction.getDef();
            String localName = newLocalDef(defNum);

            TypeReference type = instruction.getConcreteType();

            String className = type.getName().toUnicodeString();

            Element elt = doc.createElement(XMLSummaryWriter.E_NEW);
            elt.setAttribute(XMLSummaryWriter.A_DEF, localName);
            elt.setAttribute(XMLSummaryWriter.A_CLASS, className);
            
            if (type.isArrayType()) {
            	// array allocations need a size value
            	Element sizeElt = doc.createElement(XMLSummaryWriter.E_CONSTANT);
            	final String sizeName = "sizeOf$allocAt" + instruction.getNewSite().getProgramCounter();
				sizeElt.setAttribute(XMLSummaryWriter.A_NAME, sizeName);
				sizeElt.setAttribute(XMLSummaryWriter.A_TYPE, "int");
				sizeElt.setAttribute(XMLSummaryWriter.A_VALUE, "1");
				summary.add(sizeElt);
				
            	elt.setAttribute(XMLSummaryWriter.A_SIZE, sizeName);
            }
            
            summary.add(elt);
        } catch (Exception e) {
            throw new SSASerializationException(e);
        }
    }

    @Override
    public void visitArrayLength(SSAArrayLengthInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    /**
     * Serialiaze a throw to XML.
     * 
     * Something like this?
     * 
     * <throw value="val_localDef" /> 
     */
    @Override
    public void visitThrow(SSAThrowInstruction instruction) {
        throw new SSASerializationException("Exceptions not currently supported.");
//        try {
//            int exValNo = instruction.getException();
//            String value = getLocalName(exValNo);
//            
//            Element elt = doc.createElement(XMLSummaryWriter.E_ATHROW);
//            elt.setAttribute(XMLSummaryWriter.A_VALUE, value);
//            summary.add(elt);
//        } catch (Exception e) {
//            throw new SSASerializationException(e);
//        }
    }

    @Override
    public void visitMonitor(SSAMonitorInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitCheckCast(SSACheckCastInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitInstanceof(SSAInstanceofInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitPhi(SSAPhiInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitPi(SSAPiInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitGetCaughtException(
            SSAGetCaughtExceptionInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    @Override
    public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
        throw new SSASerializationException("Unsupported.");
    }

    /**
     * Add a new defNum, creating a name for that defnum.
     * 
     * @param defNum
     */
    private String newLocalDef(int defNum) {
        String newName = "localdef_" + defCounter;
        localDefs.put(defNum, newName);
        defCounter++;

        return newName;
    }

    /**
     * Get a local name for the provided defNum.
     * 
     * If, for some reason, the defNum has not yet been seen (and, thus, has no
     * local name associated with it) then this will throw an illegal state
     * exception.
     *
     * TODO needs to return 'arg0' -&gt; 'argN' for those value numbers...
     *
     * @param defNum
     *
     * @throws IllegalStateException
     */
    private String getLocalName(int defNum) throws IllegalStateException {
    	if (0 == defNum) {
    		return "unknown";
    	}
        if (localDefs.containsKey(defNum)) {
            return localDefs.get(defNum);
        }
        return XMLSummaryWriter.A_ARG + (defNum - 1);
//        throw new IllegalStateException("defNum: " + defNum
//                + " is not defined.");
    }


    public List<Element> getInstSummary() {
        return summary;
    }

    @SuppressWarnings("unused")
	private static String typeRefToStr(TypeReference fieldType)
            throws UTFDataFormatException {
        Atom className = fieldType.getName().getClassName();
        Atom pkgName = fieldType.getName().getPackage();
        if ( null == pkgName && null != className ) {
        	
        	return className.toUnicodeString();
        }

        if (null == className ) {
        	
        }
        
        return pkgName.toUnicodeString() + "/" + className.toUnicodeString();
    }

}
