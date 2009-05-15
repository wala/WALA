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
package com.ibm.wala.j2ee;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.FieldImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * A synthetic class that models aspects of the J2EE Container.
 */
class J2EEContainerModel extends SyntheticClass implements BytecodeConstants, EJBConstants {

  private final static Atom syntheticLoaderName = Atom.findOrCreateUnicodeAtom("Synthetic");

  private final static ClassLoaderReference syntheticLoaderRef = new ClassLoaderReference(syntheticLoaderName, ClassLoaderReference.Java, ClassLoaderReference.Application);

  public static final TypeName containerModelName = TypeName.string2TypeName("L$ContainerModel");

  public static final TypeName entityBeanName = TypeName.string2TypeName("Ljavax/ejb/EntityBean");

  public static final TypeName entityContextName = TypeName.string2TypeName("Ljavax/ejb/EntityContext");

  public static final TypeName sessionBeanName = TypeName.string2TypeName("Ljavax/ejb/SessionBean");

  public static final TypeName sessionContextName = TypeName.string2TypeName("Ljavax/ejb/SessionContext");

  public static final TypeName messageBeanName = TypeName.string2TypeName("Ljavax/ejb/MessageDrivenBean");

  public static final TypeName messageContextName = TypeName.string2TypeName("Ljavax/ejb/MessageDrivenContext");

  public static final TypeReference containerModelRef = TypeReference.findOrCreate(syntheticLoaderRef, containerModelName);

  public static final TypeReference entityBeanRef = TypeReference.findOrCreate(ClassLoaderReference.Extension, entityBeanName);

  public static final TypeReference entityContextRef = TypeReference.findOrCreate(ClassLoaderReference.Extension, entityContextName);

  public static final TypeReference sessionBeanRef = TypeReference.findOrCreate(ClassLoaderReference.Extension, sessionBeanName);

  public static final TypeReference sessionContextRef = TypeReference.findOrCreate(ClassLoaderReference.Extension, sessionContextName);

  public static final TypeReference messageBeanRef = TypeReference.findOrCreate(ClassLoaderReference.Extension, messageBeanName);

  public static final TypeReference messageContextRef = TypeReference.findOrCreate(ClassLoaderReference.Extension, messageContextName);

  public static final Descriptor setEntityContextDescriptor = Descriptor.findOrCreate(new TypeName[] { entityContextName },
      TypeReference.VoidName);

  public static final Descriptor setSessionContextDescriptor = Descriptor.findOrCreate(new TypeName[] { sessionContextName },
      TypeReference.VoidName);

  public static final Descriptor setMessageContextDescriptor = Descriptor.findOrCreate(new TypeName[] { messageContextName },
      TypeReference.VoidName);

  public static final MethodReference setEntityContext = MethodReference.findOrCreate(entityBeanRef, Atom
      .findOrCreateAsciiAtom("setEntityContext"), setEntityContextDescriptor);

  public static final MethodReference setSessionContext = MethodReference.findOrCreate(sessionBeanRef, Atom
      .findOrCreateAsciiAtom("setSessionContext"), setSessionContextDescriptor);

  public static final MethodReference setMessageContext = MethodReference.findOrCreate(messageBeanRef, Atom
      .findOrCreateAsciiAtom("setMessageDrivenContext"), setMessageContextDescriptor);

  /**
   * Governing deployment information
   */
  private final DeploymentMetaData deployment;

  /**
   * Governing class hierarchy
   */
  private final IClassHierarchy cha;

  J2EEContainerModel(DeploymentMetaData deployment, IClassHierarchy cha) {
    super(containerModelRef, cha);
    this.cha = cha;
    this.deployment = deployment;
    initializeStaticFieldRefs();
  }

  public IClassLoader getClassLoader() {
    return cha.getLoader(syntheticLoaderRef);
  }

  public IClass getSuperclass() {
    return cha.lookupClass(TypeReference.JavaLangObject);
  }

  public Collection<IClass> getAllInterfaces() {
    return Collections.emptyList();
  }

