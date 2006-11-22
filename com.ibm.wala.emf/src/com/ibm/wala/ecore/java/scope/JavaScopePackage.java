/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.scope.JavaScopeFactory
 * @model kind="package"
 * @generated
 */
public interface JavaScopePackage extends EPackage {
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "scope";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.java.scope";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.java.scope";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  JavaScopePackage eINSTANCE = com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.impl.EJavaAnalysisScopeImpl <em>EJava Analysis Scope</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.impl.EJavaAnalysisScopeImpl
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEJavaAnalysisScope()
   * @generated
   */
  int EJAVA_ANALYSIS_SCOPE = 0;

  /**
   * The feature id for the '<em><b>Loaders</b></em>' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_ANALYSIS_SCOPE__LOADERS = 0;

  /**
   * The feature id for the '<em><b>Exclusion File Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME = 1;

  /**
   * The number of structural features of the '<em>EJava Analysis Scope</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_ANALYSIS_SCOPE_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.impl.EClassLoaderImpl <em>EClass Loader</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.impl.EClassLoaderImpl
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEClassLoader()
   * @generated
   */
  int ECLASS_LOADER = 1;

  /**
   * The feature id for the '<em><b>Modules</b></em>' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASS_LOADER__MODULES = 0;

  /**
   * The feature id for the '<em><b>Loader Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASS_LOADER__LOADER_NAME = 1;

  /**
   * The number of structural features of the '<em>EClass Loader</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASS_LOADER_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.EModule <em>EModule</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.EModule
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEModule()
   * @generated
   */
  int EMODULE = 2;

  /**
   * The number of structural features of the '<em>EModule</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EMODULE_FEATURE_COUNT = 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.impl.EBuiltInModuleImpl <em>EBuilt In Module</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.impl.EBuiltInModuleImpl
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEBuiltInModule()
   * @generated
   */
  int EBUILT_IN_MODULE = 3;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EBUILT_IN_MODULE__ID = EMODULE_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>EBuilt In Module</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EBUILT_IN_MODULE_FEATURE_COUNT = EMODULE_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.impl.EJarFileImpl <em>EJar File</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.impl.EJarFileImpl
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEJarFile()
   * @generated
   */
  int EJAR_FILE = 4;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAR_FILE__URL = EMODULE_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>EJar File</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAR_FILE_FEATURE_COUNT = EMODULE_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.impl.EFileImpl <em>EFile</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.impl.EFileImpl
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEFile()
   * @generated
   */
  int EFILE = 5;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EFILE__URL = EMODULE_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>EFile</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EFILE_FEATURE_COUNT = EMODULE_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.impl.EClassFileImpl <em>EClass File</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.impl.EClassFileImpl
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEClassFile()
   * @generated
   */
  int ECLASS_FILE = 6;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASS_FILE__URL = EFILE__URL;

  /**
   * The number of structural features of the '<em>EClass File</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASS_FILE_FEATURE_COUNT = EFILE_FEATURE_COUNT + 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.impl.ESourceFileImpl <em>ESource File</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.impl.ESourceFileImpl
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getESourceFile()
   * @generated
   */
  int ESOURCE_FILE = 7;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ESOURCE_FILE__URL = EFILE__URL;

  /**
   * The number of structural features of the '<em>ESource File</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ESOURCE_FILE_FEATURE_COUNT = EFILE_FEATURE_COUNT + 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.impl.EClasspathImpl <em>EClasspath</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.impl.EClasspathImpl
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEClasspath()
   * @generated
   */
  int ECLASSPATH = 8;

