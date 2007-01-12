/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.j2ee.scope;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

import com.ibm.wala.ecore.java.scope.JavaScopePackage;

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
 * @see com.ibm.wala.ecore.j2ee.scope.J2EEScopeFactory
 * @model kind="package"
 * @generated
 */
public interface J2EEScopePackage extends EPackage {
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
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.j2ee.scope";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.j2ee.scope";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  J2EEScopePackage eINSTANCE = com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.j2ee.scope.impl.EJ2EEAnalysisScopeImpl <em>EJ2EE Analysis Scope</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.j2ee.scope.impl.EJ2EEAnalysisScopeImpl
   * @see com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl#getEJ2EEAnalysisScope()
   * @generated
   */
  int EJ2EE_ANALYSIS_SCOPE = 0;

  /**
   * The feature id for the '<em><b>Loaders</b></em>' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJ2EE_ANALYSIS_SCOPE__LOADERS = JavaScopePackage.EJAVA_ANALYSIS_SCOPE__LOADERS;

  /**
   * The feature id for the '<em><b>Exclusion File Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJ2EE_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME = JavaScopePackage.EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME;

  /**
   * The number of structural features of the '<em>EJ2EE Analysis Scope</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJ2EE_ANALYSIS_SCOPE_FEATURE_COUNT = JavaScopePackage.EJAVA_ANALYSIS_SCOPE_FEATURE_COUNT + 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.j2ee.scope.impl.EEarFileImpl <em>EEar File</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.j2ee.scope.impl.EEarFileImpl
   * @see com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl#getEEarFile()
   * @generated
   */
  int EEAR_FILE = 1;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EEAR_FILE__URL = JavaScopePackage.EJAR_FILE__URL;

  /**
   * The number of structural features of the '<em>EEar File</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EEAR_FILE_FEATURE_COUNT = JavaScopePackage.EJAR_FILE_FEATURE_COUNT + 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.j2ee.scope.impl.EWarFileImpl <em>EWar File</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.j2ee.scope.impl.EWarFileImpl
   * @see com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl#getEWarFile()
   * @generated
   */
  int EWAR_FILE = 2;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EWAR_FILE__URL = JavaScopePackage.EJAR_FILE__URL;

  /**
   * The number of structural features of the '<em>EWar File</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EWAR_FILE_FEATURE_COUNT = JavaScopePackage.EJAR_FILE_FEATURE_COUNT + 0;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.j2ee.scope.EJ2EEAnalysisScope <em>EJ2EE Analysis Scope</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EJ2EE Analysis Scope</em>'.
   * @see com.ibm.wala.ecore.j2ee.scope.EJ2EEAnalysisScope
   * @generated
   */
  EClass getEJ2EEAnalysisScope();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.j2ee.scope.EEarFile <em>EEar File</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EEar File</em>'.
   * @see com.ibm.wala.ecore.j2ee.scope.EEarFile
   * @generated
   */
  EClass getEEarFile();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.j2ee.scope.EWarFile <em>EWar File</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EWar File</em>'.
   * @see com.ibm.wala.ecore.j2ee.scope.EWarFile
   * @generated
   */
  EClass getEWarFile();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  J2EEScopeFactory getJ2EEScopeFactory();

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
  interface Literals {
    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.j2ee.scope.impl.EJ2EEAnalysisScopeImpl <em>EJ2EE Analysis Scope</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.j2ee.scope.impl.EJ2EEAnalysisScopeImpl
     * @see com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl#getEJ2EEAnalysisScope()
     * @generated
     */
    EClass EJ2EE_ANALYSIS_SCOPE = eINSTANCE.getEJ2EEAnalysisScope();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.j2ee.scope.impl.EEarFileImpl <em>EEar File</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.j2ee.scope.impl.EEarFileImpl
     * @see com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl#getEEarFile()
     * @generated
     */
    EClass EEAR_FILE = eINSTANCE.getEEarFile();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.j2ee.scope.impl.EWarFileImpl <em>EWar File</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.j2ee.scope.impl.EWarFileImpl
     * @see com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl#getEWarFile()
     * @generated
     */
    EClass EWAR_FILE = eINSTANCE.getEWarFile();

  }

} //J2EEScopePackage
