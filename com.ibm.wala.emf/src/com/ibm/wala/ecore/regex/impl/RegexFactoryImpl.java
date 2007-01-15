/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.regex.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import com.ibm.wala.annotations.Internal;
import com.ibm.wala.ecore.regex.EPattern;
import com.ibm.wala.ecore.regex.RegexFactory;
import com.ibm.wala.ecore.regex.RegexPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class RegexFactoryImpl extends EFactoryImpl implements RegexFactory {
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static RegexFactory init() {
    try {
      RegexFactory theRegexFactory = (RegexFactory)EPackage.Registry.INSTANCE.getEFactory("http:///com/ibm/wala/wala.ecore.regex"); 
      if (theRegexFactory != null) {
        return theRegexFactory;
      }
    }
    catch (Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new RegexFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Internal
  public RegexFactoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass) {
    switch (eClass.getClassifierID()) {
      case RegexPackage.EPATTERN: return createEPattern();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EPattern createEPattern() {
    EPatternImpl ePattern = new EPatternImpl();
    return ePattern;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public RegexPackage getRegexPackage() {
    return (RegexPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static RegexPackage getPackage() {
    return RegexPackage.eINSTANCE;
  }

} //RegexFactoryImpl
