/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis;

import com.ibm.wala.ecore.graph.GraphPackage;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

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
 * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisFactory
 * @model kind="package"
 * @generated
 */
public interface PointerAnalysisPackage extends EPackage {
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "pointerAnalysis";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.java.pointerAnalysis";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.java.pointerAnalysis";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  PointerAnalysisPackage eINSTANCE = com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EPointerImpl <em>EPointer</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EPointerImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEPointer()
   * @generated
   */
  int EPOINTER = 0;

  /**
   * The number of structural features of the '<em>EPointer</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPOINTER_FEATURE_COUNT = 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EInstanceFieldImpl <em>EInstance Field</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EInstanceFieldImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEInstanceField()
   * @generated
   */
  int EINSTANCE_FIELD = 1;

  /**
   * The feature id for the '<em><b>Field Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EINSTANCE_FIELD__FIELD_NAME = EPOINTER_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>EInstance Field</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EINSTANCE_FIELD_FEATURE_COUNT = EPOINTER_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EArrayContentsImpl <em>EArray Contents</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EArrayContentsImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEArrayContents()
   * @generated
   */
  int EARRAY_CONTENTS = 2;

  /**
   * The feature id for the '<em><b>Java Class</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EARRAY_CONTENTS__JAVA_CLASS = EPOINTER_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>EArray Contents</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EARRAY_CONTENTS_FEATURE_COUNT = EPOINTER_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EStaticFieldImpl <em>EStatic Field</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EStaticFieldImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEStaticField()
   * @generated
   */
  int ESTATIC_FIELD = 3;

  /**
   * The feature id for the '<em><b>Field Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ESTATIC_FIELD__FIELD_NAME = EPOINTER_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>EStatic Field</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ESTATIC_FIELD_FEATURE_COUNT = EPOINTER_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.ELocalPointerImpl <em>ELocal Pointer</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.ELocalPointerImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getELocalPointer()
   * @generated
   */
  int ELOCAL_POINTER = 4;

  /**
   * The feature id for the '<em><b>Value Number</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ELOCAL_POINTER__VALUE_NUMBER = EPOINTER_FEATURE_COUNT + 0;

  /**
   * The feature id for the '<em><b>Java Method</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ELOCAL_POINTER__JAVA_METHOD = EPOINTER_FEATURE_COUNT + 1;

  /**
   * The number of structural features of the '<em>ELocal Pointer</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ELOCAL_POINTER_FEATURE_COUNT = EPOINTER_FEATURE_COUNT + 2;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EReturnValuePointerImpl <em>EReturn Value Pointer</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EReturnValuePointerImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEReturnValuePointer()
   * @generated
   */
  int ERETURN_VALUE_POINTER = 5;

  /**
   * The feature id for the '<em><b>Is Exceptional Return Value</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ERETURN_VALUE_POINTER__IS_EXCEPTIONAL_RETURN_VALUE = EPOINTER_FEATURE_COUNT + 0;

  /**
   * The feature id for the '<em><b>Java Method</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ERETURN_VALUE_POINTER__JAVA_METHOD = EPOINTER_FEATURE_COUNT + 1;

  /**
   * The number of structural features of the '<em>EReturn Value Pointer</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ERETURN_VALUE_POINTER_FEATURE_COUNT = EPOINTER_FEATURE_COUNT + 2;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EInstanceImpl <em>EInstance</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EInstanceImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEInstance()
   * @generated
   */
  int EINSTANCE = 6;

  /**
   * The number of structural features of the '<em>EInstance</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EINSTANCE_FEATURE_COUNT = 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EJavaClassInstanceImpl <em>EJava Class Instance</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EJavaClassInstanceImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEJavaClassInstance()
   * @generated
   */
  int EJAVA_CLASS_INSTANCE = 7;

  /**
   * The feature id for the '<em><b>Java Class</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_CLASS_INSTANCE__JAVA_CLASS = EINSTANCE_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>EJava Class Instance</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_CLASS_INSTANCE_FEATURE_COUNT = EINSTANCE_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EHeapGraphImpl <em>EHeap Graph</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EHeapGraphImpl
   * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEHeapGraph()
   * @generated
   */
  int EHEAP_GRAPH = 8;