  public String getSourceFileName() {
    return "This synthetic J2EE model class has no source source";
  }

  public Collection<IField> getDeclaredInstanceFields() {
    return Collections.emptySet();
  }

  public Collection<IField> getAllInstanceFields() {
    return Collections.emptySet();
  }

  public IMethod getMethod(Selector selector) {
    if ((selector.getName() == MethodReference.clinitName) && (selector.getDescriptor().equals(MethodReference.defaultInitDesc)))
      return getClassInitializer();
    else
      return null;
  }

  public Collection<IMethod> getDeclaredMethods() {
    return Collections.singleton(getClassInitializer());
  }

  public String toString() {
    return "<Synthetic J2EE Container Model>";
  }

  private Collection<FieldReference> staticFieldRefs = null;

  private Map<Atom, IField> staticFieldMap = null;

  /**
   * Create a phony field name which logically holds a pointer to the one true
   * instance of a given bean type
   */
  public static Atom getBeanFieldName(TypeReference beanType) {
    return getBeanFieldName(beanType.getName());
  }

  /**
   * Create a phony field name which logically holds a pointer to the one true
   * instance of a given bean type TODO: reconsider this atom generator ....
   */
  public static Atom getBeanFieldName(TypeName beanTypeName) {
    return Atom.findOrCreateUnicodeAtom("$$existing$" + beanTypeName.toString().replace('/', '$'));
  }

  /**
   * Create a phony field reference which logically holds a pointer to the one
   * true instance of a given bean type
   */
  public static FieldReference getBeanFieldRef(TypeReference ejbType) {
    return FieldReference.findOrCreate(containerModelRef, getBeanFieldName(ejbType), ejbType);
  }

  /**
   * Create a phony field reference which logically holds a pointer to the one
   * true instance of a given bean type
   */
  public static FieldReference getBeanFieldRef(BeanMetaData bean) {
    return getBeanFieldRef(bean.getEJBClass());
  }

  private void initializeStaticFieldRefs() {
    final Iterator<BeanMetaData> entityBeans = deployment.iterateEntities();
    final Iterator<BeanMetaData> sessionBeans = deployment.iterateSessions();
    final Iterator<BeanMetaData> messageBeans = deployment.iterateMDBs();
    Iterator<FieldReference> it = new Iterator<FieldReference>() {
      public void remove() {
        throw new UnsupportedOperationException();
      }

      public boolean hasNext() {
        return entityBeans.hasNext() || sessionBeans.hasNext() || messageBeans.hasNext();
      }

      public FieldReference next() {
        if (entityBeans.hasNext()) {
          return getBeanFieldRef(entityBeans.next());
        } else if (sessionBeans.hasNext()) {
          return getBeanFieldRef(sessionBeans.next());
        } else {
          return getBeanFieldRef(messageBeans.next());
        }
      }
    };
    staticFieldRefs = Iterator2Collection.toSet(it);
    initializeStaticFieldMap();
  }

