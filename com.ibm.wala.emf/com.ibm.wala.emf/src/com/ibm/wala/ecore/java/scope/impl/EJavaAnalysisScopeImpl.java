/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope.impl;

import com.ibm.wala.ecore.java.scope.EClassLoader;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.ecore.java.scope.JavaScopePackage;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EJava Analysis Scope</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.scope.impl.EJavaAnalysisScopeImpl#getLoaders <em>Loaders</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.scope.impl.EJavaAnalysisScopeImpl#getExclusionFileName <em>Exclusion File Name</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EJavaAnalysisScopeImpl extends EObjectImpl implements EJavaAnalysisScope {
  /**
   * The cached value of the '{@link #getLoaders() <em>Loaders</em>}' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getLoaders()
   * @generated
   * @ordered
   */
  protected EList loaders = null;

  /**
   * The default value of the '{@link #getExclusionFileName() <em>Exclusion File Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getExclusionFileName()
   * @generated
   * @ordered
   */
  protected static final String EXCLUSION_FILE_NAME_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getExclusionFileName() <em>Exclusion File Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getExclusionFileName()
   * @generated
   * @ordered
   */
  protected String exclusionFileName = EXCLUSION_FILE_NAME_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EJavaAnalysisScopeImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return JavaScopePackage.Literals.EJAVA_ANALYSIS_SCOPE;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList getLoaders() {
    if (loaders == null) {
      loaders = new EObjectContainmentEList(EClassLoader.class, this, JavaScopePackage.EJAVA_ANALYSIS_SCOPE__LOADERS);
    }
    return loaders;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getExclusionFileName() {
    return exclusionFileName;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setExclusionFileName(String newExclusionFileName) {
    String oldExclusionFileName = exclusionFileName;
    exclusionFileName = newExclusionFileName;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaScopePackage.EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME, oldExclusionFileName, exclusionFileName));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch (featureID) {
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__LOADERS:
        return ((InternalEList)getLoaders()).basicRemove(otherEnd, msgs);
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
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__LOADERS:
        return getLoaders();
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME:
        return getExclusionFileName();
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
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__LOADERS:
        getLoaders().clear();
        getLoaders().addAll((Collection)newValue);
        return;
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME:
        setExclusionFileName((String)newValue);
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
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__LOADERS:
        getLoaders().clear();
        return;
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME:
        setExclusionFileName(EXCLUSION_FILE_NAME_EDEFAULT);
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
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__LOADERS:
        return loaders != null && !loaders.isEmpty();
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE__EXCLUSION_FILE_NAME:
        return EXCLUSION_FILE_NAME_EDEFAULT == null ? exclusionFileName != null : !EXCLUSION_FILE_NAME_EDEFAULT.equals(exclusionFileName);
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
    result.append(" (exclusionFileName: ");
    result.append(exclusionFileName);
    result.append(')');
    return result.toString();
  }

} //EJavaAnalysisScopeImpl