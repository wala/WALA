/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis.impl;

import com.ibm.wala.ecore.common.CommonPackage;

import com.ibm.wala.ecore.common.impl.CommonPackageImpl;

import com.ibm.wala.ecore.graph.GraphPackage;

import com.ibm.wala.ecore.graph.impl.GraphPackageImpl;

import com.ibm.wala.ecore.j2ee.scope.J2EEScopePackage;

import com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl;

import com.ibm.wala.ecore.java.JavaPackage;

import com.ibm.wala.ecore.java.callGraph.CallGraphPackage;

import com.ibm.wala.ecore.java.callGraph.impl.CallGraphPackageImpl;

import com.ibm.wala.ecore.java.impl.JavaPackageImpl;

import com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents;
import com.ibm.wala.ecore.java.pointerAnalysis.EHeapGraph;
import com.ibm.wala.ecore.java.pointerAnalysis.EInstance;
import com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField;
import com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance;
import com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer;
import com.ibm.wala.ecore.java.pointerAnalysis.EPointer;
import com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer;
import com.ibm.wala.ecore.java.pointerAnalysis.EStaticField;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisFactory;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage;

import com.ibm.wala.ecore.java.scope.JavaScopePackage;

import com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl;

import com.ibm.wala.ecore.perf.PerfPackage;

import com.ibm.wala.ecore.perf.impl.PerfPackageImpl;

import com.ibm.wala.ecore.regex.RegexPackage;

