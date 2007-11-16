/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common.impl;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EPackageImpl;

import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.CommonPackage;
import com.ibm.wala.ecore.common.ECollection;
import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.ecore.common.ENotContainer;
import com.ibm.wala.ecore.common.EObjectWithContainerId;
import com.ibm.wala.ecore.common.EPair;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.common.EStringHolder;
import com.ibm.wala.ecore.graph.GraphPackage;
import com.ibm.wala.ecore.graph.impl.GraphPackageImpl;
import com.ibm.wala.ecore.j2ee.scope.J2EEScopePackage;
import com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl;
import com.ibm.wala.ecore.java.JavaPackage;
import com.ibm.wala.ecore.java.impl.JavaPackageImpl;
import com.ibm.wala.ecore.java.scope.JavaScopePackage;
import com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl;
import com.ibm.wala.ecore.regex.RegexPackage;
import com.ibm.wala.ecore.regex.impl.RegexPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class CommonPackageImpl extends EPackageImpl implements CommonPackage {
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eCollectionEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass ePairEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eRelationEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eContainerEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eNotContainerEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eStringHolderEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eObjectWithContainerIdEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EDataType eJavaCollectionEDataType = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EDataType eFileEDataType = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EDataType eIteratorEDataType = null;

  /**
   * Creates an instance of the model <b>Package</b>, registered with
   * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
   * package URI value.
   * <p>Note: the correct way to create the package is via the static
   * factory method {@link #init init()}, which also performs
   * initialization of the package, or returns the registered package,
   * if one already exists.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.eclipse.emf.ecore.EPackage.Registry
   * @see com.ibm.wala.ecore.common.CommonPackage#eNS_URI
   * @see #init()
   * @generated
   */
  private CommonPackageImpl() {
    super(eNS_URI, CommonFactory.eINSTANCE);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private static boolean isInited = false;

  /**
   * Creates, registers, and initializes the <b>Package</b> for this
   * model, and for any others upon which it depends.  Simple
   * dependencies are satisfied by calling this method on all
   * dependent packages before doing anything else.  This method drives
   * initialization for interdependent packages directly, in parallel
   * with this package, itself.
   * <p>Of this package and its interdependencies, all packages which
   * have not yet been registered by their URI values are first created
   * and registered.  The packages are then initialized in two steps:
   * meta-model objects for all of the packages are created before any
   * are initialized, since one package's meta-model objects may refer to
   * those of another.
   * <p>Invocation of this method will not affect any packages that have
   * already been initialized.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #eNS_URI
   * @see #createPackageContents()
   * @see #initializePackageContents()
   * @generated
   */
  public static CommonPackage init() {
    if (isInited) return (CommonPackage)EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI);

    // Obtain or create and register package
    CommonPackageImpl theCommonPackage = (CommonPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(eNS_URI) instanceof CommonPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(eNS_URI) : new CommonPackageImpl());

    isInited = true;

    // Obtain or create and register interdependencies
    GraphPackageImpl theGraphPackage = (GraphPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI) instanceof GraphPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI) : GraphPackage.eINSTANCE);
    RegexPackageImpl theRegexPackage = (RegexPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI) instanceof RegexPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI) : RegexPackage.eINSTANCE);
    JavaPackageImpl theJavaPackage = (JavaPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(JavaPackage.eNS_URI) instanceof JavaPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(JavaPackage.eNS_URI) : JavaPackage.eINSTANCE);
    JavaScopePackageImpl theJavaScopePackage = (JavaScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI) instanceof JavaScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI) : JavaScopePackage.eINSTANCE);
    J2EEScopePackageImpl theJ2EEScopePackage = (J2EEScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) instanceof J2EEScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) : J2EEScopePackage.eINSTANCE);

    // Create package meta-data objects
    theCommonPackage.createPackageContents();
    theGraphPackage.createPackageContents();
    theRegexPackage.createPackageContents();
    theJavaPackage.createPackageContents();
    theJavaScopePackage.createPackageContents();
    theJ2EEScopePackage.createPackageContents();

    // Initialize created meta-data
    theCommonPackage.initializePackageContents();
    theGraphPackage.initializePackageContents();
    theRegexPackage.initializePackageContents();
    theJavaPackage.initializePackageContents();
    theJavaScopePackage.initializePackageContents();
    theJ2EEScopePackage.initializePackageContents();

    // Mark meta-data to indicate it can't be changed
    theCommonPackage.freeze();

    return theCommonPackage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getECollection() {
    return eCollectionEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getECollection_Contents() {
    return (EReference)eCollectionEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEPair() {
    return ePairEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEPair_X() {
    return (EReference)ePairEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEPair_Y() {
    return (EReference)ePairEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getERelation() {
    return eRelationEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getERelation_Name() {
    return (EAttribute)eRelationEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEContainer() {
    return eContainerEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEContainer_Containees() {
    return (EReference)eContainerEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getENotContainer() {
    return eNotContainerEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getENotContainer_Elements() {
    return (EReference)eNotContainerEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEStringHolder() {
    return eStringHolderEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEStringHolder_Value() {
    return (EAttribute)eStringHolderEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEObjectWithContainerId() {
    return eObjectWithContainerIdEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEObjectWithContainerId_Id() {
    return (EAttribute)eObjectWithContainerIdEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EDataType getEJavaCollection() {
    return eJavaCollectionEDataType;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EDataType getEFile() {
    return eFileEDataType;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EDataType getEIterator() {
    return eIteratorEDataType;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public CommonFactory getCommonFactory() {
    return (CommonFactory)getEFactoryInstance();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private boolean isCreated = false;

  /**
   * Creates the meta-model objects for the package.  This method is
   * guarded to have no affect on any invocation but its first.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void createPackageContents() {
    if (isCreated) return;
    isCreated = true;

    // Create classes and their features
    eCollectionEClass = createEClass(ECOLLECTION);
    createEReference(eCollectionEClass, ECOLLECTION__CONTENTS);

    ePairEClass = createEClass(EPAIR);
    createEReference(ePairEClass, EPAIR__X);
    createEReference(ePairEClass, EPAIR__Y);

    eRelationEClass = createEClass(ERELATION);
    createEAttribute(eRelationEClass, ERELATION__NAME);

    eContainerEClass = createEClass(ECONTAINER);
    createEReference(eContainerEClass, ECONTAINER__CONTAINEES);

    eNotContainerEClass = createEClass(ENOT_CONTAINER);
    createEReference(eNotContainerEClass, ENOT_CONTAINER__ELEMENTS);

    eStringHolderEClass = createEClass(ESTRING_HOLDER);
    createEAttribute(eStringHolderEClass, ESTRING_HOLDER__VALUE);

    eObjectWithContainerIdEClass = createEClass(EOBJECT_WITH_CONTAINER_ID);
    createEAttribute(eObjectWithContainerIdEClass, EOBJECT_WITH_CONTAINER_ID__ID);

    // Create data types
    eJavaCollectionEDataType = createEDataType(EJAVA_COLLECTION);
    eFileEDataType = createEDataType(EFILE);
    eIteratorEDataType = createEDataType(EITERATOR);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private boolean isInitialized = false;

  /**
   * Complete the initialization of the package and its meta-model.  This
   * method is guarded to have no affect on any invocation but its first.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void initializePackageContents() {
    if (isInitialized) return;
    isInitialized = true;

    // Initialize package
    setName(eNAME);
    setNsPrefix(eNS_PREFIX);
    setNsURI(eNS_URI);

    // Add supertypes to classes
    eRelationEClass.getESuperTypes().add(this.getEContainer());
    eContainerEClass.getESuperTypes().add(this.getECollection());
    eNotContainerEClass.getESuperTypes().add(this.getECollection());

    // Initialize classes and features; add operations and parameters
    initEClass(eCollectionEClass, ECollection.class, "ECollection", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getECollection_Contents(), ecorePackage.getEObject(), null, "contents", null, 0, -1, ECollection.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(ePairEClass, EPair.class, "EPair", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getEPair_X(), ecorePackage.getEObject(), null, "X", null, 1, 1, EPair.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getEPair_Y(), ecorePackage.getEObject(), null, "Y", null, 1, 1, EPair.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eRelationEClass, ERelation.class, "ERelation", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getERelation_Name(), ecorePackage.getEString(), "name", null, 0, 1, ERelation.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eContainerEClass, EContainer.class, "EContainer", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getEContainer_Containees(), ecorePackage.getEObject(), null, "containees", null, 0, -1, EContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eNotContainerEClass, ENotContainer.class, "ENotContainer", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getENotContainer_Elements(), ecorePackage.getEObject(), null, "elements", null, 0, -1, ENotContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eStringHolderEClass, EStringHolder.class, "EStringHolder", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEStringHolder_Value(), ecorePackage.getEString(), "value", null, 1, 1, EStringHolder.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eObjectWithContainerIdEClass, EObjectWithContainerId.class, "EObjectWithContainerId", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEObjectWithContainerId_Id(), ecorePackage.getEInt(), "id", "-1", 1, 1, EObjectWithContainerId.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    // Initialize data types
    initEDataType(eJavaCollectionEDataType, Collection.class, "EJavaCollection", !IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS);
    initEDataType(eFileEDataType, File.class, "EFile", !IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS);
    initEDataType(eIteratorEDataType, Iterator.class, "EIterator", !IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS);

    // Create resource
    createResource(eNS_URI);
  }

} //CommonPackageImpl
