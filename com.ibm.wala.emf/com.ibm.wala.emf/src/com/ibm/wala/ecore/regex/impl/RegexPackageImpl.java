/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.regex.impl;

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

import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage;

import com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl;

import com.ibm.wala.ecore.java.scope.JavaScopePackage;

import com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl;

import com.ibm.wala.ecore.perf.PerfPackage;

import com.ibm.wala.ecore.perf.impl.PerfPackageImpl;

import com.ibm.wala.ecore.regex.EPattern;
import com.ibm.wala.ecore.regex.RegexFactory;
import com.ibm.wala.ecore.regex.RegexPackage;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class RegexPackageImpl extends EPackageImpl implements RegexPackage {
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass ePatternEClass = null;

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
   * @see com.ibm.wala.ecore.regex.RegexPackage#eNS_URI
   * @see #init()
   * @generated
   */
  private RegexPackageImpl() {
    super(eNS_URI, RegexFactory.eINSTANCE);
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
  public static RegexPackage init() {
    if (isInited) return (RegexPackage)EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI);

    // Obtain or create and register package
    RegexPackageImpl theRegexPackage = (RegexPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(eNS_URI) instanceof RegexPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(eNS_URI) : new RegexPackageImpl());

    isInited = true;

    // Obtain or create and register interdependencies
    GraphPackageImpl theGraphPackage = (GraphPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI) instanceof GraphPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(GraphPackage.eNS_URI) : GraphPackage.eINSTANCE);
    CommonPackageImpl theCommonPackage = (CommonPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI) instanceof CommonPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI) : CommonPackage.eINSTANCE);
    PerfPackageImpl thePerfPackage = (PerfPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(PerfPackage.eNS_URI) instanceof PerfPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(PerfPackage.eNS_URI) : PerfPackage.eINSTANCE);
    JavaPackageImpl theJavaPackage = (JavaPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(JavaPackage.eNS_URI) instanceof JavaPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(JavaPackage.eNS_URI) : JavaPackage.eINSTANCE);
    CallGraphPackageImpl theCallGraphPackage = (CallGraphPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(CallGraphPackage.eNS_URI) instanceof CallGraphPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(CallGraphPackage.eNS_URI) : CallGraphPackage.eINSTANCE);
    PointerAnalysisPackageImpl thePointerAnalysisPackage = (PointerAnalysisPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(PointerAnalysisPackage.eNS_URI) instanceof PointerAnalysisPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(PointerAnalysisPackage.eNS_URI) : PointerAnalysisPackage.eINSTANCE);
    JavaScopePackageImpl theJavaScopePackage = (JavaScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI) instanceof JavaScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI) : JavaScopePackage.eINSTANCE);
    J2EEScopePackageImpl theJ2EEScopePackage = (J2EEScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) instanceof J2EEScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) : J2EEScopePackage.eINSTANCE);

    // Create package meta-data objects
    theRegexPackage.createPackageContents();
    theGraphPackage.createPackageContents();
    theCommonPackage.createPackageContents();
    thePerfPackage.createPackageContents();
    theJavaPackage.createPackageContents();
    theCallGraphPackage.createPackageContents();
    thePointerAnalysisPackage.createPackageContents();
    theJavaScopePackage.createPackageContents();
    theJ2EEScopePackage.createPackageContents();

    // Initialize created meta-data
    theRegexPackage.initializePackageContents();
    theGraphPackage.initializePackageContents();
    theCommonPackage.initializePackageContents();
    thePerfPackage.initializePackageContents();
    theJavaPackage.initializePackageContents();
    theCallGraphPackage.initializePackageContents();
    thePointerAnalysisPackage.initializePackageContents();
    theJavaScopePackage.initializePackageContents();
    theJ2EEScopePackage.initializePackageContents();

    // Mark meta-data to indicate it can't be changed
    theRegexPackage.freeze();

    return theRegexPackage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEPattern() {
    return ePatternEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEPattern_Pattern() {
    return (EAttribute)ePatternEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public RegexFactory getRegexFactory() {
    return (RegexFactory)getEFactoryInstance();
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
    ePatternEClass = createEClass(EPATTERN);
    createEAttribute(ePatternEClass, EPATTERN__PATTERN);
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

    // Initialize classes and features; add operations and parameters
    initEClass(ePatternEClass, EPattern.class, "EPattern", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEPattern_Pattern(), ecorePackage.getEString(), "pattern", null, 1, 1, EPattern.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    // Create resource
    createResource(eNS_URI);
  }

} //RegexPackageImpl
