/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.perf.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.ibm.wala.ecore.perf.EPhaseTiming;
import com.ibm.wala.ecore.perf.PerfPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EPhase Timing</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.perf.impl.EPhaseTimingImpl#getName <em>Name</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.perf.impl.EPhaseTimingImpl#getMillis <em>Millis</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.perf.impl.EPhaseTimingImpl#getOrder <em>Order</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EPhaseTimingImpl extends EObjectImpl implements EPhaseTiming {
  /**
   * The default value of the '{@link #getName() <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getName()
   * @generated
   * @ordered
   */
  protected static final String NAME_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getName()
   * @generated
   * @ordered
   */
  protected String name = NAME_EDEFAULT;

  /**
   * The default value of the '{@link #getMillis() <em>Millis</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getMillis()
   * @generated
   * @ordered
   */
  protected static final long MILLIS_EDEFAULT = 0L;

  /**
   * The cached value of the '{@link #getMillis() <em>Millis</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getMillis()
   * @generated
   * @ordered
   */
  protected long millis = MILLIS_EDEFAULT;

  /**
   * The default value of the '{@link #getOrder() <em>Order</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getOrder()
   * @generated
   * @ordered
   */
  protected static final int ORDER_EDEFAULT = 0;

  /**
   * The cached value of the '{@link #getOrder() <em>Order</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getOrder()
   * @generated
   * @ordered
   */
  protected int order = ORDER_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EPhaseTimingImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return PerfPackage.Literals.EPHASE_TIMING;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getName() {
    return name;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setName(String newName) {
    String oldName = name;
    name = newName;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PerfPackage.EPHASE_TIMING__NAME, oldName, name));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public long getMillis() {
    return millis;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setMillis(long newMillis) {
    long oldMillis = millis;
    millis = newMillis;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PerfPackage.EPHASE_TIMING__MILLIS, oldMillis, millis));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public int getOrder() {
    return order;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setOrder(int newOrder) {
    int oldOrder = order;
    order = newOrder;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PerfPackage.EPHASE_TIMING__ORDER, oldOrder, order));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case PerfPackage.EPHASE_TIMING__NAME:
        return getName();
      case PerfPackage.EPHASE_TIMING__MILLIS:
        return new Long(getMillis());
      case PerfPackage.EPHASE_TIMING__ORDER:
        return new Integer(getOrder());
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void eSet(int featureID, Object newValue) {
    switch (featureID) {
      case PerfPackage.EPHASE_TIMING__NAME:
        setName((String)newValue);
        return;
      case PerfPackage.EPHASE_TIMING__MILLIS:
        setMillis(((Long)newValue).longValue());
        return;
      case PerfPackage.EPHASE_TIMING__ORDER:
        setOrder(((Integer)newValue).intValue());
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void eUnset(int featureID) {
    switch (featureID) {
      case PerfPackage.EPHASE_TIMING__NAME:
        setName(NAME_EDEFAULT);
        return;
      case PerfPackage.EPHASE_TIMING__MILLIS:
        setMillis(MILLIS_EDEFAULT);
        return;
      case PerfPackage.EPHASE_TIMING__ORDER:
        setOrder(ORDER_EDEFAULT);
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public boolean eIsSet(int featureID) {
    switch (featureID) {
      case PerfPackage.EPHASE_TIMING__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case PerfPackage.EPHASE_TIMING__MILLIS:
        return millis != MILLIS_EDEFAULT;
      case PerfPackage.EPHASE_TIMING__ORDER:
        return order != ORDER_EDEFAULT;
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String toString() {
    if (eIsProxy()) return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (name: ");
    result.append(name);
    result.append(", millis: ");
    result.append(millis);
    result.append(", order: ");
    result.append(order);
    result.append(')');
    return result.toString();
  }

} //EPhaseTimingImpl