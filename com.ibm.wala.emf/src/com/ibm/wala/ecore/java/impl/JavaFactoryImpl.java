/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import com.ibm.wala.annotations.Internal;
import com.ibm.wala.ecore.java.EClassLoaderName;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.JavaFactory;
import com.ibm.wala.ecore.java.JavaPackage;

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
  @Internal
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
