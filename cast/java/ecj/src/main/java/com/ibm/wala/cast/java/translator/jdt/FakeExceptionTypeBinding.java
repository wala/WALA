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

import com.ibm.wala.util.debug.Assertions;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * This is a hack to get around the fact that AST.resolveWellKnownTypes() doesn't know about some
 * implicitly declared exceptions, such as ArithmeticException (implicitly thrown in a division
 * operation) and NullPointerException (implicitly thrown in a field access). We need to know the
 * lineage of these types to determine possible catch targets.
 *
 * @author evan
 */
public class FakeExceptionTypeBinding implements ITypeBinding {

  public static final FakeExceptionTypeBinding arithmetic =
      new FakeExceptionTypeBinding("Ljava/lang/ArithmeticException;");

  public static final FakeExceptionTypeBinding nullPointer =
      new FakeExceptionTypeBinding("Ljava/lang/NullPointerException;");

  public static final FakeExceptionTypeBinding classCast =
      new FakeExceptionTypeBinding("Ljava/lang/ClassCastException;");

  public static final FakeExceptionTypeBinding noClassDef =
      new FakeExceptionTypeBinding("Ljava/lang/NoClassDefFoundError;");

  public static final FakeExceptionTypeBinding initException =
      new FakeExceptionTypeBinding("Ljava/lang/ExceptionInInitializerError;");

  public static final FakeExceptionTypeBinding outOfMemory =
      new FakeExceptionTypeBinding("Ljava/lang/OutOfMemoryError;");

  private final String exceptionBinaryName;

  private FakeExceptionTypeBinding(String exceptionBinaryName) {
    this.exceptionBinaryName = exceptionBinaryName;
  }

  @Override
  public boolean isAssignmentCompatible(ITypeBinding variableType) {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof FakeExceptionTypeBinding) return this == o;
    if (o instanceof ITypeBinding iTypeBinding)
      return iTypeBinding.getBinaryName().equals(exceptionBinaryName);
    return false;
  }

  @Override
  public int hashCode() {
    return exceptionBinaryName.hashCode();
  }

  // --- rest not needed

  @Override
  public ITypeBinding createArrayType(int dimension) {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding createArrayType");
  }

  @Override
  public String getBinaryName() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getBound() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getComponentType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public IVariableBinding[] getDeclaredFields() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public IMethodBinding[] getDeclaredMethods() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @SuppressWarnings("deprecation")
  @Override
  public int getDeclaredModifiers() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding[] getDeclaredTypes() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getDeclaringClass() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public IMethodBinding getDeclaringMethod() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public int getDimensions() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getElementType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getErasure() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding[] getInterfaces() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public int getModifiers() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public String getName() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public IPackageBinding getPackage() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public String getQualifiedName() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getSuperclass() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding[] getTypeArguments() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding[] getTypeBounds() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getTypeDeclaration() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding[] getTypeParameters() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getWildcard() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isAnnotation() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isAnonymous() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isArray() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isCapture() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isCastCompatible(ITypeBinding type) {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isClass() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isEnum() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isRecord() {
    return false;
  }

  @Override
  public boolean isFromSource() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isGenericType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isInterface() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isIntersectionType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isLocal() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isMember() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isNested() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isNullType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isParameterizedType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isPrimitive() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isRawType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isSubTypeCompatible(ITypeBinding type) {
    String name = type.getBinaryName();
    if (exceptionBinaryName.endsWith("Error;")) {
      return switch (name) {
        case "Ljava/lang/Error;", "Ljava/lang/Throwable;" -> true;
        default -> name.equals(exceptionBinaryName);
      };

    } else {
      return switch (name) {
        case "Ljava/lang/Exception;", "Ljava/lang/RuntimeException;", "Ljava/lang/Throwable;" ->
            true;
        default -> name.equals(exceptionBinaryName);
      };
    }
  }

  @Override
  public boolean isTopLevel() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isTypeVariable() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isUpperbound() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isWildcardType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public IAnnotationBinding[] getAnnotations() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public IJavaElement getJavaElement() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public String getKey() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public int getKind() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isDeprecated() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isEqualTo(IBinding binding) {
    return this.equals(binding);
  }

  @Override
  public boolean isRecovered() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public boolean isSynthetic() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public ITypeBinding getGenericTypeOfWildcardType() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public int getRank() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  // do not put @Override here, to avoid breaking compilation on Juno
  @Override
  public IMethodBinding getFunctionalInterfaceMethod() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  // do not put @Override here, to avoid breaking compilation on Juno
  @Override
  public IAnnotationBinding[] getTypeAnnotations() {
    return Assertions.UNREACHABLE("FakeExceptionTypeBinding ");
  }

  @Override
  public IBinding getDeclaringMember() {
    // TODO Auto-generated method stub
    return null;
  }
}
