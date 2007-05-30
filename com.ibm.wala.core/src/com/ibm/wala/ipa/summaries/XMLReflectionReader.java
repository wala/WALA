/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.summaries;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ReflectionSpecification;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

/**
 * 
 * This class reads reflection summaries from an XML Stream. TODO: share part of
 * the implementation with XMLMethodSummaryReader
 * 
 * @author sfink
 * 
 */
public class XMLReflectionReader implements BytecodeConstants, ReflectionSpecification {

  static final boolean DEBUG = false;

  private AnalysisScope scope;

  /**
   * Method summaries collected for methods. Mapping MethodReference ->
   * ReflectionSummary
   */
  private HashMap<MethodReference, ReflectionSummary> summaries = HashMapFactory.make();

  //
  // Define XML element names
  //
  private final static int E_CLASSLOADER = 0;

  private final static int E_METHOD = 1;

  private final static int E_CLASS = 2;

  private final static int E_PACKAGE = 3;

  private final static int E_REFLECTION_SPEC = 4;

  private final static int E_NEW_INSTANCE = 5;

  private final static int E_TYPE = 6;

  private final static Map<String, Integer> elementMap = HashMapFactory.make(7);
  static {
    elementMap.put("classloader", new Integer(E_CLASSLOADER));
    elementMap.put("method", new Integer(E_METHOD));
    elementMap.put("class", new Integer(E_CLASS));
    elementMap.put("package", new Integer(E_PACKAGE));
    elementMap.put("reflectionSpec", new Integer(E_REFLECTION_SPEC));
    elementMap.put("newInstance", new Integer(E_NEW_INSTANCE));
    elementMap.put("type", new Integer(E_TYPE));
  }

  //
  // Define XML attribute names
  //
  private final static String A_NAME = "name";

  private final static String A_DESCRIPTOR = "descriptor";

  private final static String A_BCINDEX = "bcIndex";

  /**
   * Constructor for XMLReflectionReader.
   */
  public XMLReflectionReader(InputStream xmlFile, AnalysisScope scope) {
    super();
    Assertions.productionAssertion(xmlFile != null, "XMLMethodSummaryReader given null xmlFile");
    this.scope = scope;
    try {
      readXML(xmlFile);
    } catch (Exception e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  private void readXML(InputStream xml) throws SAXException, IOException, ParserConfigurationException {
    SAXHandler handler = new SAXHandler();

    if (Assertions.verifyAssertions) {
      Assertions._assert(xml != null, "Null xml stream");
    }

    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.newSAXParser().parse(new InputSource(xml), handler);
  }

  /**
   * @return Method summaries collected for methods. Mapping MethodReference ->
   *         ReflectionSummary
   */
  public Map<MethodReference, ReflectionSummary> getSummaries() {
    return summaries;
  }

  /**
   * @author sfink
   * 
   * SAX parser logic for XML method summaries
   */
  private class SAXHandler extends DefaultHandler {
    /**
     * The class loader reference for the element being processed
     */
    private ClassLoaderReference governingLoader = null;

    /**
     * The method summary for the element being processed
     */
    private ReflectionSummary governingMethod = null;

    /**
     * The declaring class for the element begin processed
     */
    private TypeReference governingClass = null;

    /**
     * The package for the element being processed
     */
    private Atom governingPackage = null;

    /**
     * The current bytecode index of the current newInstance call being
     * processed
     */
    private int bcIndex = -1;

    /*
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String name, String qName, Attributes atts) {
      Integer element = elementMap.get(qName);
      if (element == null) {
        Assertions.UNREACHABLE("Invalid element: " + qName);
      }
      switch (element.intValue()) {
      case E_CLASSLOADER: {
        String clName = atts.getValue(A_NAME);
        governingLoader = classLoaderName2Ref(clName);
      }
        break;
      case E_METHOD:
        startMethod(atts);
        break;
      case E_CLASS:
        String cname = atts.getValue(A_NAME);
        startClass(cname);
        break;
      case E_TYPE:
        String tName = atts.getValue(A_NAME);
        processType(tName);
        break;
      case E_NEW_INSTANCE:
        String bcIndexString = atts.getValue(A_BCINDEX);
        bcIndex = Integer.parseInt(bcIndexString);
        break;
      case E_PACKAGE:
        governingPackage = Atom.findOrCreateUnicodeAtom(atts.getValue(A_NAME).replace('.', '/'));
        break;
      case E_REFLECTION_SPEC:
        break;
      default:
        Assertions.UNREACHABLE("Unexpected element: " + name);
        break;
      }
    }

    /**
     * record that the currently active newInstance may allocate a particular
     * concrete type.
     * 
     * @param tName
     */
    private void processType(String tName) {
      TypeReference T = className2Ref(tName);
      governingMethod.addType(bcIndex, T);
    }

    private void startClass(String cname) {
      String clName = "L" + governingPackage + "." + cname;
      governingClass = className2Ref(clName);
    }

    /*
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String name, String qName) {
      Integer element = elementMap.get(qName);
      if (element == null) {
        Assertions.UNREACHABLE("Invalid element: " + name);
      }
      switch (element.intValue()) {
      case E_CLASSLOADER:
        governingLoader = null;
        break;
      case E_METHOD:
        governingMethod = null;
        break;
      case E_CLASS:
        governingClass = null;
        break;
      case E_NEW_INSTANCE:
        bcIndex = -1;
        break;
      case E_PACKAGE:
        governingPackage = null;
        break;
      case E_TYPE:
      case E_REFLECTION_SPEC:
        break;
      default:
        Assertions.UNREACHABLE("Unexpected element: " + name);
        break;
      }
    }

    /**
     * Begin processing of a method. 1. Set the governing method. 2. Initialize
     * the nextLocal variable
     * 
     * @param atts
     */
    private void startMethod(Attributes atts) {

      String methodName = atts.getValue(A_NAME);
      Atom mName = Atom.findOrCreateUnicodeAtom(methodName);
      String descString = atts.getValue(A_DESCRIPTOR).replace('.', '/');
      Descriptor D = Descriptor.findOrCreateUTF8(descString);

      MethodReference ref = MethodReference.findOrCreate(governingClass, mName, D);
      governingMethod = new ReflectionSummary();

      if (DEBUG) {
        Trace.println("Register method summary: " + ref);
      }
      summaries.put(ref, governingMethod);
    }

    private ClassLoaderReference classLoaderName2Ref(String clName) {
      return scope.getLoader(Atom.findOrCreateUnicodeAtom(clName));
    }

    private TypeReference className2Ref(String clName) {
      clName = clName.replace('.', '/');
      return TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(clName));
    }
  }

  public TypeAbstraction getTypeForNewInstance(MemberReference method, int bcIndex, ClassHierarchy cha) {
    ReflectionSummary summary = summaries.get(method);
    if (summary != null) {
      return summary.getTypeForNewInstance(bcIndex, cha);
    } else {
      return null;
    }
  }

  public ReflectionSummary getSummary(MemberReference m) {
    return summaries.get(m);
  }
}