import com.ibm.wala.ecore.regex.impl.RegexPackageImpl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class PointerAnalysisPackageImpl extends EPackageImpl implements PointerAnalysisPackage {
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass ePointerEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eInstanceFieldEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eArrayContentsEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eStaticFieldEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eLocalPointerEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eReturnValuePointerEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eInstanceEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eJavaClassInstanceEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eHeapGraphEClass = null;

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
   * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#eNS_URI
   * @see #init()
   * @generated
   */
  private PointerAnalysisPackageImpl() {
    super(eNS_URI, PointerAnalysisFactory.eINSTANCE);
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
  public static PointerAnalysisPackage init() {
    if (isInited) return (PointerAnalysisPackage)EPackage.Registry.INSTANCE.getEPackage(PointerAnalysisPackage.eNS_URI);

    // Obtain or create and register package
    PointerAnalysisPackageImpl thePointerAnalysisPackage = (PointerAnalysisPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(eNS_URI) instanceof PointerAnalysisPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(eNS_URI) : new PointerAnalysisPackageImpl());

    isInited = true;

    // Obtain or create and register interdependencies
    GraphPackageImpl theGraphPackage = (GraphPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI) instanceof GraphPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI) : GraphPackage.eINSTANCE);
    CommonPackageImpl theCommonPackage = (CommonPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI) instanceof CommonPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI) : CommonPackage.eINSTANCE);
    RegexPackageImpl theRegexPackage = (RegexPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI) instanceof RegexPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI) : RegexPackage.eINSTANCE);
    PerfPackageImpl thePerfPackage = (PerfPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(PerfPackage.eNS_URI) instanceof PerfPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(PerfPackage.eNS_URI) : PerfPackage.eINSTANCE);
    JavaPackageImpl theJavaPackage = (JavaPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(JavaPackage.eNS_URI) instanceof JavaPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(JavaPackage.eNS_URI) : JavaPackage.eINSTANCE);
    CallGraphPackageImpl theCallGraphPackage = (CallGraphPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(CallGraphPackage.eNS_URI) instanceof CallGraphPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(CallGraphPackage.eNS_URI) : CallGraphPackage.eINSTANCE);
    JavaScopePackageImpl theJavaScopePackage = (JavaScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI) instanceof JavaScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI) : JavaScopePackage.eINSTANCE);
    J2EEScopePackageImpl theJ2EEScopePackage = (J2EEScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) instanceof J2EEScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) : J2EEScopePackage.eINSTANCE);

    // Create package meta-data objects
    thePointerAnalysisPackage.createPackageContents();
    theGraphPackage.createPackageContents();
    theCommonPackage.createPackageContents();
    theRegexPackage.createPackageContents();
    thePerfPackage.createPackageContents();
    theJavaPackage.createPackageContents();
    theCallGraphPackage.createPackageContents();
    theJavaScopePackage.createPackageContents();
    theJ2EEScopePackage.createPackageContents();

    // Initialize created meta-data
    thePointerAnalysisPackage.initializePackageContents();
    theGraphPackage.initializePackageContents();
    theCommonPackage.initializePackageContents();
    theRegexPackage.initializePackageContents();
    thePerfPackage.initializePackageContents();
    theJavaPackage.initializePackageContents();
    theCallGraphPackage.initializePackageContents();
    theJavaScopePackage.initializePackageContents();
    theJ2EEScopePackage.initializePackageContents();

    // Mark meta-data to indicate it can't be changed
    thePointerAnalysisPackage.freeze();

    return thePointerAnalysisPackage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEPointer() {
    return ePointerEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEInstanceField() {
    return eInstanceFieldEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEInstanceField_FieldName() {
    return (EAttribute)eInstanceFieldEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEArrayContents() {
    return eArrayContentsEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEArrayContents_JavaClass() {
    return (EReference)eArrayContentsEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEStaticField() {
    return eStaticFieldEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEStaticField_FieldName() {
    return (EAttribute)eStaticFieldEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getELocalPointer() {
    return eLocalPointerEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getELocalPointer_ValueNumber() {
    return (EAttribute)eLocalPointerEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getELocalPointer_JavaMethod() {
    return (EReference)eLocalPointerEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEReturnValuePointer() {
    return eReturnValuePointerEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEReturnValuePointer_IsExceptionalReturnValue() {
    return (EAttribute)eReturnValuePointerEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEReturnValuePointer_JavaMethod() {
    return (EReference)eReturnValuePointerEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEInstance() {
    return eInstanceEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEJavaClassInstance() {
    return eJavaClassInstanceEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEJavaClassInstance_JavaClass() {
    return (EReference)eJavaClassInstanceEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEHeapGraph() {
    return eHeapGraphEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public PointerAnalysisFactory getPointerAnalysisFactory() {
    return (PointerAnalysisFactory)getEFactoryInstance();
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
    ePointerEClass = createEClass(EPOINTER);

    eInstanceFieldEClass = createEClass(EINSTANCE_FIELD);
    createEAttribute(eInstanceFieldEClass, EINSTANCE_FIELD__FIELD_NAME);

    eArrayContentsEClass = createEClass(EARRAY_CONTENTS);
    createEReference(eArrayContentsEClass, EARRAY_CONTENTS__JAVA_CLASS);

    eStaticFieldEClass = createEClass(ESTATIC_FIELD);
    createEAttribute(eStaticFieldEClass, ESTATIC_FIELD__FIELD_NAME);

    eLocalPointerEClass = createEClass(ELOCAL_POINTER);
    createEAttribute(eLocalPointerEClass, ELOCAL_POINTER__VALUE_NUMBER);
    createEReference(eLocalPointerEClass, ELOCAL_POINTER__JAVA_METHOD);

    eReturnValuePointerEClass = createEClass(ERETURN_VALUE_POINTER);
    createEAttribute(eReturnValuePointerEClass, ERETURN_VALUE_POINTER__IS_EXCEPTIONAL_RETURN_VALUE);
    createEReference(eReturnValuePointerEClass, ERETURN_VALUE_POINTER__JAVA_METHOD);

    eInstanceEClass = createEClass(EINSTANCE);

    eJavaClassInstanceEClass = createEClass(EJAVA_CLASS_INSTANCE);
    createEReference(eJavaClassInstanceEClass, EJAVA_CLASS_INSTANCE__JAVA_CLASS);

    eHeapGraphEClass = createEClass(EHEAP_GRAPH);
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

    // Obtain other dependent packages
    JavaPackage theJavaPackage = (JavaPackage)EPackage.Registry.INSTANCE.getEPackage(JavaPackage.eNS_URI);
    GraphPackage theGraphPackage = (GraphPackage)EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI);

    // Add supertypes to classes
    eInstanceFieldEClass.getESuperTypes().add(this.getEPointer());
    eArrayContentsEClass.getESuperTypes().add(this.getEPointer());
    eStaticFieldEClass.getESuperTypes().add(this.getEPointer());
    eLocalPointerEClass.getESuperTypes().add(this.getEPointer());
    eReturnValuePointerEClass.getESuperTypes().add(this.getEPointer());
    eJavaClassInstanceEClass.getESuperTypes().add(this.getEInstance());
    eHeapGraphEClass.getESuperTypes().add(theGraphPackage.getEGraph());

    // Initialize classes and features; add operations and parameters
    initEClass(ePointerEClass, EPointer.class, "EPointer", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(eInstanceFieldEClass, EInstanceField.class, "EInstanceField", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEInstanceField_FieldName(), ecorePackage.getEString(), "fieldName", null, 0, 1, EInstanceField.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eArrayContentsEClass, EArrayContents.class, "EArrayContents", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getEArrayContents_JavaClass(), theJavaPackage.getEJavaClass(), null, "javaClass", null, 1, 1, EArrayContents.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eStaticFieldEClass, EStaticField.class, "EStaticField", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEStaticField_FieldName(), ecorePackage.getEString(), "fieldName", null, 0, 1, EStaticField.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eLocalPointerEClass, ELocalPointer.class, "ELocalPointer", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getELocalPointer_ValueNumber(), ecorePackage.getEInt(), "valueNumber", null, 1, 1, ELocalPointer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getELocalPointer_JavaMethod(), theJavaPackage.getEJavaMethod(), null, "javaMethod", null, 1, 1, ELocalPointer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eReturnValuePointerEClass, EReturnValuePointer.class, "EReturnValuePointer", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEReturnValuePointer_IsExceptionalReturnValue(), ecorePackage.getEBoolean(), "isExceptionalReturnValue", null, 0, 1, EReturnValuePointer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getEReturnValuePointer_JavaMethod(), theJavaPackage.getEJavaMethod(), null, "javaMethod", null, 1, 1, EReturnValuePointer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eInstanceEClass, EInstance.class, "EInstance", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(eJavaClassInstanceEClass, EJavaClassInstance.class, "EJavaClassInstance", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getEJavaClassInstance_JavaClass(), theJavaPackage.getEJavaClass(), null, "javaClass", null, 1, 1, EJavaClassInstance.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eHeapGraphEClass, EHeapGraph.class, "EHeapGraph", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
  }

} //PointerAnalysisPackageImpl
