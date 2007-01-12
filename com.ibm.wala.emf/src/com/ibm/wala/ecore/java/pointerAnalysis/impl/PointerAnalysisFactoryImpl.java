/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents;
import com.ibm.wala.ecore.java.pointerAnalysis.EHeapGraph;
import com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField;
import com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance;
import com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer;
import com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer;
import com.ibm.wala.ecore.java.pointerAnalysis.EStaticField;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisFactory;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class PointerAnalysisFactoryImpl extends EFactoryImpl implements PointerAnalysisFactory {
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static PointerAnalysisFactory init() {
    try {
      PointerAnalysisFactory thePointerAnalysisFactory = (PointerAnalysisFactory)EPackage.Registry.INSTANCE.getEFactory("http:///com/ibm/wala/wala.ecore.java.pointerAnalysis"); 
      if (thePointerAnalysisFactory != null) {
        return thePointerAnalysisFactory;
      }
    }
    catch (Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new PointerAnalysisFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public PointerAnalysisFactoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass) {
    switch (eClass.getClassifierID()) {
      case PointerAnalysisPackage.EINSTANCE_FIELD: return createEInstanceField();
      case PointerAnalysisPackage.EARRAY_CONTENTS: return createEArrayContents();
      case PointerAnalysisPackage.ESTATIC_FIELD: return createEStaticField();
      case PointerAnalysisPackage.ELOCAL_POINTER: return createELocalPointer();
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER: return createEReturnValuePointer();
      case PointerAnalysisPackage.EJAVA_CLASS_INSTANCE: return createEJavaClassInstance();
      case PointerAnalysisPackage.EHEAP_GRAPH: return createEHeapGraph();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EInstanceField createEInstanceField() {
    EInstanceFieldImpl eInstanceField = new EInstanceFieldImpl();
    return eInstanceField;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EArrayContents createEArrayContents() {
    EArrayContentsImpl eArrayContents = new EArrayContentsImpl();
    return eArrayContents;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EStaticField createEStaticField() {
    EStaticFieldImpl eStaticField = new EStaticFieldImpl();
    return eStaticField;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ELocalPointer createELocalPointer() {
    ELocalPointerImpl eLocalPointer = new ELocalPointerImpl();
    return eLocalPointer;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReturnValuePointer createEReturnValuePointer() {
    EReturnValuePointerImpl eReturnValuePointer = new EReturnValuePointerImpl();
    return eReturnValuePointer;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJavaClassInstance createEJavaClassInstance() {
    EJavaClassInstanceImpl eJavaClassInstance = new EJavaClassInstanceImpl();
    return eJavaClassInstance;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EHeapGraph createEHeapGraph() {
    EHeapGraphImpl eHeapGraph = new EHeapGraphImpl();
    return eHeapGraph;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public PointerAnalysisPackage getPointerAnalysisPackage() {
    return (PointerAnalysisPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static PointerAnalysisPackage getPackage() {
    return PointerAnalysisPackage.eINSTANCE;
  }

} //PointerAnalysisFactoryImpl