  private void initializeStaticFieldMap() {
    staticFieldMap = HashMapFactory.make();
    for (Iterator<FieldReference> fs = staticFieldRefs.iterator(); fs.hasNext();) {
      FieldReference f = fs.next();
      staticFieldMap.put(f.getName(), new FieldImpl(this, f, ClassConstants.ACC_STATIC, null));
    }
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredStaticFields()
   */
  public Collection<IField> getDeclaredStaticFields() {
    return staticFieldMap.values();
  }

  public IField getField(Atom name) {
    if (staticFieldMap == null) {
      initializeStaticFieldMap();
    }
    return staticFieldMap.get(name);
  }

  private IMethod clinitMethod = null;

  /**
   * Create a <clinit> method for the synthetic container class
   * 
   * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
   */
  public IMethod getClassInitializer() {
    if (clinitMethod == null) {
      SSAInstructionFactory insts = Language.JAVA.instructionFactory();
      
      MethodReference clinit = MethodReference.findOrCreate(containerModelRef, MethodReference.clinitName,
          MethodReference.defaultInitDesc);
      MethodSummary code = new MethodSummary(clinit);
      code.setStatic(true);
      int nextLocal = code.getNumberOfParameters() + 1;
      Iterator<IField> fields = getDeclaredStaticFields().iterator();
      while (fields.hasNext()) {
        FieldReference field = fields.next().getReference();
        TypeReference ejbType = field.getFieldType();

        // 1. create bean type representing pool objects
        int beanAlloc = nextLocal++;
        code.addStatement(insts.NewInstruction(beanAlloc, NewSiteReference.make(code.getNextProgramCounter(), ejbType)));

        // 2. call set...Context, as required by lifecycle.
        int contextAlloc = nextLocal++;
        int ignoredExceptions = nextLocal++;
        if (deployment.getBeanMetaData(ejbType).isSessionBean()) {
          code.addStatement(insts.NewInstruction(contextAlloc, NewSiteReference.make(code.getNextProgramCounter(),
              sessionContextRef)));
          code.addStatement(insts.InvokeInstruction(new int[] { beanAlloc, contextAlloc }, ignoredExceptions, CallSiteReference
              .make(code.getNextProgramCounter(), setSessionContext, IInvokeInstruction.Dispatch.INTERFACE)));
        } else if (deployment.getBeanMetaData(ejbType).isMessageDrivenBean()) {
          code.addStatement(insts.NewInstruction(contextAlloc, NewSiteReference.make(code.getNextProgramCounter(),
              messageContextRef)));
          code.addStatement(insts.InvokeInstruction(new int[] { beanAlloc, contextAlloc }, ignoredExceptions, CallSiteReference
              .make(code.getNextProgramCounter(), setMessageContext, IInvokeInstruction.Dispatch.INTERFACE)));
          // message driven beans also get ejbCreate called at this point
          // (or that is how I interpret the spec, such as it is)
          int moreIgnoredExceptions = nextLocal++;
          MethodReference ejbCreate = MethodReference.findOrCreate(ejbType, EJB_CREATE, Descriptor.findOrCreate(null,
              TypeReference.VoidName));
          code.addStatement(insts.InvokeInstruction(new int[] { beanAlloc }, moreIgnoredExceptions, CallSiteReference.make(code
              .getNextProgramCounter(), ejbCreate, IInvokeInstruction.Dispatch.VIRTUAL)));
        } else {
          code.addStatement(insts.NewInstruction(contextAlloc, NewSiteReference.make(code.getNextProgramCounter(),
              entityContextRef)));
          code.addStatement(insts.InvokeInstruction(new int[] { beanAlloc, contextAlloc }, ignoredExceptions, CallSiteReference
              .make(code.getNextProgramCounter(), setEntityContext, IInvokeInstruction.Dispatch.INTERFACE)));
        }

        // 3. put bean into static field representing pool.
        code.addStatement(insts.PutInstruction(beanAlloc, field));
      }

      clinitMethod = new SummarizedMethod(clinit, code, this);
    }

    return clinitMethod;
  }

  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object arg0) {
    return arg0.getClass().equals(getClass());
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return 7;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getModifiers()
   */
  public int getModifiers() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return 0;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces()
   */
  public Collection<IClass> getAllImplementedInterfaces() {
    return Collections.emptySet();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllAncestorInterfaces()
   */
  public Collection<IClass> getAllAncestorInterfaces() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getName()
   */
  public TypeName getName() {
    return getReference().getName();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#isReferenceType()
   */
  public boolean isReferenceType() {
    return getReference().isReferenceType();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDirectInterfaces()
   */
  public Collection<IClass> getDirectInterfaces() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  public Collection<IField> getAllStaticFields() {
    return getDeclaredStaticFields();
  }

  public Collection<IField> getAllFields() {
    return getDeclaredStaticFields();
  }

  public Collection<IMethod> getAllMethods() {
    return Collections.singleton(getClassInitializer());
  }

  public boolean isPublic() {
    return false;
  }
  
  public boolean isPrivate() {
    return false;
  }

}
