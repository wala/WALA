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

import static com.ibm.wala.types.TypeName.ArrayMask;
import static com.ibm.wala.types.TypeName.ElementBits;
import static com.ibm.wala.types.TypeName.PrimitiveMask;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warning;

/**
 * This class reads method summaries from an XML Stream.
 */
public class XMLMethodSummaryReader implements BytecodeConstants {

  static final boolean DEBUG = false;

  /**
   * Governing analysis scope
   */
  final private AnalysisScope scope;

  /**
   * Method summaries collected for methods
   */
  final private HashMap<MethodReference, MethodSummary> summaries = HashMapFactory.make();

  /**
   * Set of TypeReferences that are marked as "allocatable"
   */
  final private HashSet<TypeReference> allocatable = HashSetFactory.make();

  /**
   * Set of Atoms that represent packages that can be ignored
   */
  final private HashSet<Atom> ignoredPackages = HashSetFactory.make();

  //
  // Define XML element names
  //
  private final static int E_CLASSLOADER = 0;

  private final static int E_METHOD = 1;

  private final static int E_CLASS = 2;

  private final static int E_PACKAGE = 3;

  private final static int E_CALL = 4;

  private final static int E_NEW = 5;

  private final static int E_POISON = 6;

  private final static int E_SUMMARY_SPEC = 7;

  private final static int E_RETURN = 8;

  private final static int E_PUTSTATIC = 9;

  private final static int E_AASTORE = 10;

  private final static int E_PUTFIELD = 11;

  private final static int E_GETFIELD = 12;

  private final static int E_ATHROW = 13;

  private final static int E_CONSTANT = 14;

  private final static int E_AALOAD = 15;

  private final static Map<String, Integer> elementMap = HashMapFactory.make(14);
  static {
    elementMap.put("classloader", new Integer(E_CLASSLOADER));
    elementMap.put("method", new Integer(E_METHOD));
    elementMap.put("class", new Integer(E_CLASS));
    elementMap.put("package", new Integer(E_PACKAGE));
    elementMap.put("call", new Integer(E_CALL));
    elementMap.put("new", new Integer(E_NEW));
    elementMap.put("poison", new Integer(E_POISON));
    elementMap.put("summary-spec", new Integer(E_SUMMARY_SPEC));
    elementMap.put("return", new Integer(E_RETURN));
    elementMap.put("putstatic", new Integer(E_PUTSTATIC));
    elementMap.put("aastore", new Integer(E_AASTORE));
    elementMap.put("putfield", new Integer(E_PUTFIELD));
    elementMap.put("getfield", new Integer(E_GETFIELD));
    elementMap.put("throw", new Integer(E_ATHROW));
    elementMap.put("constant", new Integer(E_CONSTANT));
    elementMap.put("aaload", new Integer(E_AALOAD));
  }

  //
  // Define XML attribute names
  //
  private final static String A_NAME = "name";

  private final static String A_TYPE = "type";

  private final static String A_CLASS = "class";

  private final static String A_SIZE = "size";

  private final static String A_DESCRIPTOR = "descriptor";

  private final static String A_REASON = "reason";

  private final static String A_LEVEL = "level";

  private final static String A_WILDCARD = "*";

  private final static String A_DEF = "def";

  private final static String A_STATIC = "static";

  private final static String A_VALUE = "value";

  private final static String A_FIELD = "field";

  private final static String A_FIELD_TYPE = "fieldType";

  private final static String A_ARG = "arg";

  private final static String A_ALLOCATABLE = "allocatable";

  private final static String A_REF = "ref";

  private final static String A_INDEX = "index";

  private final static String A_IGNORE = "ignore";

  private final static String A_FACTORY = "factory";

  private final static String A_NUM_ARGS = "numArgs";

  private final static String V_NULL = "null";

  private final static String V_TRUE = "true";

