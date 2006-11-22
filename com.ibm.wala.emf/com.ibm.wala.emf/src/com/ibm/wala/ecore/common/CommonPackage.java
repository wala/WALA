/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
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
 * @see com.ibm.wala.ecore.common.CommonFactory
 * @model kind="package"
 * @generated
 */
public interface CommonPackage extends EPackage {
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "common";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.common";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.common";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  CommonPackage eINSTANCE = com.ibm.wala.ecore.common.impl.CommonPackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.common.ECollection <em>ECollection</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.common.ECollection
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getECollection()
   * @generated
   */
  int ECOLLECTION = 0;

  /**
   * The feature id for the '<em><b>Contents</b></em>' reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECOLLECTION__CONTENTS = 0;

  /**
   * The number of structural features of the '<em>ECollection</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECOLLECTION_FEATURE_COUNT = 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.common.impl.EPairImpl <em>EPair</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.common.impl.EPairImpl
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEPair()
   * @generated
   */
  int EPAIR = 1;

  /**
   * The feature id for the '<em><b>X</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPAIR__X = 0;

  /**
   * The feature id for the '<em><b>Y</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPAIR__Y = 1;

  /**
   * The number of structural features of the '<em>EPair</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EPAIR_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.common.impl.EContainerImpl <em>EContainer</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.common.impl.EContainerImpl
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEContainer()
   * @generated
   */
  int ECONTAINER = 3;

  /**
   * The feature id for the '<em><b>Contents</b></em>' reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECONTAINER__CONTENTS = ECOLLECTION__CONTENTS;

  /**
   * The feature id for the '<em><b>Containees</b></em>' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECONTAINER__CONTAINEES = ECOLLECTION_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>EContainer</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECONTAINER_FEATURE_COUNT = ECOLLECTION_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.common.impl.ERelationImpl <em>ERelation</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.common.impl.ERelationImpl
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getERelation()
   * @generated
   */
  int ERELATION = 2;

  /**
   * The feature id for the '<em><b>Contents</b></em>' reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ERELATION__CONTENTS = ECONTAINER__CONTENTS;

  /**
   * The feature id for the '<em><b>Containees</b></em>' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ERELATION__CONTAINEES = ECONTAINER__CONTAINEES;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ERELATION__NAME = ECONTAINER_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>ERelation</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ERELATION_FEATURE_COUNT = ECONTAINER_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.common.impl.ENotContainerImpl <em>ENot Container</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.common.impl.ENotContainerImpl
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getENotContainer()
   * @generated
   */
  int ENOT_CONTAINER = 4;

  /**
   * The feature id for the '<em><b>Contents</b></em>' reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ENOT_CONTAINER__CONTENTS = ECOLLECTION__CONTENTS;

  /**
   * The feature id for the '<em><b>Elements</b></em>' reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ENOT_CONTAINER__ELEMENTS = ECOLLECTION_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>ENot Container</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ENOT_CONTAINER_FEATURE_COUNT = ECOLLECTION_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.common.impl.EStringHolderImpl <em>EString Holder</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.common.impl.EStringHolderImpl
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEStringHolder()
   * @generated
   */
  int ESTRING_HOLDER = 5;

  /**
   * The feature id for the '<em><b>Value</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ESTRING_HOLDER__VALUE = 0;

  /**
   * The number of structural features of the '<em>EString Holder</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ESTRING_HOLDER_FEATURE_COUNT = 1;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.common.impl.EObjectWithContainerIdImpl <em>EObject With Container Id</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.common.impl.EObjectWithContainerIdImpl
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEObjectWithContainerId()
   * @generated
   */
  int EOBJECT_WITH_CONTAINER_ID = 6;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EOBJECT_WITH_CONTAINER_ID__ID = 0;

  /**
   * The number of structural features of the '<em>EObject With Container Id</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT = 1;

  /**
   * The meta object id for the '<em>EJava Collection</em>' data type.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see java.util.Collection
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEJavaCollection()
   * @generated
   */
  int EJAVA_COLLECTION = 7;

