/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import com.ibm.wala.annotations.Internal;
import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.CommonPackage;
import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.ecore.common.ENotContainer;
import com.ibm.wala.ecore.common.EObjectWithContainerId;
import com.ibm.wala.ecore.common.EPair;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.common.EStringHolder;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class CommonFactoryImpl extends EFactoryImpl implements CommonFactory {
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static CommonFactory init() {
    try {
      CommonFactory theCommonFactory = (CommonFactory)EPackage.Registry.INSTANCE.getEFactory("http:///com/ibm/wala/wala.ecore.common"); 
      if (theCommonFactory != null) {
        return theCommonFactory;
      }
    }
    catch (Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new CommonFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Internal
  public CommonFactoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass) {
    switch (eClass.getClassifierID()) {
      case CommonPackage.EPAIR: return createEPair();
      case CommonPackage.ERELATION: return createERelation();
      case CommonPackage.ECONTAINER: return createEContainer();
      case CommonPackage.ENOT_CONTAINER: return createENotContainer();
      case CommonPackage.ESTRING_HOLDER: return createEStringHolder();
      case CommonPackage.EOBJECT_WITH_CONTAINER_ID: return createEObjectWithContainerId();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object createFromString(EDataType eDataType, String initialValue) {
    switch (eDataType.getClassifierID()) {
      default:
        throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String convertToString(EDataType eDataType, Object instanceValue) {
    switch (eDataType.getClassifierID()) {
      default:
        throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EPair createEPair() {
    EPairImpl ePair = new EPairImpl();
    return ePair;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ERelation createERelation() {
    ERelationImpl eRelation = new ERelationImpl();
    return eRelation;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EContainer createEContainer() {
    EContainerImpl eContainer = new EContainerImpl();
    return eContainer;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ENotContainer createENotContainer() {
    ENotContainerImpl eNotContainer = new ENotContainerImpl();
    return eNotContainer;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EStringHolder createEStringHolder() {
    EStringHolderImpl eStringHolder = new EStringHolderImpl();
    return eStringHolder;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObjectWithContainerId createEObjectWithContainerId() {
    EObjectWithContainerIdImpl eObjectWithContainerId = new EObjectWithContainerIdImpl();
    return eObjectWithContainerId;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public CommonPackage getCommonPackage() {
    return (CommonPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static CommonPackage getPackage() {
    return CommonPackage.eINSTANCE;
  }

} //CommonFactoryImpl
