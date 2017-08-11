/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * WALA JDT Frontend is Copyright (c) 2008 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.cast.java.translator.jdt;

import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * Class responsible for mapping JDT type system objects representing types, methods and fields to the corresponding WALA
 * TypeReferences, MethodReferences and FieldReferences. Used during translation and by clients to help correlate WALA analysis
 * results to the various AST nodes.
 * 
 * In English: keeps a hashtable of WALA "type references", "field references", etc. which describe types, fields, etc. Creates
 * these from their JDT equivalents and keeps the hashtable linking the two representations.
 * 
 * @author rfuhrer
 */
public class JDTIdentityMapper {
  private final Map<String, TypeReference> fTypeMap = HashMapFactory.make();

  private final Map<String, FieldReference> fFieldMap = HashMapFactory.make();

  private final Map<String, MethodReference> fMethodMap = HashMapFactory.make();

  private final ClassLoaderReference fClassLoaderRef; // TAGALONG

  private final AST fAst;

  public JDTIdentityMapper(ClassLoaderReference clr, AST ast) {
    fClassLoaderRef = clr;
    fAst = ast;
  }

  // TYPES

  /**
   * Create (or reuse) a TypeReference for the given JDT Type Binding.<br>
   * This method canonicalizes the TypeReferences
   */
  public TypeReference getTypeRef(ITypeBinding type) {
    type = JDT2CAstUtils.getErasedType(type, fAst); // GENERICS: erasure...

    if (!fTypeMap.containsKey(type.getKey())) {
      TypeName typeName = TypeName.string2TypeName(typeToTypeID(type));
      TypeReference ref = TypeReference.findOrCreate(fClassLoaderRef, typeName);

      fTypeMap.put(type.getKey(), ref);
      return ref;
    }
    return fTypeMap.get(type.getKey());
  }

  /**
   * Translates the given Polyglot type to a name suitable for use in a DOMO TypeReference (i.e. a bytecode-compliant type name).
   */
  public String typeToTypeID(ITypeBinding type) {
    if (type.isPrimitive())
      return type.getBinaryName();
    else if (type.isArray())
      // arrays' binary names in JDT are like "[Ljava.lang.String;"
      return type.getBinaryName().replace('.', '/').replace(";", "");
    else if (type.isLocal() || type.isAnonymous())
      return anonLocalTypeToTypeID(type);
    else if (type.isClass() || type.isEnum() || type.isInterface()) // in polyglot interfaces are classes too. not in JDT
      // class binary names in JDT are like "java.lang.String"
      return 'L' + type.getBinaryName().replace('.', '/'); // TODO:
    else if (type.isTypeVariable()) {
      return typeToTypeID(JDT2CAstUtils.getTypesVariablesBase(type, fAst));
    }
    Assertions.UNREACHABLE("typeToTypeID() encountered the type " + type + " that is neither primitive, array, nor class!");
    return null;
  }

  public String anonLocalTypeToTypeID(ITypeBinding type) {
    String outerTypeID = typeToTypeID(type.getDeclaringClass());

    String metSelectorName;
    IMethodBinding metBinding = type.getDeclaringMethod();
    if (metBinding == null) // anonymous class declared in initializer or static initializer (rare case...)
      metSelectorName = "<init>";
    else
      metSelectorName = getMethodRef(metBinding).getSelector().toString();

    String shortName = (type.isAnonymous()) ? JDT2CAstUtils.anonTypeName(type) : type.getName();

    return outerTypeID + '/' + metSelectorName + '/' + shortName;
  }

  // FIELDS

  public FieldReference getFieldRef(IVariableBinding field) {
    if (!fFieldMap.containsKey(field.getKey())) {
      // create one
      ITypeBinding targetType = field.getDeclaringClass();
      TypeReference targetTypeRef = TypeReference.findOrCreate(fClassLoaderRef, typeToTypeID(targetType));
      ITypeBinding fieldType = field.getType();
      TypeReference fieldTypeRef = TypeReference.findOrCreate(fClassLoaderRef, typeToTypeID(fieldType));
      Atom fieldName = Atom.findOrCreateUnicodeAtom(field.getName());
      FieldReference ref = FieldReference.findOrCreate(targetTypeRef, fieldName, fieldTypeRef);

      fFieldMap.put(field.getKey(), ref);
      return ref;
    }
    return fFieldMap.get(field.getKey());
  }

  public MethodReference fakeMethodRefNoArgs(String key, String typeID, String metName, String returnTypeID) {
    if (!fMethodMap.containsKey(key)) {
      // create one
      TypeName ownerType = TypeName.string2TypeName(typeID);
      TypeReference ownerTypeRef = TypeReference.findOrCreate(fClassLoaderRef, ownerType);

      // FAKE SELECTOR
      Atom name = Atom.findOrCreateUnicodeAtom(metName);
      TypeName[] argTypeNames = null;
      TypeName retTypeName = TypeName.string2TypeName(returnTypeID);
      Descriptor desc = Descriptor.findOrCreate(argTypeNames, retTypeName);
      Selector selector = new Selector(name, desc);

      MethodReference ref = MethodReference.findOrCreate(ownerTypeRef, selector);

      fMethodMap.put(key, ref);
      return ref;
    }
    return fMethodMap.get(key);
  }

  // METHODS
  public MethodReference getMethodRef(IMethodBinding met) {
    if (!fMethodMap.containsKey(met.getKey())) {
      // create one
      TypeName ownerType = TypeName.string2TypeName(typeToTypeID(met.getDeclaringClass()));
      TypeReference ownerTypeRef = TypeReference.findOrCreate(fClassLoaderRef, ownerType);
      MethodReference ref = MethodReference.findOrCreate(ownerTypeRef, selectorForMethod(met));

      fMethodMap.put(met.getKey(), ref);
      return ref;
    }
    return fMethodMap.get(met.getKey());
  }

  private Selector selectorForMethod(IMethodBinding met) {
    // TODO: have to handle default constructors?
    // TODO: generics...
    Atom name = (met.isConstructor()) ? MethodReference.initAtom : Atom.findOrCreateUnicodeAtom(met.getName());

    TypeName[] argTypeNames = null;
    ITypeBinding[] formalTypes = met.getParameterTypes();

    int length = formalTypes.length;

    // ENUMS: hidden name and ID in constructor
    if (met.isConstructor() && met.getDeclaringClass().isEnum())
      length += 2;

    // Descriptor prefers null to an empty array
    if (length > 0) {
      argTypeNames = new TypeName[length];
      int i = 0;
      if (met.isConstructor() && met.getDeclaringClass().isEnum()) {
        argTypeNames[0] = TypeName.string2TypeName(typeToTypeID(fAst.resolveWellKnownType("java.lang.String")));
        argTypeNames[1] = TypeName.string2TypeName(typeToTypeID(fAst.resolveWellKnownType("int")));
        i = 2;
      }
      for (ITypeBinding argType : formalTypes)
        argTypeNames[i++] = TypeName.string2TypeName(typeToTypeID(argType));
    }

    TypeName retTypeName = TypeName.string2TypeName(typeToTypeID(met.getReturnType()));
    Descriptor desc = Descriptor.findOrCreate(argTypeNames, retTypeName);

    return new Selector(name, desc);
  }
}