  /**
   * The meta object id for the '<em>EFile</em>' data type.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see java.io.File
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEFile()
   * @generated
   */
  int EFILE = 8;

  /**
   * The meta object id for the '<em>EIterator</em>' data type.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see java.util.Iterator
   * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEIterator()
   * @generated
   */
  int EITERATOR = 9;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.common.ECollection <em>ECollection</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>ECollection</em>'.
   * @see com.ibm.wala.ecore.common.ECollection
   * @generated
   */
  EClass getECollection();

  /**
   * Returns the meta object for the reference list '{@link com.ibm.wala.ecore.common.ECollection#getContents <em>Contents</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference list '<em>Contents</em>'.
   * @see com.ibm.wala.ecore.common.ECollection#getContents()
   * @see #getECollection()
   * @generated
   */
  EReference getECollection_Contents();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.common.EPair <em>EPair</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EPair</em>'.
   * @see com.ibm.wala.ecore.common.EPair
   * @generated
   */
  EClass getEPair();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.common.EPair#getX <em>X</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>X</em>'.
   * @see com.ibm.wala.ecore.common.EPair#getX()
   * @see #getEPair()
   * @generated
   */
  EReference getEPair_X();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.common.EPair#getY <em>Y</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Y</em>'.
   * @see com.ibm.wala.ecore.common.EPair#getY()
   * @see #getEPair()
   * @generated
   */
  EReference getEPair_Y();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.common.ERelation <em>ERelation</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>ERelation</em>'.
   * @see com.ibm.wala.ecore.common.ERelation
   * @generated
   */
  EClass getERelation();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.common.ERelation#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see com.ibm.wala.ecore.common.ERelation#getName()
   * @see #getERelation()
   * @generated
   */
  EAttribute getERelation_Name();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.common.EContainer <em>EContainer</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EContainer</em>'.
   * @see com.ibm.wala.ecore.common.EContainer
   * @generated
   */
  EClass getEContainer();

  /**
   * Returns the meta object for the containment reference list '{@link com.ibm.wala.ecore.common.EContainer#getContainees <em>Containees</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference list '<em>Containees</em>'.
   * @see com.ibm.wala.ecore.common.EContainer#getContainees()
   * @see #getEContainer()
   * @generated
   */
  EReference getEContainer_Containees();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.common.ENotContainer <em>ENot Container</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>ENot Container</em>'.
   * @see com.ibm.wala.ecore.common.ENotContainer
   * @generated
   */
  EClass getENotContainer();

  /**
   * Returns the meta object for the reference list '{@link com.ibm.wala.ecore.common.ENotContainer#getElements <em>Elements</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference list '<em>Elements</em>'.
   * @see com.ibm.wala.ecore.common.ENotContainer#getElements()
   * @see #getENotContainer()
   * @generated
   */
  EReference getENotContainer_Elements();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.common.EStringHolder <em>EString Holder</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EString Holder</em>'.
   * @see com.ibm.wala.ecore.common.EStringHolder
   * @generated
   */
  EClass getEStringHolder();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.common.EStringHolder#getValue <em>Value</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Value</em>'.
   * @see com.ibm.wala.ecore.common.EStringHolder#getValue()
   * @see #getEStringHolder()
   * @generated
   */
  EAttribute getEStringHolder_Value();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.common.EObjectWithContainerId <em>EObject With Container Id</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EObject With Container Id</em>'.
   * @see com.ibm.wala.ecore.common.EObjectWithContainerId
   * @generated
   */
  EClass getEObjectWithContainerId();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.common.EObjectWithContainerId#getId <em>Id</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see com.ibm.wala.ecore.common.EObjectWithContainerId#getId()
   * @see #getEObjectWithContainerId()
   * @generated
   */
  EAttribute getEObjectWithContainerId_Id();

  /**
   * Returns the meta object for data type '{@link java.util.Collection <em>EJava Collection</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for data type '<em>EJava Collection</em>'.
   * @see java.util.Collection
   * @model instanceClass="java.util.Collection" serializable="false"
   * @generated
   */
  EDataType getEJavaCollection();

