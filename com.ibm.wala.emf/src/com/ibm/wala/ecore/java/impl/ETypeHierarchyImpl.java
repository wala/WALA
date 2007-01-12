/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.java.EClassHierarchy;
import com.ibm.wala.ecore.java.EInterfaceHierarchy;
import com.ibm.wala.ecore.java.ETypeHierarchy;
import com.ibm.wala.ecore.java.JavaPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EType Hierarchy</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.impl.ETypeHierarchyImpl#getClasses <em>Classes</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.impl.ETypeHierarchyImpl#getInterfaces <em>Interfaces</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.impl.ETypeHierarchyImpl#getImplements <em>Implements</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ETypeHierarchyImpl extends EObjectImpl implements ETypeHierarchy {
  /**
   * The cached value of the '{@link #getClasses() <em>Classes</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getClasses()
   * @generated
   * @ordered
   */
  protected EClassHierarchy classes = null;

  /**
   * The cached value of the '{@link #getInterfaces() <em>Interfaces</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getInterfaces()
   * @generated
   * @ordered
   */
  protected EInterfaceHierarchy interfaces = null;

  /**
   * The cached value of the '{@link #getImplements() <em>Implements</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getImplements()
   * @generated
   * @ordered
   */
  protected ERelation implements_ = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected ETypeHierarchyImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return JavaPackage.Literals.ETYPE_HIERARCHY;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClassHierarchy getClasses() {
    return classes;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetClasses(EClassHierarchy newClasses, NotificationChain msgs) {
    EClassHierarchy oldClasses = classes;
    classes = newClasses;
    if (eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, JavaPackage.ETYPE_HIERARCHY__CLASSES, oldClasses, newClasses);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setClasses(EClassHierarchy newClasses) {
    if (newClasses != classes) {
      NotificationChain msgs = null;
      if (classes != null)
        msgs = ((InternalEObject)classes).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - JavaPackage.ETYPE_HIERARCHY__CLASSES, null, msgs);
      if (newClasses != null)
        msgs = ((InternalEObject)newClasses).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - JavaPackage.ETYPE_HIERARCHY__CLASSES, null, msgs);
      msgs = basicSetClasses(newClasses, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.ETYPE_HIERARCHY__CLASSES, newClasses, newClasses));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EInterfaceHierarchy getInterfaces() {
    return interfaces;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetInterfaces(EInterfaceHierarchy newInterfaces, NotificationChain msgs) {
    EInterfaceHierarchy oldInterfaces = interfaces;
    interfaces = newInterfaces;
    if (eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, JavaPackage.ETYPE_HIERARCHY__INTERFACES, oldInterfaces, newInterfaces);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setInterfaces(EInterfaceHierarchy newInterfaces) {
    if (newInterfaces != interfaces) {
      NotificationChain msgs = null;
      if (interfaces != null)
        msgs = ((InternalEObject)interfaces).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - JavaPackage.ETYPE_HIERARCHY__INTERFACES, null, msgs);
      if (newInterfaces != null)
        msgs = ((InternalEObject)newInterfaces).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - JavaPackage.ETYPE_HIERARCHY__INTERFACES, null, msgs);
      msgs = basicSetInterfaces(newInterfaces, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.ETYPE_HIERARCHY__INTERFACES, newInterfaces, newInterfaces));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ERelation getImplements() {
    return implements_;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetImplements(ERelation newImplements, NotificationChain msgs) {
    ERelation oldImplements = implements_;
    implements_ = newImplements;
    if (eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS, oldImplements, newImplements);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setImplements(ERelation newImplements) {
    if (newImplements != implements_) {
      NotificationChain msgs = null;
      if (implements_ != null)
        msgs = ((InternalEObject)implements_).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS, null, msgs);
      if (newImplements != null)
        msgs = ((InternalEObject)newImplements).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS, null, msgs);
      msgs = basicSetImplements(newImplements, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS, newImplements, newImplements));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch (featureID) {
      case JavaPackage.ETYPE_HIERARCHY__CLASSES:
        return basicSetClasses(null, msgs);
      case JavaPackage.ETYPE_HIERARCHY__INTERFACES:
        return basicSetInterfaces(null, msgs);
      case JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS:
        return basicSetImplements(null, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case JavaPackage.ETYPE_HIERARCHY__CLASSES:
        return getClasses();
      case JavaPackage.ETYPE_HIERARCHY__INTERFACES:
        return getInterfaces();
      case JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS:
        return getImplements();
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
      case JavaPackage.ETYPE_HIERARCHY__CLASSES:
        setClasses((EClassHierarchy)newValue);
        return;
      case JavaPackage.ETYPE_HIERARCHY__INTERFACES:
        setInterfaces((EInterfaceHierarchy)newValue);
        return;
      case JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS:
        setImplements((ERelation)newValue);
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
      case JavaPackage.ETYPE_HIERARCHY__CLASSES:
        setClasses((EClassHierarchy)null);
        return;
      case JavaPackage.ETYPE_HIERARCHY__INTERFACES:
        setInterfaces((EInterfaceHierarchy)null);
        return;
      case JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS:
        setImplements((ERelation)null);
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
      case JavaPackage.ETYPE_HIERARCHY__CLASSES:
        return classes != null;
      case JavaPackage.ETYPE_HIERARCHY__INTERFACES:
        return interfaces != null;
      case JavaPackage.ETYPE_HIERARCHY__IMPLEMENTS:
        return implements_ != null;
    }
    return super.eIsSet(featureID);
  }

} //ETypeHierarchyImpl