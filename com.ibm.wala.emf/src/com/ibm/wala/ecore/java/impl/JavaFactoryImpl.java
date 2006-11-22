/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.impl;

import com.ibm.wala.ecore.java.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class JavaFactoryImpl extends EFactoryImpl implements JavaFactory {
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static JavaFactory init() {
    try {
      JavaFactory theJavaFactory = (JavaFactory)EPackage.Registry.INSTANCE.getEFactory("http:///com/ibm/wala/wala.ecore.java"); 
      if (theJavaFactory != null) {
        return theJavaFactory;
      }
    }
    catch (Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new JavaFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaFactoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass) {
    switch (eClass.getClassifierID()) {
      case JavaPackage.EJAVA_CLASS: return createEJavaClass();
      case JavaPackage.EJAVA_METHOD: return createEJavaMethod();
      case JavaPackage.ECALL_SITE: return createECallSite();
      case JavaPackage.ECLASS_HIERARCHY: return createEClassHierarchy();
      case JavaPackage.EINTERFACE_HIERARCHY: return createEInterfaceHierarchy();
      case JavaPackage.ETYPE_HIERARCHY: return createETypeHierarchy();
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
      case JavaPackage.ECLASS_LOADER_NAME:
        return createEClassLoaderNameFromString(eDataType, initialValue);
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
      case JavaPackage.ECLASS_LOADER_NAME:
        return convertEClassLoaderNameToString(eDataType, instanceValue);
      default:
        throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJavaClass createEJavaClass() {
    EJavaClassImpl eJavaClass = new EJavaClassImpl();
    return eJavaClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJavaMethod createEJavaMethod() {
    EJavaMethodImpl eJavaMethod = new EJavaMethodImpl();
    return eJavaMethod;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ECallSite createECallSite() {
    ECallSiteImpl eCallSite = new ECallSiteImpl();
    return eCallSite;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClassHierarchy createEClassHierarchy() {
    EClassHierarchyImpl eClassHierarchy = new EClassHierarchyImpl();
    return eClassHierarchy;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EInterfaceHierarchy createEInterfaceHierarchy() {
    EInterfaceHierarchyImpl eInterfaceHierarchy = new EInterfaceHierarchyImpl();
    return eInterfaceHierarchy;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ETypeHierarchy createETypeHierarchy() {
    ETypeHierarchyImpl eTypeHierarchy = new ETypeHierarchyImpl();
    return eTypeHierarchy;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClassLoaderName createEClassLoaderNameFromString(EDataType eDataType, String initialValue) {
    EClassLoaderName result = EClassLoaderName.get(initialValue);
    if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
    return result;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String convertEClassLoaderNameToString(EDataType eDataType, Object instanceValue) {
    return instanceValue == null ? null : instanceValue.toString();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaPackage getJavaPackage() {
    return (JavaPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static JavaPackage getPackage() {
    return JavaPackage.eINSTANCE;
  }

} //JavaFactoryImpl