  public XMLMethodSummaryReader(InputStream xmlFile, AnalysisScope scope) {
    super();
    if (xmlFile == null) {
      throw new IllegalArgumentException("null xmlFile");
    }
    if (scope == null) {
      throw new IllegalArgumentException("null scope");
    }
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

    assert xml != null : "Null xml stream";
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.newSAXParser().parse(new InputSource(xml), handler);
  }

  /**
   * @return Method summaries collected for methods. Mapping Object -&gt; MethodSummary where Object is either a
   *         <ul>
   *         <li>MethodReference
   *         <li>TypeReference
   *         <li>Atom (package name)
   *         </ul>
   */
  public Map<MethodReference, MethodSummary> getSummaries() {
    return summaries;
  }

  /**
   * @return Set of TypeReferences marked "allocatable"
   */
  public Set<TypeReference> getAllocatableClasses() {
    return allocatable;
  }

  /**
   * @return Set of Atoms representing ignorable packages
   */
  public Set<Atom> getIgnoredPackages() {
    return ignoredPackages;
  }

  /**
   * @author sfink
   * 
   *         SAX parser logic for XML method summaries
   */
  private class SAXHandler extends DefaultHandler {
    /**
     * The class loader reference for the element being processed
     */
    private ClassLoaderReference governingLoader = null;

    /**
     * The method summary for the element being processed
     */
    private MethodSummary governingMethod = null;

    /**
     * The declaring class for the element begin processed
     */
    private TypeReference governingClass = null;

    /**
     * The package for the element being processed
     */
    private Atom governingPackage = null;

    /**
     * The next available local number for the method being processed
     */
    private int nextLocal = -1;

    /**
     * A mapping from String (variable name) -&gt; Integer (local number)
     */
    private Map<String, Integer> symbolTable = null;

    /*
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
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
        String mname = atts.getValue(A_NAME);
        if (mname.equals(A_WILDCARD)) {
          Assertions.UNREACHABLE("Wildcards not currently implemented.");
        } else {
          startMethod(atts);
        }
        break;
      case E_CLASS:
        String cname = atts.getValue(A_NAME);
        if (cname.equals(A_WILDCARD)) {
          Assertions.UNREACHABLE("Wildcards not currently implemented");
        } else {
          startClass(cname, atts);
        }
        break;
      case E_PACKAGE:
        governingPackage = Atom.findOrCreateUnicodeAtom(atts.getValue(A_NAME));
        String ignore = atts.getValue(A_IGNORE);
        if (ignore != null && ignore.equals(V_TRUE)) {
          ignoredPackages.add(governingPackage);
        }
        break;
      case E_CALL:
        processCallSite(atts);
        break;
      case E_NEW:
        processAllocation(atts);
        break;
      case E_PUTSTATIC:
        processPutStatic(atts);
        break;
      case E_PUTFIELD:
        processPutField(atts);
        break;
      case E_GETFIELD:
        processGetField(atts);
        break;
      case E_ATHROW:
        processAthrow(atts);
        break;
      case E_AASTORE:
        processAastore(atts);
        break;
      case E_AALOAD:
        processAaload(atts);
        break;
      case E_RETURN:
        processReturn(atts);
        break;
      case E_POISON:
        processPoison(atts);
        break;
      case E_CONSTANT:
        processConstant(atts);
        break;
      case E_SUMMARY_SPEC:
        break;
      default:
        Assertions.UNREACHABLE("Unexpected element: " + name);
        break;
      }
    }

    private void startClass(String cname, Attributes atts) {
      String clName = "L" + governingPackage + "/" + cname;
      governingClass = className2Ref(clName);
      String allocString = atts.getValue(A_ALLOCATABLE);
      if (allocString != null) {
        Assertions.productionAssertion(allocString.equals("true"));
        allocatable.add(governingClass);
      }
    }

    /*
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
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
        if (governingMethod != null) {
          checkReturnValue(governingMethod);
        }
        governingMethod = null;
        symbolTable = null;
        break;
      case E_CLASS:
        governingClass = null;
        break;
      case E_PACKAGE:
        governingPackage = null;
        break;
      case E_CALL:
      case E_GETFIELD:
      case E_NEW:
      case E_POISON:
      case E_PUTSTATIC:
      case E_PUTFIELD:
      case E_AALOAD:
      case E_AASTORE:
      case E_ATHROW:
      case E_SUMMARY_SPEC:
      case E_RETURN:
      case E_CONSTANT:
        break;
      default:
        Assertions.UNREACHABLE("Unexpected element: " + name);
        break;
      }
    }

    /**
     * If a method is declared to return a value, be sure the method summary includes a return statement. Throw an assertion if not.
     * 
     * @param governingMethod
     */
    private void checkReturnValue(MethodSummary governingMethod) {
      Assertions.productionAssertion(governingMethod != null);
      Assertions.productionAssertion(governingMethod.getReturnType() != null);
      if (governingMethod.getReturnType().isReferenceType()) {
        SSAInstruction[] statements = governingMethod.getStatements();
        for (SSAInstruction statement : statements) {
          if (statement instanceof SSAReturnInstruction) {
            return;
          }
        }
        Assertions.UNREACHABLE("Method summary " + governingMethod + " must have a return statement.");
      }

    }

