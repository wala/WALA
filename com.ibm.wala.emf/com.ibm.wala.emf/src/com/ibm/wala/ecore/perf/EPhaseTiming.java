/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.perf;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EPhase Timing</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.perf.EPhaseTiming#getName <em>Name</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.perf.EPhaseTiming#getMillis <em>Millis</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.perf.EPhaseTiming#getOrder <em>Order</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.perf.PerfPackage#getEPhaseTiming()
 * @model
 * @generated
 */
public interface EPhaseTiming extends EObject {
  /**
   * Returns the value of the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Name</em>' attribute.
   * @see #setName(String)
   * @see com.ibm.wala.ecore.perf.PerfPackage#getEPhaseTiming_Name()
   * @model required="true"
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.perf.EPhaseTiming#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Millis</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Millis</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Millis</em>' attribute.
   * @see #setMillis(long)
   * @see com.ibm.wala.ecore.perf.PerfPackage#getEPhaseTiming_Millis()
   * @model required="true"
   * @generated
   */
  long getMillis();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.perf.EPhaseTiming#getMillis <em>Millis</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Millis</em>' attribute.
   * @see #getMillis()
   * @generated
   */
  void setMillis(long value);

  /**
   * Returns the value of the '<em><b>Order</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Order</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Order</em>' attribute.
   * @see #setOrder(int)
   * @see com.ibm.wala.ecore.perf.PerfPackage#getEPhaseTiming_Order()
   * @model required="true"
   * @generated
   */
  int getOrder();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.perf.EPhaseTiming#getOrder <em>Order</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Order</em>' attribute.
   * @see #getOrder()
   * @generated
   */
  void setOrder(int value);

} // EPhaseTiming