  /**
   * Returns the meta object for data type '{@link java.io.File <em>EFile</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for data type '<em>EFile</em>'.
   * @see java.io.File
   * @model instanceClass="java.io.File" serializable="false"
   * @generated
   */
  EDataType getEFile();

  /**
   * Returns the meta object for data type '{@link java.util.Iterator <em>EIterator</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for data type '<em>EIterator</em>'.
   * @see java.util.Iterator
   * @model instanceClass="java.util.Iterator" serializable="false"
   * @generated
   */
  EDataType getEIterator();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  CommonFactory getCommonFactory();

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
     * The meta object literal for the '{@link com.ibm.wala.ecore.common.ECollection <em>ECollection</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.common.ECollection
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getECollection()
     * @generated
     */
    EClass ECOLLECTION = eINSTANCE.getECollection();

    /**
     * The meta object literal for the '<em><b>Contents</b></em>' reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ECOLLECTION__CONTENTS = eINSTANCE.getECollection_Contents();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.common.impl.EPairImpl <em>EPair</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.common.impl.EPairImpl
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEPair()
     * @generated
     */
    EClass EPAIR = eINSTANCE.getEPair();

    /**
     * The meta object literal for the '<em><b>X</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference EPAIR__X = eINSTANCE.getEPair_X();

    /**
     * The meta object literal for the '<em><b>Y</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference EPAIR__Y = eINSTANCE.getEPair_Y();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.common.impl.ERelationImpl <em>ERelation</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.common.impl.ERelationImpl
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getERelation()
     * @generated
     */
    EClass ERELATION = eINSTANCE.getERelation();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ERELATION__NAME = eINSTANCE.getERelation_Name();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.common.impl.EContainerImpl <em>EContainer</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.common.impl.EContainerImpl
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEContainer()
     * @generated
     */
    EClass ECONTAINER = eINSTANCE.getEContainer();

    /**
     * The meta object literal for the '<em><b>Containees</b></em>' containment reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ECONTAINER__CONTAINEES = eINSTANCE.getEContainer_Containees();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.common.impl.ENotContainerImpl <em>ENot Container</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.common.impl.ENotContainerImpl
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getENotContainer()
     * @generated
     */
    EClass ENOT_CONTAINER = eINSTANCE.getENotContainer();

    /**
     * The meta object literal for the '<em><b>Elements</b></em>' reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ENOT_CONTAINER__ELEMENTS = eINSTANCE.getENotContainer_Elements();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.common.impl.EStringHolderImpl <em>EString Holder</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.common.impl.EStringHolderImpl
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEStringHolder()
     * @generated
     */
    EClass ESTRING_HOLDER = eINSTANCE.getEStringHolder();

    /**
     * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ESTRING_HOLDER__VALUE = eINSTANCE.getEStringHolder_Value();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.common.impl.EObjectWithContainerIdImpl <em>EObject With Container Id</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.common.impl.EObjectWithContainerIdImpl
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEObjectWithContainerId()
     * @generated
     */
    EClass EOBJECT_WITH_CONTAINER_ID = eINSTANCE.getEObjectWithContainerId();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EOBJECT_WITH_CONTAINER_ID__ID = eINSTANCE.getEObjectWithContainerId_Id();

    /**
     * The meta object literal for the '<em>EJava Collection</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see java.util.Collection
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEJavaCollection()
     * @generated
     */
    EDataType EJAVA_COLLECTION = eINSTANCE.getEJavaCollection();

    /**
     * The meta object literal for the '<em>EFile</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see java.io.File
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEFile()
     * @generated
     */
    EDataType EFILE = eINSTANCE.getEFile();

    /**
     * The meta object literal for the '<em>EIterator</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see java.util.Iterator
     * @see com.ibm.wala.ecore.common.impl.CommonPackageImpl#getEIterator()
     * @generated
     */
    EDataType EITERATOR = eINSTANCE.getEIterator();

  }

} //CommonPackage