    /**
     * Process an element indicating a call instruction
     * 
     * @param atts
     */
    private void processCallSite(Attributes atts) {
      String typeString = atts.getValue(A_TYPE);
      String nameString = atts.getValue(A_NAME);
      String classString = atts.getValue(A_CLASS);
      String descString = atts.getValue(A_DESCRIPTOR);
      TypeReference type = TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(classString));
      Atom nm = Atom.findOrCreateAsciiAtom(nameString);
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();
      Descriptor D = Descriptor.findOrCreateUTF8(lang, descString);
      MethodReference ref = MethodReference.findOrCreate(type, nm, D);
      CallSiteReference site = null;
      int nParams = ref.getNumberOfParameters();
      if (typeString.equals("virtual")) {
        site = CallSiteReference.make(governingMethod.getNextProgramCounter(), ref, IInvokeInstruction.Dispatch.VIRTUAL);
        nParams++;
      } else if (typeString.equals("special")) {
        site = CallSiteReference.make(governingMethod.getNextProgramCounter(), ref, IInvokeInstruction.Dispatch.SPECIAL);
        nParams++;
      } else if (typeString.equals("interface")) {
        site = CallSiteReference.make(governingMethod.getNextProgramCounter(), ref, IInvokeInstruction.Dispatch.INTERFACE);
        nParams++;
      } else if (typeString.equals("static")) {
        site = CallSiteReference.make(governingMethod.getNextProgramCounter(), ref, IInvokeInstruction.Dispatch.STATIC);
      } else {
        Assertions.UNREACHABLE("Invalid call type " + typeString);
      }
      int[] params = new int[nParams];

      for (int i = 0; i < params.length; i++) {
        String argString = atts.getValue(A_ARG + i);
        Assertions.productionAssertion(argString != null, "unspecified arg in method " + governingMethod + " " + site);
        Integer valueNumber = symbolTable.get(argString);
        if (valueNumber == null) {
          Assertions.UNREACHABLE("Cannot lookup value: " + argString);
        }
        params[i] = valueNumber.intValue();
      }

      // allocate local for exceptions
      int exceptionValue = nextLocal++;

      // register the local variable defined by this call, if appropriate
      String defVar = atts.getValue(A_DEF);
      if (defVar != null) {

        if (symbolTable.keySet().contains(defVar)) {
          Assertions.UNREACHABLE("Cannot def variable twice: " + defVar + " in " + governingMethod);
        }

        int defNum = nextLocal;
        symbolTable.put(defVar, new Integer(nextLocal++));

        governingMethod.addStatement(insts.InvokeInstruction(governingMethod.getNumberOfStatements(), defNum, params, exceptionValue, site, null));
      } else {
        // ignore return value, if any
        governingMethod.addStatement(insts.InvokeInstruction(governingMethod.getNumberOfStatements(), params, exceptionValue, site, null));
      }
    }