  /**
   * The feature id for the '<em><b>String</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASSPATH__STRING = 0;

  /**
   * The number of structural features of the '<em>EClasspath</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASSPATH_FEATURE_COUNT = 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.EBuiltInResource <em>EBuilt In Resource</em>}' enum.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.EBuiltInResource
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEBuiltInResource()
   * @generated
   */
  int EBUILT_IN_RESOURCE = 9;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.scope.EStandardClassLoader <em>EStandard Class Loader</em>}' enum.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.scope.EStandardClassLoader
   * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEStandardClassLoader()
   * @generated
   */
  int ESTANDARD_CLASS_LOADER = 10;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.EJavaAnalysisScope <em>EJava Analysis Scope</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EJava Analysis Scope</em>'.
   * @see com.ibm.wala.ecore.java.scope.EJavaAnalysisScope
   * @generated
   */
  EClass getEJavaAnalysisScope();

  /**
   * Returns the meta object for the containment reference list '{@link com.ibm.wala.ecore.java.scope.EJavaAnalysisScope#getLoaders <em>Loaders</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference list '<em>Loaders</em>'.
   * @see com.ibm.wala.ecore.java.scope.EJavaAnalysisScope#getLoaders()
   * @see #getEJavaAnalysisScope()
   * @generated
   */
  EReference getEJavaAnalysisScope_Loaders();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.scope.EJavaAnalysisScope#getExclusionFileName <em>Exclusion File Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Exclusion File Name</em>'.
   * @see com.ibm.wala.ecore.java.scope.EJavaAnalysisScope#getExclusionFileName()
   * @see #getEJavaAnalysisScope()
   * @generated
   */
  EAttribute getEJavaAnalysisScope_ExclusionFileName();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.EClassLoader <em>EClass Loader</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EClass Loader</em>'.
   * @see com.ibm.wala.ecore.java.scope.EClassLoader
   * @generated
   */
  EClass getEClassLoader();

  /**
   * Returns the meta object for the containment reference list '{@link com.ibm.wala.ecore.java.scope.EClassLoader#getModules <em>Modules</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference list '<em>Modules</em>'.
   * @see com.ibm.wala.ecore.java.scope.EClassLoader#getModules()
   * @see #getEClassLoader()
   * @generated
   */
  EReference getEClassLoader_Modules();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.scope.EClassLoader#getLoaderName <em>Loader Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Loader Name</em>'.
   * @see com.ibm.wala.ecore.java.scope.EClassLoader#getLoaderName()
   * @see #getEClassLoader()
   * @generated
   */
  EAttribute getEClassLoader_LoaderName();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.EModule <em>EModule</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EModule</em>'.
   * @see com.ibm.wala.ecore.java.scope.EModule
   * @generated
   */
  EClass getEModule();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.EBuiltInModule <em>EBuilt In Module</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EBuilt In Module</em>'.
   * @see com.ibm.wala.ecore.java.scope.EBuiltInModule
   * @generated
   */
  EClass getEBuiltInModule();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.scope.EBuiltInModule#getId <em>Id</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see com.ibm.wala.ecore.java.scope.EBuiltInModule#getId()
   * @see #getEBuiltInModule()
   * @generated
   */
  EAttribute getEBuiltInModule_Id();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.EJarFile <em>EJar File</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EJar File</em>'.
   * @see com.ibm.wala.ecore.java.scope.EJarFile
   * @generated
   */
  EClass getEJarFile();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.scope.EJarFile#getUrl <em>Url</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see com.ibm.wala.ecore.java.scope.EJarFile#getUrl()
   * @see #getEJarFile()
   * @generated
   */
  EAttribute getEJarFile_Url();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.EFile <em>EFile</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EFile</em>'.
   * @see com.ibm.wala.ecore.java.scope.EFile
   * @generated
   */
  EClass getEFile();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.scope.EFile#getUrl <em>Url</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see com.ibm.wala.ecore.java.scope.EFile#getUrl()
   * @see #getEFile()
   * @generated
   */
  EAttribute getEFile_Url();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.EClassFile <em>EClass File</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EClass File</em>'.
   * @see com.ibm.wala.ecore.java.scope.EClassFile
   * @generated
   */
  EClass getEClassFile();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.ESourceFile <em>ESource File</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>ESource File</em>'.
   * @see com.ibm.wala.ecore.java.scope.ESourceFile
   * @generated
   */
  EClass getESourceFile();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.scope.EClasspath <em>EClasspath</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EClasspath</em>'.
   * @see com.ibm.wala.ecore.java.scope.EClasspath
   * @generated
   */
  EClass getEClasspath();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.scope.EClasspath#getString <em>String</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>String</em>'.
   * @see com.ibm.wala.ecore.java.scope.EClasspath#getString()
   * @see #getEClasspath()
   * @generated
   */
  EAttribute getEClasspath_String();

  /**
   * Returns the meta object for enum '{@link com.ibm.wala.ecore.java.scope.EBuiltInResource <em>EBuilt In Resource</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for enum '<em>EBuilt In Resource</em>'.
   * @see com.ibm.wala.ecore.java.scope.EBuiltInResource
   * @generated
   */
  EEnum getEBuiltInResource();

  /**
   * Returns the meta object for enum '{@link com.ibm.wala.ecore.java.scope.EStandardClassLoader <em>EStandard Class Loader</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for enum '<em>EStandard Class Loader</em>'.
   * @see com.ibm.wala.ecore.java.scope.EStandardClassLoader
   * @generated
   */
  EEnum getEStandardClassLoader();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  JavaScopeFactory getJavaScopeFactory();

  /**
   * <!-- begin-user-doc -->
   * Defines literals for the meta objects that represent
   * <ul>
   *   <li>each class,</li>
   *   <li>each feature of each class,</li>
   *   <li>each enum,</li>
   *   <li>and each data type</li>
   * </ul>
   * <!-- end-user-doc -->
   * @generated
   */
  interface Literals  {
    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.impl.EJavaAnalysisScopeImpl <em>EJava Analysis Scope</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.impl.EJavaAnalysisScopeImpl
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEJavaAnalysisScope()
     * @generated
     */
    EClass EJAVA_ANALYSIS_SCOPE = eINSTANCE.getEJavaAnalysisScope();

    /**
     * The meta object literal for the '<em><b>Loaders</b></em>' containment reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference EJAVA_ANALYSIS_SCOPE__LOADERS = eINSTANCE.getEJavaAnalysisScope_Loaders();

    /**
     * The meta object literal for the '<em><b>Exclusion File Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME = eINSTANCE.getEJavaAnalysisScope_ExclusionFileName();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.impl.EClassLoaderImpl <em>EClass Loader</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.impl.EClassLoaderImpl
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEClassLoader()
     * @generated
     */
    EClass ECLASS_LOADER = eINSTANCE.getEClassLoader();

    /**
     * The meta object literal for the '<em><b>Modules</b></em>' containment reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ECLASS_LOADER__MODULES = eINSTANCE.getEClassLoader_Modules();

    /**
     * The meta object literal for the '<em><b>Loader Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ECLASS_LOADER__LOADER_NAME = eINSTANCE.getEClassLoader_LoaderName();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.EModule <em>EModule</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.EModule
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEModule()
     * @generated
     */
    EClass EMODULE = eINSTANCE.getEModule();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.impl.EBuiltInModuleImpl <em>EBuilt In Module</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.impl.EBuiltInModuleImpl
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEBuiltInModule()
     * @generated
     */
    EClass EBUILT_IN_MODULE = eINSTANCE.getEBuiltInModule();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EBUILT_IN_MODULE__ID = eINSTANCE.getEBuiltInModule_Id();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.impl.EJarFileImpl <em>EJar File</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.impl.EJarFileImpl
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEJarFile()
     * @generated
     */
    EClass EJAR_FILE = eINSTANCE.getEJarFile();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EJAR_FILE__URL = eINSTANCE.getEJarFile_Url();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.impl.EFileImpl <em>EFile</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.impl.EFileImpl
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEFile()
     * @generated
     */
    EClass EFILE = eINSTANCE.getEFile();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EFILE__URL = eINSTANCE.getEFile_Url();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.impl.EClassFileImpl <em>EClass File</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.impl.EClassFileImpl
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEClassFile()
     * @generated
     */
    EClass ECLASS_FILE = eINSTANCE.getEClassFile();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.impl.ESourceFileImpl <em>ESource File</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.impl.ESourceFileImpl
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getESourceFile()
     * @generated
     */
    EClass ESOURCE_FILE = eINSTANCE.getESourceFile();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.impl.EClasspathImpl <em>EClasspath</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.impl.EClasspathImpl
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEClasspath()
     * @generated
     */
    EClass ECLASSPATH = eINSTANCE.getEClasspath();

    /**
     * The meta object literal for the '<em><b>String</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ECLASSPATH__STRING = eINSTANCE.getEClasspath_String();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.EBuiltInResource <em>EBuilt In Resource</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.EBuiltInResource
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEBuiltInResource()
     * @generated
     */
    EEnum EBUILT_IN_RESOURCE = eINSTANCE.getEBuiltInResource();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.scope.EStandardClassLoader <em>EStandard Class Loader</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.scope.EStandardClassLoader
     * @see com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl#getEStandardClassLoader()
     * @generated
     */
    EEnum ESTANDARD_CLASS_LOADER = eINSTANCE.getEStandardClassLoader();

  }

} //JavaScopePackage
