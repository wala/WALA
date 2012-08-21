/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * WALA JDT Frontend is Copyright (c) 2008 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.cast.java.translator.jdt;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import com.ibm.wala.util.debug.Assertions;

/**
 * This is a hack to get around the fact that AST.resolveWellKnownTypes() doesn't know about some implicitly declared exceptions,
 * such as ArithmeticException (implicitly thrown in a division operation) and NullPointerException (implicitly thrown in a field
 * access). We need to know the lineage of these types to determine possible catch targets.
 * 
 * @author evan
 * 
 */
public class FakeExceptionTypeBinding implements ITypeBinding {

  static public final FakeExceptionTypeBinding arithmetic = new FakeExceptionTypeBinding("Ljava/lang/ArithmeticException;");

  static public final FakeExceptionTypeBinding nullPointer = new FakeExceptionTypeBinding("Ljava/lang/NullPointerException;");

  static public final FakeExceptionTypeBinding classCast = new FakeExceptionTypeBinding("Ljava/lang/ClassCastException;");

  static public final FakeExceptionTypeBinding noClassDef = new FakeExceptionTypeBinding("Ljava/lang/NoClassDefFoundError;");

  static public final FakeExceptionTypeBinding initException = new FakeExceptionTypeBinding(
      "Ljava/lang/ExceptionInInitializerError;");

  static public final FakeExceptionTypeBinding outOfMemory = new FakeExceptionTypeBinding("Ljava/lang/OutOfMemoryError;");

  private final String exceptionBinaryName;

  private FakeExceptionTypeBinding(String exceptionBinaryName) {
    this.exceptionBinaryName = exceptionBinaryName;
  }

  public boolean isAssignmentCompatible(ITypeBinding variableType) {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean equals(Object o) {
    if (o instanceof FakeExceptionTypeBinding)
      return this == o;
    if (o instanceof ITypeBinding)
      return ((ITypeBinding) o).getBinaryName().equals(exceptionBinaryName);
    return false;
  }

  // --- rest not needed

  public ITypeBinding createArrayType(int dimension) {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding createArrayType");
    return null;
  }

  public String getBinaryName() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding getBound() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding getComponentType() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public IVariableBinding[] getDeclaredFields() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public IMethodBinding[] getDeclaredMethods() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public int getDeclaredModifiers() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return 0;
  }

  public ITypeBinding[] getDeclaredTypes() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding getDeclaringClass() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public IMethodBinding getDeclaringMethod() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public int getDimensions() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return 0;
  }

  public ITypeBinding getElementType() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding getErasure() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding[] getInterfaces() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public int getModifiers() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return 0;
  }

  public String getName() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public IPackageBinding getPackage() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public String getQualifiedName() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding getSuperclass() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding[] getTypeArguments() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding[] getTypeBounds() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding getTypeDeclaration() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding[] getTypeParameters() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public ITypeBinding getWildcard() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public boolean isAnnotation() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isAnonymous() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isArray() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isCapture() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isCastCompatible(ITypeBinding type) {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isClass() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isEnum() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isFromSource() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isGenericType() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isInterface() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isLocal() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isMember() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isNested() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isNullType() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isParameterizedType() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isPrimitive() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isRawType() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isSubTypeCompatible(ITypeBinding type) {
    String name = type.getBinaryName();
    if (exceptionBinaryName.endsWith("Error;")) {
      if (name.equals("Ljava/lang/Throwable;") || name.equals("Ljava/lang/Error;") || name.equals(exceptionBinaryName)) {
        return true;
      }

    } else {
      if (name.equals("Ljava/lang/Throwable;") || name.equals("Ljava/lang/Exception;")
          || name.equals("Ljava/lang/RuntimeException;") || name.equals(exceptionBinaryName)) {
        return true;
      }
    }

    return false;
  }

  public boolean isTopLevel() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isTypeVariable() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isUpperbound() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isWildcardType() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public IAnnotationBinding[] getAnnotations() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public IJavaElement getJavaElement() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public String getKey() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public int getKind() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return 0;
  }

  public boolean isDeprecated() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isEqualTo(IBinding binding) {
    return this.equals(binding);
  }

  public boolean isRecovered() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public boolean isSynthetic() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return false;
  }

  public ITypeBinding getGenericTypeOfWildcardType() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return null;
  }

  public int getRank() {
    Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
    return 0;
  }

}
