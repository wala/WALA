/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EPackageImpl;

import com.ibm.wala.ecore.common.CommonPackage;
import com.ibm.wala.ecore.common.impl.CommonPackageImpl;
import com.ibm.wala.ecore.j2ee.scope.J2EEScopePackage;
import com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl;
import com.ibm.wala.ecore.java.scope.EBuiltInModule;
import com.ibm.wala.ecore.java.scope.EBuiltInResource;
import com.ibm.wala.ecore.java.scope.EClassFile;
import com.ibm.wala.ecore.java.scope.EClassLoader;
import com.ibm.wala.ecore.java.scope.EClasspath;
import com.ibm.wala.ecore.java.scope.EFile;
import com.ibm.wala.ecore.java.scope.EJarFile;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.ecore.java.scope.EModule;
import com.ibm.wala.ecore.java.scope.ESourceFile;
import com.ibm.wala.ecore.java.scope.EStandardClassLoader;
import com.ibm.wala.ecore.java.scope.JavaScopeFactory;
import com.ibm.wala.ecore.java.scope.JavaScopePackage;
import com.ibm.wala.ecore.regex.RegexPackage;
import com.ibm.wala.ecore.regex.impl.RegexPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class JavaScopePackageImpl extends EPackageImpl implements JavaScopePackage {
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eJavaAnalysisScopeEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eClassLoaderEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eModuleEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eBuiltInModuleEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eJarFileEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eFileEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eClassFileEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eSourceFileEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass eClasspathEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EEnum eBuiltInResourceEEnum = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EEnum eStandardClassLoaderEEnum = null;

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
   * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#eNS_URI
   * @see #init()
   * @generated
   */
  private JavaScopePackageImpl() {
    super(eNS_URI, JavaScopeFactory.eINSTANCE);
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
  public static JavaScopePackage init() {
    if (isInited) return (JavaScopePackage)EPackage.Registry.INSTANCE.getEPackage(JavaScopePackage.eNS_URI);

    // Obtain or create and register package
    JavaScopePackageImpl theJavaScopePackage = (JavaScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(eNS_URI) instanceof JavaScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(eNS_URI) : new JavaScopePackageImpl());

    isInited = true;

    // Obtain or create and register interdependencies
    CommonPackageImpl theCommonPackage = (CommonPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI) instanceof CommonPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(CommonPackage.eNS_URI) : CommonPackage.eINSTANCE);
    RegexPackageImpl theRegexPackage = (RegexPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI) instanceof RegexPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(RegexPackage.eNS_URI) : RegexPackage.eINSTANCE);
    J2EEScopePackageImpl theJ2EEScopePackage = (J2EEScopePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) instanceof J2EEScopePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(J2EEScopePackage.eNS_URI) : J2EEScopePackage.eINSTANCE);

    // Create package meta-data objects
    theJavaScopePackage.createPackageContents();
    theCommonPackage.createPackageContents();
    theRegexPackage.createPackageContents();
    theJ2EEScopePackage.createPackageContents();

    // Initialize created meta-data
    theJavaScopePackage.initializePackageContents();
    theCommonPackage.initializePackageContents();
    theRegexPackage.initializePackageContents();
    theJ2EEScopePackage.initializePackageContents();

    // Mark meta-data to indicate it can't be changed
    theJavaScopePackage.freeze();

    return theJavaScopePackage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEJavaAnalysisScope() {
    return eJavaAnalysisScopeEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEJavaAnalysisScope_Loaders() {
    return (EReference)eJavaAnalysisScopeEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEJavaAnalysisScope_ExclusionFileName() {
    return (EAttribute)eJavaAnalysisScopeEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEClassLoader() {
    return eClassLoaderEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getEClassLoader_Modules() {
    return (EReference)eClassLoaderEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEClassLoader_LoaderName() {
    return (EAttribute)eClassLoaderEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEModule() {
    return eModuleEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEBuiltInModule() {
    return eBuiltInModuleEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEBuiltInModule_Id() {
    return (EAttribute)eBuiltInModuleEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEJarFile() {
    return eJarFileEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEJarFile_Url() {
    return (EAttribute)eJarFileEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEFile() {
    return eFileEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEFile_Url() {
    return (EAttribute)eFileEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEClassFile() {
    return eClassFileEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getESourceFile() {
    return eSourceFileEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getEClasspath() {
    return eClasspathEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getEClasspath_String() {
    return (EAttribute)eClasspathEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EEnum getEBuiltInResource() {
    return eBuiltInResourceEEnum;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EEnum getEStandardClassLoader() {
    return eStandardClassLoaderEEnum;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaScopeFactory getJavaScopeFactory() {
    return (JavaScopeFactory)getEFactoryInstance();
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
    eJavaAnalysisScopeEClass = createEClass(EJAVA_ANALYSIS_SCOPE);
    createEReference(eJavaAnalysisScopeEClass, EJAVA_ANALYSIS_SCOPE__LOADERS);
    createEAttribute(eJavaAnalysisScopeEClass, EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME);

    eClassLoaderEClass = createEClass(ECLASS_LOADER);
    createEReference(eClassLoaderEClass, ECLASS_LOADER__MODULES);
    createEAttribute(eClassLoaderEClass, ECLASS_LOADER__LOADER_NAME);

    eModuleEClass = createEClass(EMODULE);

    eBuiltInModuleEClass = createEClass(EBUILT_IN_MODULE);
    createEAttribute(eBuiltInModuleEClass, EBUILT_IN_MODULE__ID);

    eJarFileEClass = createEClass(EJAR_FILE);
    createEAttribute(eJarFileEClass, EJAR_FILE__URL);

    eFileEClass = createEClass(EFILE);
    createEAttribute(eFileEClass, EFILE__URL);

    eClassFileEClass = createEClass(ECLASS_FILE);

    eSourceFileEClass = createEClass(ESOURCE_FILE);

    eClasspathEClass = createEClass(ECLASSPATH);
    createEAttribute(eClasspathEClass, ECLASSPATH__STRING);

    // Create enums
    eBuiltInResourceEEnum = createEEnum(EBUILT_IN_RESOURCE);
    eStandardClassLoaderEEnum = createEEnum(ESTANDARD_CLASS_LOADER);
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
    eBuiltInModuleEClass.getESuperTypes().add(this.getEModule());
    eJarFileEClass.getESuperTypes().add(this.getEModule());
    eFileEClass.getESuperTypes().add(this.getEModule());
    eClassFileEClass.getESuperTypes().add(this.getEFile());
    eSourceFileEClass.getESuperTypes().add(this.getEFile());

    // Initialize classes and features; add operations and parameters
    initEClass(eJavaAnalysisScopeEClass, EJavaAnalysisScope.class, "EJavaAnalysisScope", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getEJavaAnalysisScope_Loaders(), this.getEClassLoader(), null, "loaders", null, 1, -1, EJavaAnalysisScope.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getEJavaAnalysisScope_ExclusionFileName(), ecorePackage.getEString(), "exclusionFileName", null, 0, 1, EJavaAnalysisScope.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eClassLoaderEClass, EClassLoader.class, "EClassLoader", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getEClassLoader_Modules(), this.getEModule(), null, "modules", null, 1, -1, EClassLoader.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getEClassLoader_LoaderName(), ecorePackage.getEString(), "loaderName", null, 0, 1, EClassLoader.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eModuleEClass, EModule.class, "EModule", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(eBuiltInModuleEClass, EBuiltInModule.class, "EBuiltInModule", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEBuiltInModule_Id(), this.getEBuiltInResource(), "id", null, 1, 1, EBuiltInModule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eJarFileEClass, EJarFile.class, "EJarFile", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEJarFile_Url(), ecorePackage.getEString(), "url", null, 1, 1, EJarFile.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eFileEClass, EFile.class, "EFile", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEFile_Url(), ecorePackage.getEString(), "url", null, 1, 1, EFile.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(eClassFileEClass, EClassFile.class, "EClassFile", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(eSourceFileEClass, ESourceFile.class, "ESourceFile", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(eClasspathEClass, EClasspath.class, "EClasspath", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getEClasspath_String(), ecorePackage.getEString(), "string", null, 1, 1, EClasspath.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    // Initialize enums and add enum literals
    initEEnum(eBuiltInResourceEEnum, EBuiltInResource.class, "EBuiltInResource");
    addEEnumLiteral(eBuiltInResourceEEnum, EBuiltInResource.DEFAULT_J2SE_LIBS_LITERAL);
    addEEnumLiteral(eBuiltInResourceEEnum, EBuiltInResource.DEFAULT_J2EE_LIBS_LITERAL);
    addEEnumLiteral(eBuiltInResourceEEnum, EBuiltInResource.PRIMORDIAL_JAR_MODEL_LITERAL);
    addEEnumLiteral(eBuiltInResourceEEnum, EBuiltInResource.EXTENSION_JAR_MODEL_LITERAL);

    initEEnum(eStandardClassLoaderEEnum, EStandardClassLoader.class, "EStandardClassLoader");
    addEEnumLiteral(eStandardClassLoaderEEnum, EStandardClassLoader.PRIMORDIAL_LITERAL);
    addEEnumLiteral(eStandardClassLoaderEEnum, EStandardClassLoader.EXTENSION_LITERAL);
    addEEnumLiteral(eStandardClassLoaderEEnum, EStandardClassLoader.APPLICATION_LITERAL);
    addEEnumLiteral(eStandardClassLoaderEEnum, EStandardClassLoader.SYNTHETIC_LITERAL);
  }

} //JavaScopePackageImpl
