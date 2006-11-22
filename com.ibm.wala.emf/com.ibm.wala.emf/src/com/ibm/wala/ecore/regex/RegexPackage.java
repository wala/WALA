/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.regex;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

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
 * @see com.ibm.wala.ecore.regex.RegexFactory
 * @model kind="package"
 * @generated
 */
public interface RegexPackage extends EPackage {
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "regex";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.regex";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.regex";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  RegexPackage eINSTANCE = com.ibm.wala.ecore.regex.impl.RegexPackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.regex.impl.EPatternImpl <em>EPattern</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.regex.impl.EPatternImpl
   * @see com.ibm.wala.ecore.regex.impl.RegexPackageImpl#getEPattern()
   * @generated
   */
  int EPATTERN = 0;

  /**
   * The feature id for the '<em><b>Pattern</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPATTERN__PATTERN = 0;

  /**
   * The number of structural features of the '<em>EPattern</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPATTERN_FEATURE_COUNT = 1;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.regex.EPattern <em>EPattern</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EPattern</em>'.
   * @see com.ibm.wala.ecore.regex.EPattern
   * @generated
   */
  EClass getEPattern();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.regex.EPattern#getPattern <em>Pattern</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Pattern</em>'.
   * @see com.ibm.wala.ecore.regex.EPattern#getPattern()
   * @see #getEPattern()
   * @generated
   */
  EAttribute getEPattern_Pattern();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  RegexFactory getRegexFactory();

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
     * The meta object literal for the '{@link com.ibm.wala.ecore.regex.impl.EPatternImpl <em>EPattern</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.regex.impl.EPatternImpl
     * @see com.ibm.wala.ecore.regex.impl.RegexPackageImpl#getEPattern()
     * @generated
     */
    EClass EPATTERN = eINSTANCE.getEPattern();

    /**
     * The meta object literal for the '<em><b>Pattern</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EPATTERN__PATTERN = eINSTANCE.getEPattern_Pattern();

  }

} //RegexPackage