  /**
   * The feature id for the '<em><b>Nodes</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EHEAP_GRAPH__NODES = GraphPackage.EGRAPH__NODES;

  /**
   * The feature id for the '<em><b>Edges</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EHEAP_GRAPH__EDGES = GraphPackage.EGRAPH__EDGES;

  /**
   * The number of structural features of the '<em>EHeap Graph</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EHEAP_GRAPH_FEATURE_COUNT = GraphPackage.EGRAPH_FEATURE_COUNT + 0;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EPointer <em>EPointer</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EPointer</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EPointer
   * @generated
   */
  EClass getEPointer();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField <em>EInstance Field</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EInstance Field</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField
   * @generated
   */
  EClass getEInstanceField();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField#getFieldName <em>Field Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Field Name</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField#getFieldName()
   * @see #getEInstanceField()
   * @generated
   */
  EAttribute getEInstanceField_FieldName();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents <em>EArray Contents</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EArray Contents</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents
   * @generated
   */
  EClass getEArrayContents();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents#getJavaClass <em>Java Class</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Java Class</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents#getJavaClass()
   * @see #getEArrayContents()
   * @generated
   */
  EReference getEArrayContents_JavaClass();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EStaticField <em>EStatic Field</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EStatic Field</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EStaticField
   * @generated
   */
  EClass getEStaticField();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.pointerAnalysis.EStaticField#getFieldName <em>Field Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Field Name</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EStaticField#getFieldName()
   * @see #getEStaticField()
   * @generated
   */
  EAttribute getEStaticField_FieldName();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer <em>ELocal Pointer</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>ELocal Pointer</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer
   * @generated
   */
  EClass getELocalPointer();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer#getValueNumber <em>Value Number</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Value Number</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer#getValueNumber()
   * @see #getELocalPointer()
   * @generated
   */
  EAttribute getELocalPointer_ValueNumber();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer#getJavaMethod <em>Java Method</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Java Method</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer#getJavaMethod()
   * @see #getELocalPointer()
   * @generated
   */
  EReference getELocalPointer_JavaMethod();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer <em>EReturn Value Pointer</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EReturn Value Pointer</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer
   * @generated
   */
  EClass getEReturnValuePointer();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer#isIsExceptionalReturnValue <em>Is Exceptional Return Value</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Is Exceptional Return Value</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer#isIsExceptionalReturnValue()
   * @see #getEReturnValuePointer()
   * @generated
   */
  EAttribute getEReturnValuePointer_IsExceptionalReturnValue();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer#getJavaMethod <em>Java Method</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Java Method</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer#getJavaMethod()
   * @see #getEReturnValuePointer()
   * @generated
   */
  EReference getEReturnValuePointer_JavaMethod();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EInstance <em>EInstance</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EInstance</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EInstance
   * @generated
   */
  EClass getEInstance();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance <em>EJava Class Instance</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EJava Class Instance</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance
   * @generated
   */
  EClass getEJavaClassInstance();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance#getJavaClass <em>Java Class</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Java Class</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance#getJavaClass()
   * @see #getEJavaClassInstance()
   * @generated
   */
  EReference getEJavaClassInstance_JavaClass();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EHeapGraph <em>EHeap Graph</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EHeap Graph</em>'.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EHeapGraph
   * @generated
   */
  EClass getEHeapGraph();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  PointerAnalysisFactory getPointerAnalysisFactory();

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
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EPointerImpl <em>EPointer</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EPointerImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEPointer()
     * @generated
     */
    EClass EPOINTER = eINSTANCE.getEPointer();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EInstanceFieldImpl <em>EInstance Field</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EInstanceFieldImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEInstanceField()
     * @generated
     */
    EClass EINSTANCE_FIELD = eINSTANCE.getEInstanceField();

    /**
     * The meta object literal for the '<em><b>Field Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EINSTANCE_FIELD__FIELD_NAME = eINSTANCE.getEInstanceField_FieldName();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EArrayContentsImpl <em>EArray Contents</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EArrayContentsImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEArrayContents()
     * @generated
     */
    EClass EARRAY_CONTENTS = eINSTANCE.getEArrayContents();

    /**
     * The meta object literal for the '<em><b>Java Class</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference EARRAY_CONTENTS__JAVA_CLASS = eINSTANCE.getEArrayContents_JavaClass();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EStaticFieldImpl <em>EStatic Field</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EStaticFieldImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEStaticField()
     * @generated
     */
    EClass ESTATIC_FIELD = eINSTANCE.getEStaticField();

    /**
     * The meta object literal for the '<em><b>Field Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ESTATIC_FIELD__FIELD_NAME = eINSTANCE.getEStaticField_FieldName();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.ELocalPointerImpl <em>ELocal Pointer</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.ELocalPointerImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getELocalPointer()
     * @generated
     */
    EClass ELOCAL_POINTER = eINSTANCE.getELocalPointer();

    /**
     * The meta object literal for the '<em><b>Value Number</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ELOCAL_POINTER__VALUE_NUMBER = eINSTANCE.getELocalPointer_ValueNumber();

    /**
     * The meta object literal for the '<em><b>Java Method</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ELOCAL_POINTER__JAVA_METHOD = eINSTANCE.getELocalPointer_JavaMethod();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EReturnValuePointerImpl <em>EReturn Value Pointer</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EReturnValuePointerImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEReturnValuePointer()
     * @generated
     */
    EClass ERETURN_VALUE_POINTER = eINSTANCE.getEReturnValuePointer();

    /**
     * The meta object literal for the '<em><b>Is Exceptional Return Value</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ERETURN_VALUE_POINTER__IS_EXCEPTIONAL_RETURN_VALUE = eINSTANCE.getEReturnValuePointer_IsExceptionalReturnValue();

    /**
     * The meta object literal for the '<em><b>Java Method</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ERETURN_VALUE_POINTER__JAVA_METHOD = eINSTANCE.getEReturnValuePointer_JavaMethod();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EInstanceImpl <em>EInstance</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EInstanceImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEInstance()
     * @generated
     */
    EClass EINSTANCE = eINSTANCE.getEInstance();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EJavaClassInstanceImpl <em>EJava Class Instance</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EJavaClassInstanceImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEJavaClassInstance()
     * @generated
     */
    EClass EJAVA_CLASS_INSTANCE = eINSTANCE.getEJavaClassInstance();

    /**
     * The meta object literal for the '<em><b>Java Class</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference EJAVA_CLASS_INSTANCE__JAVA_CLASS = eINSTANCE.getEJavaClassInstance_JavaClass();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EHeapGraphImpl <em>EHeap Graph</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.EHeapGraphImpl
     * @see com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisPackageImpl#getEHeapGraph()
     * @generated
     */
    EClass EHEAP_GRAPH = eINSTANCE.getEHeapGraph();

  }

} //PointerAnalysisPackage
