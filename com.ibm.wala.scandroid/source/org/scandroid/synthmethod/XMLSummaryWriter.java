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

import java.io.ByteArrayOutputStream;
import java.io.UTFDataFormatException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.Atom;

public class XMLSummaryWriter {

    //
    // Define XML element names
    //
    final static String E_CLASSLOADER = "classloader";
    final static String E_METHOD = "method";
    final static String E_CLASS = "class";
    final static String E_PACKAGE = "package";
    final static String E_CALL = "call";
    final static String E_NEW = "new";
    final static String E_POISON = "poison";
    final static String E_SUMMARY_SPEC = "summary-spec";
    final static String E_RETURN = "return";
    final static String E_PUTSTATIC = "putstatic";
    final static String E_GETSTATIC = "getstatic";
    final static String E_PUTFIELD = "putfield";
    final static String E_AALOAD = "aaload";
    final static String E_AASTORE = "aastore";
    final static String E_GETFIELD = "getfield";
    final static String E_ATHROW = "throw";
    final static String E_CONSTANT = "constant";

    //
    // Define XML attribute names
    //
    final static String A_NAME = "name";
    final static String A_TYPE = "type";
    final static String A_CLASS = "class";
    final static String A_SIZE = "size";
    final static String A_DESCRIPTOR = "descriptor";
    final static String A_REASON = "reason";
    final static String A_LEVEL = "level";
    final static String A_WILDCARD = "*";
    final static String A_DEF = "def";
    final static String A_STATIC = "static";
    final static String A_VALUE = "value";
    final static String A_FIELD = "field";
    final static String A_FIELD_TYPE = "fieldType";
    final static String A_ARG = "arg";
    final static String A_ALLOCATABLE = "allocatable";
    final static String A_REF = "ref";
    final static String A_INDEX = "index";
    final static String A_IGNORE = "ignore";
    final static String A_FACTORY = "factory";
    final static String A_NUM_ARGS = "numArgs";
    final static String V_NULL = "null";
    final static String V_TRUE = "true";

    private final Document doc;
	private final Element rootElement;
	private Element clrElt = null;
	private Element pkgElt = null;
	private final Map<Atom, Element> classElts;

    public XMLSummaryWriter() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        doc = docBuilder.newDocument();
        rootElement = doc.createElement(E_SUMMARY_SPEC);
        doc.appendChild(rootElement);
        classElts = HashMapFactory.make();
    }

    public String serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // write the content into xml file
        TransformerFactory transformerFactory =
                TransformerFactory.newInstance();
        // transformerFactory.setAttribute("indent-number", new Integer(4));
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(baos);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        
        // using the default encoding here, since the bytes were just written
        // with a default encoding...
        return baos.toString();
    }
    
    /**
     * Throws various exceptions if a problem occurred serializing this method.
     * 
     * No guarantees as to the state of the Document if an exception is thrown.
     * 
     * @param summary
     * @throws DOMException
     * @throws UTFDataFormatException
     * @throws SSASerializationException
     */
	public void add(MethodSummary summary) throws UTFDataFormatException {
		// create a method element, and populate it's attributes:
		Element methElt;
		TypeReference methClass = summary.getMethod().getDeclaringClass();

		Atom clrName = methClass.getClassLoader().getName();
		Atom pkg = methClass.getName().getPackage();
		Atom className = methClass.getName().getClassName();
		Atom methodName = summary.getMethod().getName();

		methElt = doc.createElement(E_METHOD);
		methElt.setAttribute(A_NAME, methodName.toUnicodeString());

		String descriptor = getMethodDescriptor(summary);
		methElt.setAttribute(A_DESCRIPTOR, descriptor);

		// default is false:
		if (summary.isStatic()) {
			methElt.setAttribute(A_STATIC, "true");
		}

		// default is false:
		if (summary.isFactory()) {
			methElt.setAttribute(A_FACTORY, "true");
		}

		// summarize the instructions:
		List<Element> instructions = summarizeInstructions(summary);
		for (Element elt : instructions) {
			methElt.appendChild(elt);
		}

		// get an element to add this method to:
		Element classElt = findOrCreateClassElt(clrName, pkg, className);
		classElt.appendChild(methElt);
	}

    private Element findOrCreateClassElt(Atom classLoaderName, Atom pkg, Atom className)
            throws UTFDataFormatException {
    	Element classElt = classElts.get(className);
    	if (classElt == null) {
    		Element pkgElt = findOrCreatePkgElt(classLoaderName, pkg);
    		classElt = doc.createElement(E_CLASS);        

    		classElt.setAttribute(A_NAME, className.toUnicodeString());
    		pkgElt.appendChild(classElt);
    		classElts.put(className, classElt);
    	}
        return classElt;
    }

    private Element findOrCreateClrElt(Atom classLoaderName) throws DOMException, 
        UTFDataFormatException {
        
    	if (clrElt == null) {
    		clrElt = doc.createElement(E_CLASSLOADER);
    		clrElt.setAttribute(A_NAME, classLoaderName.toUnicodeString());
    		rootElement.appendChild(clrElt);
    	}
        return clrElt;
    }

    private Element findOrCreatePkgElt(Atom classLoaderName, Atom pkg) throws UTFDataFormatException {
    	if (pkgElt == null) {
    		Element clrElt = findOrCreateClrElt(classLoaderName);
    		pkgElt = doc.createElement(E_PACKAGE);
    		pkgElt.setAttribute(A_NAME, pkg.toUnicodeString());
    		clrElt.appendChild(pkgElt);
    	}
        return pkgElt;
    }

    private List<Element> summarizeInstructions(MethodSummary summary) {
        SSAtoXMLVisitor v = 
                new SSAtoXMLVisitor(doc, summary.getNumberOfParameters());

        for (SSAInstruction inst : summary.getStatements()) {
            inst.visit(v);
        }

        return v.getInstSummary();
    }

    /**
     * Generate a method descriptor, such as
     * (I[Ljava/lang/String;)[Ljava/lang/String;
     * 
     * @param summary
     */
    private static String getMethodDescriptor(MethodSummary summary) {
        StringBuilder typeSigs = new StringBuilder("(");
        
        int i=0;
        if (!summary.isStatic()) {
        	i = 1; // if it's not static, start with param 1.
        }
        
        for (; i < summary.getNumberOfParameters(); i++) {
            TypeReference tr = summary.getParameterType(i);

            // unwrap array types
            while (tr.isArrayType()) {
            	typeSigs.append("[");
            	tr = tr.getArrayElementType();
            }
            
            if (tr.isPrimitiveType()) {
            	typeSigs.append(tr.getName().toUnicodeString());
            } else {
            	typeSigs.append(tr.getName().toUnicodeString()+ ";");
            }
        }
        typeSigs.append(")");
        
        TypeReference returnType = summary.getReturnType();
        if (returnType.isPrimitiveType()) {
        	typeSigs.append(returnType.getName().toUnicodeString());
        } else {
        	typeSigs.append(returnType.getName().toUnicodeString() + ";");
        }
        String descriptor = typeSigs.toString();
        return descriptor;
    }
}
