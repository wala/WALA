/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EPackageImpl;

import com.ibm.wala.ecore.common.CommonPackage;
import com.ibm.wala.ecore.common.impl.CommonPackageImpl;
import com.ibm.wala.ecore.graph.GraphPackage;
import com.ibm.wala.ecore.graph.impl.GraphPackageImpl;
import com.ibm.wala.ecore.j2ee.scope.J2EEScopePackage;
import com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl;
import com.ibm.wala.ecore.java.ECallSite;
import com.ibm.wala.ecore.java.EClassHierarchy;
import com.ibm.wala.ecore.java.EClassLoaderName;
import com.ibm.wala.ecore.java.EInterfaceHierarchy;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.EJavaMethod;
import com.ibm.wala.ecore.java.ETypeHierarchy;
import com.ibm.wala.ecore.java.JavaFactory;
import com.ibm.wala.ecore.java.JavaPackage;
import com.ibm.wala.ecore.java.callGraph.CallGraphPackage;
import com.ibm.wala.ecore.java.callGraph.impl.CallGraphPackageImpl;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage;
import com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl;
import com.ibm.wala.ecore.java.scope.JavaScopePackage;
import com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl;
import com.ibm.wala.ecore.perf.PerfPackage;
import com.ibm.wala.ecore.perf.impl.PerfPackageImpl;
import com.ibm.wala.ecore.regex.RegexPackage;
import com.ibm.wala.ecore.regex.impl.RegexPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class JavaPackageImpl extends EPackageImpl implements JavaPackage {
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eJavaClassEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eJavaMethodEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eCallSiteEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eClassHierarchyEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eInterfaceHierarchyEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eTypeHierarchyEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EEnum eClassLoaderNameEEnum = null;

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
   * @see com.ibm.wala.ecore.java.JavaPackage#eNS_URI
   * @see #init()
   * @generated
   */
  private JavaPackageImpl() {
    super(eNS_URI, JavaFactory.eINSTANCE);
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
  public static JavaPackage init() {
    if (isInited) return (JavaPackage)EPackage.Registry.INSTANCE.getEPackage(JavaPackage.eNS_URI);

    // Obtain or create and register package
    JavaPackageImpl theJavaPackage = (JavaPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(eNS_URI) instanceof JavaPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(eNS_URI) : new JavaPackageImpl());

    isInited = true;

    // Obtain or create and register interdependencies
    GraphPackageImpl theGraphPackage = (GraphPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI) instanceof GraphPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI) : GraphPackage.eINSTANCE);
    CommonPackageImpl theCommonPackage = (CommonPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI) instanceof CommonPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI) : CommonPackage.eINSTANCE);
    RegexPackageImpl theRegexPackage = (RegexPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI) instanceof RegexPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI) : RegexPackage.eINSTANCE);
    PerfPackageImpl thePerfPackage = (PerfPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(PerfPackage.eNS_URI) instanceof PerfPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(PerfPackage.eNS_URI) : PerfPackage.eINSTANCE);
    CallGraphPackageImpl theCallGraphPackage = (CallGraphPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(CallGraphPackage.eNS_URI) instanceof CallGraphPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(CallGraphPackage.eNS_URI) : CallGraphPackage.eINSTANCE);
    PointerAnalysisPackageImpl thePointerAnalysisPackage = (PointerAnalysisPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(PointerAnalysisPackage.eNS_URI) instanceof PointerAnalysisPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(PointerAnalysisPackage.eNS_URI) : PointerAnalysisPackage.eINSTANCE);
    JavaScopePackageImpl theJavaScopePackage = (JavaScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI) instanceof JavaScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI) : JavaScopePackage.eINSTANCE);
    J2EEScopePackageImpl theJ2EEScopePackage = (J2EEScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) instanceof J2EEScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) : J2EEScopePackage.eINSTANCE);

    // Create package meta-data objects
    theJavaPackage.createPackageContents();
    theGraphPackage.createPackageContents();
    theCommonPackage.createPackageContents();
    theRegexPackage.createPackageContents();
    thePerfPackage.createPackageContents();
    theCallGraphPackage.createPackageContents();
    thePointerAnalysisPackage.createPackageContents();
    theJavaScopePackage.createPackageContents();
    theJ2EEScopePackage.createPackageContents();

    // Initialize created meta-data
    theJavaPackage.initializePackageContents();
    theGraphPackage.initializePackageContents();
    theCommonPackage.initializePackageContents();
    theRegexPackage.initializePackageContents();
    thePerfPackage.initializePackageContents();
    theCallGraphPackage.initializePackageContents();
    thePointerAnalysisPackage.initializePackageContents();
    theJavaScopePackage.initializePackageContents();
    theJ2EEScopePackage.initializePackageContents();

    // Mark meta-data to indicate it can't be changed
    theJavaPackage.freeze();

    return theJavaPackage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEJavaClass() {
    return eJavaClassEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEJavaClass_ClassName() {
    return (EAttribute)eJavaClassEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEJavaClass_Loader() {
    return (EAttribute)eJavaClassEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEJavaMethod() {
    return eJavaMethodEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEJavaMethod_MethodName() {
    return (EAttribute)eJavaMethodEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEJavaMethod_Descriptor() {
    return (EAttribute)eJavaMethodEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEJavaMethod_JavaClass() {
    return (EReference)eJavaMethodEClass.getEStructuralFeatures().get(2);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEJavaMethod_Signature() {
    return (EAttribute)eJavaMethodEClass.getEStructuralFeatures().get(3);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getECallSite() {
    return eCallSiteEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getECallSite_BytecodeIndex() {
    return (EAttribute)eCallSiteEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getECallSite_JavaMethod() {
    return (EReference)eCallSiteEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getECallSite_DeclaredTarget() {
    return (EReference)eCallSiteEClass.getEStructuralFeatures().get(2);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEClassHierarchy() {
    return eClassHierarchyEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEInterfaceHierarchy() {
    return eInterfaceHierarchyEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getETypeHierarchy() {
    return eTypeHierarchyEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getETypeHierarchy_Classes() {
    return (EReference)eTypeHierarchyEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getETypeHierarchy_Interfaces() {
    return (EReference)eTypeHierarchyEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getETypeHierarchy_Implements() {
    return (EReference)eTypeHierarchyEClass.getEStructuralFeatures().get(2);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EEnum getEClassLoaderName() {
    return eClassLoaderNameEEnum;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaFactory getJavaFactory() {
    return (JavaFactory)getEFactoryInstance();
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
    eJavaClassEClass = createEClass(EJAVA_CLASS);
    createEAttribute(eJavaClassEClass, EJAVA_CLASS__CLASS_NAME);
    createEAttribute(eJavaClassEClass, EJAVA_CLASS__LOADER);

    eJavaMethodEClass = createEClass(EJAVA_METHOD);
    createEAttribute(eJavaMethodEClass, EJAVA_METHOD__METHOD_NAME);
    createEAttribute(eJavaMethodEClass, EJAVA_METHOD__DESCRIPTOR);
    createEReference(eJavaMethodEClass, EJAVA_METHOD__JAVA_CLASS);
    createEAttribute(eJavaMethodEClass, EJAVA_METHOD__SIGNATURE);

    eCallSiteEClass = createEClass(ECALL_SITE);
    createEAttribute(eCallSiteEClass, ECALL_SITE__BYTECODE_INDEX);
    createEReference(eCallSiteEClass, ECALL_SITE__JAVA_METHOD);
    createEReference(eCallSiteEClass, ECALL_SITE__DECLARED_TARGET);

    eClassHierarchyEClass = createEClass(ECLASS_HIERARCHY);

    eInterfaceHierarchyEClass = createEClass(EINTERFACE_HIERARCHY);

    eTypeHierarchyEClass = createEClass(ETYPE_HIERARCHY);
    createEReference(eTypeHierarchyEClass, ETYPE_HIERARCHY__CLASSES);
    createEReference(eTypeHierarchyEClass, ETYPE_HIERARCHY__INTERFACES);
    createEReference(eTypeHierarchyEClass, ETYPE_HIERARCHY__IMPLEMENTS);

    // Create enums
    eClassLoaderNameEEnum = createEEnum(ECLASS_LOADER_NAME);
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
    CallGraphPackage theCallGraphPackage = (CallGraphPackage)EPackage.Registry.INSTANCE.getEPackage(CallGraphPackage.eNS_URI);
    PointerAnalysisPackage thePointerAnalysisPackage = (PointerAnalysisPackage)EPackage.Registry.INSTANCE.getEPackage(PointerAnalysisPackage.eNS_URI);
    JavaScopePackage theJavaScopePackage = (JavaScopePackage)EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI);
    CommonPackage theCommonPackage = (CommonPackage)EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI);
    GraphPackage theGraphPackage = (GraphPackage)EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI);

    // Add subpackages
    getESubpackages().add(theCallGraphPackage);
    getESubpackages().add(thePointerAnalysisPackage);
    getESubpackages().add(theJavaScopePackage);

    // Add supertypes to classes
    eJavaClassEClass.getESuperTypes().add(theCommonPackage.getEObjectWithContainerId());
    eJavaMethodEClass.getESuperTypes().add(theCommonPackage.getEObjectWithContainerId());
    eCallSiteEClass.getESuperTypes().add(theCommonPackage.getEObjectWithContainerId());
    eClassHierarchyEClass.getESuperTypes().add(theGraphPackage.getETree());
    eInterfaceHierarchyEClass.getESuperTypes().add(theGraphPackage.getEGraph());

    // Initialize classes and features; add operations and parameters
    initEClass(eJavaClassEClass, EJavaClass.class, "EJavaClass", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEJavaClass_ClassName(), ecorePackage.getEString(), "className", null, 1, 1, EJavaClass.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getEJavaClass_Loader(), this.getEClassLoaderName(), "loader", null, 1, 1, EJavaClass.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eJavaMethodEClass, EJavaMethod.class, "EJavaMethod", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEJavaMethod_MethodName(), ecorePackage.getEString(), "methodName", null, 1, 1, EJavaMethod.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getEJavaMethod_Descriptor(), ecorePackage.getEString(), "descriptor", null, 1, 1, EJavaMethod.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getEJavaMethod_JavaClass(), this.getEJavaClass(), null, "javaClass", null, 1, 1, EJavaMethod.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getEJavaMethod_Signature(), ecorePackage.getEString(), "signature", null, 0, 1, EJavaMethod.class, IS_TRANSIENT, IS_VOLATILE, !IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

    addEOperation(eJavaMethodEClass, ecorePackage.getEBoolean(), "isClinit", 0, 1);

    initEClass(eCallSiteEClass, ECallSite.class, "ECallSite", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getECallSite_BytecodeIndex(), ecorePackage.getEInt(), "bytecodeIndex", null, 0, 1, ECallSite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getECallSite_JavaMethod(), this.getEJavaMethod(), null, "javaMethod", null, 1, 1, ECallSite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getECallSite_DeclaredTarget(), this.getEJavaMethod(), null, "declaredTarget", null, 1, 1, ECallSite.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eClassHierarchyEClass, EClassHierarchy.class, "EClassHierarchy", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(eInterfaceHierarchyEClass, EInterfaceHierarchy.class, "EInterfaceHierarchy", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(eTypeHierarchyEClass, ETypeHierarchy.class, "ETypeHierarchy", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getETypeHierarchy_Classes(), this.getEClassHierarchy(), null, "classes", null, 1, 1, ETypeHierarchy.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getETypeHierarchy_Interfaces(), this.getEInterfaceHierarchy(), null, "interfaces", null, 1, 1, ETypeHierarchy.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getETypeHierarchy_Implements(), theCommonPackage.getERelation(), null, "implements", null, 1, 1, ETypeHierarchy.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    // Initialize enums and add enum literals
    initEEnum(eClassLoaderNameEEnum, EClassLoaderName.class, "EClassLoaderName");
    addEEnumLiteral(eClassLoaderNameEEnum, EClassLoaderName.APPLICATION_LITERAL);
    addEEnumLiteral(eClassLoaderNameEEnum, EClassLoaderName.PRIMORDIAL_LITERAL);
    addEEnumLiteral(eClassLoaderNameEEnum, EClassLoaderName.EXTENSION_LITERAL);

    // Create resource
    createResource(eNS_URI);
  }

} //JavaPackageImpl
