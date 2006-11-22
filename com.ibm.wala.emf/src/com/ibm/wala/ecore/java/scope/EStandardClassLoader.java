/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEnumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>EStandard Class Loader</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEStandardClassLoader()
 * @model
 * @generated
 */
public final class EStandardClassLoader extends AbstractEnumerator {
  /**
   * The '<em><b>Primordial</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Primordial</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #PRIMORDIAL_LITERAL
   * @model name="Primordial"
   * @generated
   * @ordered
   */
  public static final int PRIMORDIAL = 0;

  /**
   * The '<em><b>Extension</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Extension</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #EXTENSION_LITERAL
   * @model name="Extension"
   * @generated
   * @ordered
   */
  public static final int EXTENSION = 1;

  /**
   * The '<em><b>Application</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Application</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #APPLICATION_LITERAL
   * @model name="Application"
   * @generated
   * @ordered
   */
  public static final int APPLICATION = 2;

  /**
   * The '<em><b>Synthetic</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Synthetic</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #SYNTHETIC_LITERAL
   * @model name="Synthetic"
   * @generated
   * @ordered
   */
  public static final int SYNTHETIC = 4;

  /**
   * The '<em><b>Primordial</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #PRIMORDIAL
   * @generated
   * @ordered
   */
  public static final EStandardClassLoader PRIMORDIAL_LITERAL = new EStandardClassLoader(PRIMORDIAL, "Primordial", "Primordial");

  /**
   * The '<em><b>Extension</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #EXTENSION
   * @generated
   * @ordered
   */
  public static final EStandardClassLoader EXTENSION_LITERAL = new EStandardClassLoader(EXTENSION, "Extension", "Extension");

  /**
   * The '<em><b>Application</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #APPLICATION
   * @generated
   * @ordered
   */
  public static final EStandardClassLoader APPLICATION_LITERAL = new EStandardClassLoader(APPLICATION, "Application", "Application");

  /**
   * The '<em><b>Synthetic</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #SYNTHETIC
   * @generated
   * @ordered
   */
  public static final EStandardClassLoader SYNTHETIC_LITERAL = new EStandardClassLoader(SYNTHETIC, "Synthetic", "Synthetic");

  /**
   * An array of all the '<em><b>EStandard Class Loader</b></em>' enumerators.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private static final EStandardClassLoader[] VALUES_ARRAY =
    new EStandardClassLoader[] {
      PRIMORDIAL_LITERAL,
      EXTENSION_LITERAL,
      APPLICATION_LITERAL,
      SYNTHETIC_LITERAL,
    };

  /**
   * A public read-only list of all the '<em><b>EStandard Class Loader</b></em>' enumerators.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

  /**
   * Returns the '<em><b>EStandard Class Loader</b></em>' literal with the specified literal value.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EStandardClassLoader get(String literal) {
    for (int i = 0; i < VALUES_ARRAY.length; ++i) {
      EStandardClassLoader result = VALUES_ARRAY[i];
      if (result.toString().equals(literal)) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the '<em><b>EStandard Class Loader</b></em>' literal with the specified name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EStandardClassLoader getByName(String name) {
    for (int i = 0; i < VALUES_ARRAY.length; ++i) {
      EStandardClassLoader result = VALUES_ARRAY[i];
      if (result.getName().equals(name)) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the '<em><b>EStandard Class Loader</b></em>' literal with the specified integer value.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EStandardClassLoader get(int value) {
    switch (value) {
      case PRIMORDIAL: return PRIMORDIAL_LITERAL;
      case EXTENSION: return EXTENSION_LITERAL;
      case APPLICATION: return APPLICATION_LITERAL;
      case SYNTHETIC: return SYNTHETIC_LITERAL;
    }
    return null;	
  }

  /**
   * Only this class can construct instances.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EStandardClassLoader(int value, String name, String literal) {
    super(value, name, literal);
  }

} //EStandardClassLoader