    /**
     * Process an element indicating a new allocation site.
     * 
     * @param atts
     */
    private void processAllocation(Attributes atts) {
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();

      // deduce the concrete type allocated
      String classString = atts.getValue(A_CLASS);
      final TypeReference type = TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(classString));

      // register the local variable defined by this allocation
      String defVar = atts.getValue(A_DEF);
      if (symbolTable.keySet().contains(defVar)) {
        Assertions.UNREACHABLE("Cannot def variable twice: " + defVar + " in " + governingMethod);
      }
      if (defVar == null) {
        // the method summary ignores the def'ed variable.
        // just allocate a temporary
        defVar = "L" + nextLocal;
      }
      int defNum = nextLocal;
      symbolTable.put(defVar, new Integer(nextLocal++));

      // create the allocation statement and add it to the method summary
      NewSiteReference ref = NewSiteReference.make(governingMethod.getNextProgramCounter(), type);

      SSANewInstruction a = null;
      if (type.isArrayType()) {
        String size = atts.getValue(A_SIZE);
        Assertions.productionAssertion(size != null);
        Integer sNumber = symbolTable.get(size);
        Assertions.productionAssertion(sNumber != null);
        Assertions.productionAssertion(
            // array of objects
            type.getDerivedMask()==ArrayMask || 
            // array of primitives
            type.getDerivedMask()==((ArrayMask<<ElementBits)|PrimitiveMask));  
        a = insts.NewInstruction(governingMethod.getNumberOfStatements(), defNum, ref, new int[] { sNumber.intValue() });
      } else {
        a = insts.NewInstruction(governingMethod.getNumberOfStatements(), defNum, ref);
      }
      governingMethod.addStatement(a);
    }

    /**
     * Process an element indicating an Athrow
     * 
     * @param atts
     */
    private void processAthrow(Attributes atts) {
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();

      // get the value thrown
      String V = atts.getValue(A_VALUE);
      if (V == null) {
        Assertions.UNREACHABLE("Must specify value for putfield " + governingMethod);
      }
      Integer valueNumber = symbolTable.get(V);
      if (valueNumber == null) {
        Assertions.UNREACHABLE("Cannot lookup value: " + V);
      }

      SSAThrowInstruction T = insts.ThrowInstruction(governingMethod.getNumberOfStatements(), valueNumber.intValue());
      governingMethod.addStatement(T);
    }

    /**
     * Process an element indicating a putfield.
     * 
     * @param atts
     */
    private void processGetField(Attributes atts) {
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();

      // deduce the field written
      String classString = atts.getValue(A_CLASS);
      TypeReference type = TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(classString));

      String fieldString = atts.getValue(A_FIELD);
      Atom fieldName = Atom.findOrCreateAsciiAtom(fieldString);

      String ftString = atts.getValue(A_FIELD_TYPE);
      TypeReference fieldType = TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(ftString));

      FieldReference field = FieldReference.findOrCreate(type, fieldName, fieldType);

      // get the value def'fed
      String defVar = atts.getValue(A_DEF);
      if (symbolTable.keySet().contains(defVar)) {
        Assertions.UNREACHABLE("Cannot def variable twice: " + defVar + " in " + governingMethod);
      }
      if (defVar == null) {
        Assertions.UNREACHABLE("Must specify def for getfield " + governingMethod);
      }
      int defNum = nextLocal;
      symbolTable.put(defVar, new Integer(nextLocal++));

      // get the ref read from
      String R = atts.getValue(A_REF);
      if (R == null) {
        Assertions.UNREACHABLE("Must specify ref for getfield " + governingMethod);
      }
      Integer refNumber = symbolTable.get(R);
      if (refNumber == null) {
        Assertions.UNREACHABLE("Cannot lookup ref: " + R);
      }

      SSAGetInstruction G = insts.GetInstruction(governingMethod.getNumberOfStatements(), defNum, refNumber.intValue(), field);
      governingMethod.addStatement(G);
    }

    /**
     * Process an element indicating a putfield.
     * 
     * @param atts
     */
    private void processPutField(Attributes atts) {
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();

      // deduce the field written
      String classString = atts.getValue(A_CLASS);
      TypeReference type = TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(classString));

      String fieldString = atts.getValue(A_FIELD);
      Atom fieldName = Atom.findOrCreateAsciiAtom(fieldString);

      String ftString = atts.getValue(A_FIELD_TYPE);
      TypeReference fieldType = TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(ftString));

      FieldReference field = FieldReference.findOrCreate(type, fieldName, fieldType);

      // get the value stored
      String V = atts.getValue(A_VALUE);
      if (V == null) {
        Assertions.UNREACHABLE("Must specify value for putfield " + governingMethod);
      }
      Integer valueNumber = symbolTable.get(V);
      if (valueNumber == null) {
        Assertions.UNREACHABLE("Cannot lookup value: " + V);
      }

      // get the ref stored to
      String R = atts.getValue(A_REF);
      if (R == null) {
        Assertions.UNREACHABLE("Must specify ref for putfield " + governingMethod);
      }
      Integer refNumber = symbolTable.get(R);
      if (refNumber == null) {
        Assertions.UNREACHABLE("Cannot lookup ref: " + R);
      }

      SSAPutInstruction P = insts.PutInstruction(governingMethod.getNumberOfStatements(), refNumber.intValue(), valueNumber.intValue(), field);
      governingMethod.addStatement(P);
    }

    /**
     * Process an element indicating a putstatic.
     * 
     * @param atts
     */
    private void processPutStatic(Attributes atts) {
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();

      // deduce the field written
      String classString = atts.getValue(A_CLASS);
      TypeReference type = TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(classString));

      String fieldString = atts.getValue(A_FIELD);
      Atom fieldName = Atom.findOrCreateAsciiAtom(fieldString);

      String ftString = atts.getValue(A_FIELD_TYPE);
      TypeReference fieldType = TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(ftString));

      FieldReference field = FieldReference.findOrCreate(type, fieldName, fieldType);

      // get the value stored
      String V = atts.getValue(A_VALUE);
      if (V == null) {
        Assertions.UNREACHABLE("Must specify value for putstatic " + governingMethod);
      }
      Integer valueNumber = symbolTable.get(V);
      if (valueNumber == null) {
        Assertions.UNREACHABLE("Cannot lookup value: " + V);
      }
      SSAPutInstruction P = insts.PutInstruction(governingMethod.getNumberOfStatements(), valueNumber.intValue(), field);
      governingMethod.addStatement(P);
    }

    /**
     * Process an element indicating an Aastore
     * 
     * @param atts
     */
    private void processAastore(Attributes atts) {
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();

      String R = atts.getValue(A_REF);
      if (R == null) {
        Assertions.UNREACHABLE("Must specify ref for aastore " + governingMethod);
      }
      Integer refNumber = symbolTable.get(R);
      if (refNumber == null) {
        Assertions.UNREACHABLE("Cannot lookup value: " + R);
      }
      // N.B: we currently ignore the index
      String I = atts.getValue(A_INDEX);
      if (I == null) {
        Assertions.UNREACHABLE("Must specify index for aastore " + governingMethod);
      }
      String V = atts.getValue(A_VALUE);
      if (V == null) {
        Assertions.UNREACHABLE("Must specify value for aastore " + governingMethod);
      }
      /** BEGIN Custom change: expect type information in array-store instructions */
      String strType = atts.getValue(A_TYPE);
      TypeReference type;
      if (strType == null) {
        type = TypeReference.JavaLangObject;
      } else {
        type = TypeReference.findOrCreate(governingLoader, strType);
      }
      /** END Custom change: get type information in array-store instructions */
      Integer valueNumber = symbolTable.get(V);
      if (valueNumber == null) {
        Assertions.UNREACHABLE("Cannot lookup value: " + V);
      }
      /** BEGIN Custom change: expect type information in array-store instructions */
      SSAArrayStoreInstruction S = insts.ArrayStoreInstruction(governingMethod.getNumberOfStatements(), refNumber.intValue(), 0, valueNumber.intValue(), type);
      /** END Custom change: get type information in array-store instructions */
      governingMethod.addStatement(S);
    }

    /**
     * Process an element indicating an Aaload
     * 
     * @param atts
     */
    private void processAaload(Attributes atts) {
      //<aaload def="foo" ref="arg1" index="the-answer" />
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();

      String R = atts.getValue(A_REF);
      if (R == null) {
        Assertions.UNREACHABLE("Must specify ref for aaload " + governingMethod);
      }
      Integer refNumber = symbolTable.get(R);
      if (refNumber == null) {
        Assertions.UNREACHABLE("Cannot lookup value: " + R);
      }
      // N.B: we currently ignore the index
      String I = atts.getValue(A_INDEX);
      if (I == null) {
        Assertions.UNREACHABLE("Must specify index for aaload " + governingMethod);
      }
      String strType = atts.getValue(A_TYPE);
      TypeReference type;
      if (strType == null) {
        type = TypeReference.JavaLangObject;
      } else {
        type = TypeReference.findOrCreate(governingLoader, strType);
      }
   // get the value def'fed
      String defVar = atts.getValue(A_DEF);
      if (symbolTable.keySet().contains(defVar)) {
        Assertions.UNREACHABLE("Cannot def variable twice: " + defVar + " in " + governingMethod);
      }
      if (defVar == null) {
        Assertions.UNREACHABLE("Must specify def for getfield " + governingMethod);
      }
      int defNum = nextLocal;
      symbolTable.put(defVar, new Integer(nextLocal++));
      SSAArrayLoadInstruction S = insts.ArrayLoadInstruction(governingMethod.getNumberOfStatements(), defNum, refNumber.intValue(), 0,
          type);
      governingMethod.addStatement(S);
    }

    /**
     * Process an element indicating a return statement.
     * 
     * @param atts
     */
    private void processReturn(Attributes atts) {
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      SSAInstructionFactory insts = lang.instructionFactory();

      if (governingMethod.getReturnType() != null) {
        String retV = atts.getValue(A_VALUE);
        if (retV == null) {
          SSAReturnInstruction R = insts.ReturnInstruction(governingMethod.getNumberOfStatements());
          governingMethod.addStatement(R);
        } else {
          Integer valueNumber = symbolTable.get(retV);
          if (valueNumber == null) {
            if (!retV.equals(V_NULL)) {
              Assertions.UNREACHABLE("Cannot return value with no def: " + retV);
            } else {
              valueNumber = symbolTable.get(V_NULL);
              if (valueNumber == null) {
                valueNumber = new Integer(nextLocal++);
                symbolTable.put(V_NULL, valueNumber);
              }
            }
          }
          boolean isPrimitive = governingMethod.getReturnType().isPrimitiveType();
          SSAReturnInstruction R = insts.ReturnInstruction(governingMethod.getNumberOfStatements(), valueNumber.intValue(), isPrimitive);
          governingMethod.addStatement(R);
        }
      }
    }

    /**
     * @param atts
     */
    private void processConstant(Attributes atts) {
      String var = atts.getValue(A_NAME);
      if (var == null)
        Assertions.UNREACHABLE("Must give name for constant");
      Integer valueNumber = new Integer(nextLocal++);
      symbolTable.put(var, valueNumber);

      String typeString = atts.getValue(A_TYPE);
      String valueString = atts.getValue(A_VALUE);

      governingMethod.addConstant(valueNumber, (typeString.equals("int")) ? new ConstantValue(new Integer(valueString))
          : (typeString.equals("long")) ? new ConstantValue(new Long(valueString))
              : (typeString.equals("short")) ? new ConstantValue(new Short(valueString))
                  : (typeString.equals("float")) ? new ConstantValue(new Float(valueString))
                      : (typeString.equals("double")) ? new ConstantValue(new Double(valueString)) : null);
    }

    /**
     * Process an element which indicates this method is "poison"
     * 
     * @param atts
     */
    private void processPoison(Attributes atts) {
      String reason = atts.getValue(A_REASON);
      governingMethod.addPoison(reason);
      String level = atts.getValue(A_LEVEL);
      if (level.equals("severe")) {
        governingMethod.setPoisonLevel(Warning.SEVERE);
      } else if (level.equals("moderate")) {
        governingMethod.setPoisonLevel(Warning.MODERATE);
      } else if (level.equals("mild")) {
        governingMethod.setPoisonLevel(Warning.MILD);
      } else {
        Assertions.UNREACHABLE("Unexpected level: " + level);
      }
    }

    /**
     * Begin processing of a method. 1. Set the governing method. 2. Initialize the nextLocal variable
     * 
     * @param atts
     */
    private void startMethod(Attributes atts) {

      String methodName = atts.getValue(A_NAME);

      Atom mName = Atom.findOrCreateUnicodeAtom(methodName);
      String descString = atts.getValue(A_DESCRIPTOR);
      Language lang = scope.getLanguage(governingLoader.getLanguage());
      Descriptor D = Descriptor.findOrCreateUTF8(lang, descString);

      MethodReference ref = MethodReference.findOrCreate(governingClass, mName, D);
      governingMethod = new MethodSummary(ref);

      if (DEBUG) {
        System.err.println(("Register method summary: " + ref));
      }
      summaries.put(ref, governingMethod);

      boolean isStatic = false;
      String staticString = atts.getValue(A_STATIC);
      if (staticString != null) {
        if (staticString.equals("true")) {
          isStatic = true;
          governingMethod.setStatic(true);
        } else if (staticString.equals("false")) {
          isStatic = false;
          governingMethod.setStatic(false);
        } else {
          Assertions.UNREACHABLE("Invalid attribute value " + A_STATIC + ": " + staticString);
        }
      }

      String factoryString = atts.getValue(A_FACTORY);
      if (factoryString != null) {
        if (factoryString.equals("true")) {
          governingMethod.setFactory(true);
        } else if (factoryString.equals("false")) {
          governingMethod.setFactory(false);
        } else {
          Assertions.UNREACHABLE("Invalid attribute value " + A_FACTORY + ": " + factoryString);
        }
      }

      // This is somewhat gross, but it is to deal with the fact that
      // some non-Java languages have notions of arguments that do not
      // map nicely to descriptors.
      int nParams;
      String specifiedArgs = atts.getValue(A_NUM_ARGS);
      if (specifiedArgs == null) {
        nParams = ref.getNumberOfParameters();
        if (!isStatic) {
          nParams += 1;
        }
      } else {
        nParams = Integer.parseInt(specifiedArgs);
      }

      // note that symbol tables reserve v0 for "unknown", so v1 gets assigned
      // to the first parameter "arg0", and so forth.
      nextLocal = nParams + 1;
      symbolTable = HashMapFactory.make(5);
      // create symbols for the parameters
      for (int i = 0; i < nParams; i++) {
        symbolTable.put("arg" + i, new Integer(i + 1));
      }
    }

    /**
     * Method classLoaderName2Ref.
     * 
     * @param clName
     * @return ClassLoaderReference
     */
    private ClassLoaderReference classLoaderName2Ref(String clName) {
      return scope.getLoader(Atom.findOrCreateUnicodeAtom(clName));
    }

    /**
     * Method classLoaderName2Ref.
     * 
     * @param clName
     * @return ClassLoaderReference
     */
    private TypeReference className2Ref(String clName) {
      return TypeReference.findOrCreate(governingLoader, TypeName.string2TypeName(clName));
    }
  }

}
