/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.j2ee;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * Method representing a factory that creates ActionForm objects. We extend {@link SummarizedMethod} to allow for re-use
 * of the machinery in FactoryBypassInterpreter.
 * 
 * @author manu
 * 
 */
public class ActionFormFactoryMethod extends SummarizedMethod {

  public static final Atom name = Atom.findOrCreateUnicodeAtom("makeActionForm");

  public static final Descriptor descr = Descriptor.findOrCreateUTF8("()" + StrutsEntrypoints.actionFormName + ";");

  public static final TypeReference factoryClassRef = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TypeName
      .string2TypeName("Lcom/ibm/wala/FakeActionFormFactoryClass"));

  public static final MethodReference ref = MethodReference.findOrCreate(factoryClassRef, name, descr);

  public ActionFormFactoryMethod(IClassHierarchy cha) {
    super(ref, makeNoOpFactorySummary(), new FakeActionFormFactoryClass(cha));
  }

  private static MethodSummary makeNoOpFactorySummary() {
    MethodSummary noOpSummary = BypassMethodTargetSelector.generateStandardNoOp(Language.JAVA, ref, true);
    noOpSummary.setFactory(true);
    return noOpSummary;
  }

  private static class FakeActionFormFactoryClass extends SyntheticClass {

    public FakeActionFormFactoryClass(IClassHierarchy cha) {
      super(factoryClassRef, cha);
    }

    public Collection<IField> getAllFields() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    public Collection<IClass> getAllImplementedInterfaces() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    public Collection<IField> getAllInstanceFields() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    public Collection<IMethod> getAllMethods() {
      // TODO Auto-generated method stub
      assert false;
      return null;
    }

    public Collection<IField> getAllStaticFields() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    public IMethod getClassInitializer() {
      return null;
    }

    public Collection<IField> getDeclaredInstanceFields() {
      return Collections.emptySet();
    }

    public Collection<IMethod> getDeclaredMethods() {
      // TODO Auto-generated method stub
      assert false;
      return null;
    }

    public Collection<IField> getDeclaredStaticFields() {
      return Collections.emptySet();
    }

    public Collection<IClass> getDirectInterfaces() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    public IField getField(Atom name) {
      // TODO Auto-generated method stub
      assert false;
      return null;
    }

    public IMethod getMethod(Selector selector) {
      // TODO Auto-generated method stub
      assert false;
      return null;
    }

    public int getModifiers() {
      // TODO Auto-generated method stub
      assert false;
      return 0;
    }

    public TypeName getName() {
      return factoryClassRef.getName();
    }

    public IClass getSuperclass()  {
      return null;
    }

    public boolean isPublic() {
      // TODO Auto-generated method stub
      assert false;
      return false;
    }

    public boolean isReferenceType() {
      // TODO Auto-generated method stub
      assert false;
      return false;
    }
    
    @Override
    public int hashCode() {
      return super.hashCode(); // like a singleton
    }
    
    @Override
    public boolean equals(Object o) {
      return super.equals(o);
    }
    
  }

}
