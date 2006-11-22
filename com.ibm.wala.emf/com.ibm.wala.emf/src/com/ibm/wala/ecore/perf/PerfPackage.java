/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.perf;

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
 * @see com.ibm.wala.ecore.perf.PerfFactory
 * @model kind="package"
 * @generated
 */
public interface PerfPackage extends EPackage {
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "perf";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.perf";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.perf";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  PerfPackage eINSTANCE = com.ibm.wala.ecore.perf.impl.PerfPackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.perf.impl.EPhaseTimingImpl <em>EPhase Timing</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.perf.impl.EPhaseTimingImpl
   * @see com.ibm.wala.ecore.perf.impl.PerfPackageImpl#getEPhaseTiming()
   * @generated
   */
  int EPHASE_TIMING = 0;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPHASE_TIMING__NAME = 0;

  /**
   * The feature id for the '<em><b>Millis</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPHASE_TIMING__MILLIS = 1;

  /**
   * The feature id for the '<em><b>Order</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPHASE_TIMING__ORDER = 2;

  /**
   * The number of structural features of the '<em>EPhase Timing</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPHASE_TIMING_FEATURE_COUNT = 3;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.perf.EPhaseTiming <em>EPhase Timing</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EPhase Timing</em>'.
   * @see com.ibm.wala.ecore.perf.EPhaseTiming
   * @generated
   */
  EClass getEPhaseTiming();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.perf.EPhaseTiming#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see com.ibm.wala.ecore.perf.EPhaseTiming#getName()
   * @see #getEPhaseTiming()
   * @generated
   */
  EAttribute getEPhaseTiming_Name();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.perf.EPhaseTiming#getMillis <em>Millis</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Millis</em>'.
   * @see com.ibm.wala.ecore.perf.EPhaseTiming#getMillis()
   * @see #getEPhaseTiming()
   * @generated
   */
  EAttribute getEPhaseTiming_Millis();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.perf.EPhaseTiming#getOrder <em>Order</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Order</em>'.
   * @see com.ibm.wala.ecore.perf.EPhaseTiming#getOrder()
   * @see #getEPhaseTiming()
   * @generated
   */
  EAttribute getEPhaseTiming_Order();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  PerfFactory getPerfFactory();

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
     * The meta object literal for the '{@link com.ibm.wala.ecore.perf.impl.EPhaseTimingImpl <em>EPhase Timing</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.perf.impl.EPhaseTimingImpl
     * @see com.ibm.wala.ecore.perf.impl.PerfPackageImpl#getEPhaseTiming()
     * @generated
     */
    EClass EPHASE_TIMING = eINSTANCE.getEPhaseTiming();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EPHASE_TIMING__NAME = eINSTANCE.getEPhaseTiming_Name();

    /**
     * The meta object literal for the '<em><b>Millis</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EPHASE_TIMING__MILLIS = eINSTANCE.getEPhaseTiming_Millis();

    /**
     * The meta object literal for the '<em><b>Order</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EPHASE_TIMING__ORDER = eINSTANCE.getEPhaseTiming_Order();

  }

} //PerfPackage
