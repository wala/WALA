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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSymbol;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.util.debug.Assertions;

public class JDT2CAstUtils {
  public static Collection<CAstQualifier> mapModifiersToQualifiers(int modifiers, boolean isInterface, boolean isAnnotation) {
    Set<CAstQualifier> quals = new LinkedHashSet<>();

    if (isInterface)
      quals.add(CAstQualifier.INTERFACE);

    if (isAnnotation)
      quals.add(CAstQualifier.ANNOTATION);
    
    if ((modifiers & Modifier.ABSTRACT) != 0)
      quals.add(CAstQualifier.ABSTRACT);
    if ((modifiers & Modifier.FINAL) != 0)
      quals.add(CAstQualifier.FINAL);
    if ((modifiers & Modifier.NATIVE) != 0)
      quals.add(CAstQualifier.NATIVE);
    // if (flags.isPackage()) quals.add(CAstQualifier.PACKAGE);
    if ((modifiers & Modifier.PRIVATE) != 0)
      quals.add(CAstQualifier.PRIVATE);
    if ((modifiers & Modifier.PROTECTED) != 0)
      quals.add(CAstQualifier.PROTECTED);
    if ((modifiers & Modifier.PUBLIC) != 0)
      quals.add(CAstQualifier.PUBLIC);
    if ((modifiers & Modifier.STATIC) != 0)
      quals.add(CAstQualifier.STATIC);
    if ((modifiers & Modifier.STRICTFP) != 0)
      quals.add(CAstQualifier.STRICTFP);
    if ((modifiers & Modifier.SYNCHRONIZED) != 0)
      quals.add(CAstQualifier.SYNCHRONIZED);
    if ((modifiers & Modifier.TRANSIENT) != 0)
      quals.add(CAstQualifier.TRANSIENT);
    if ((modifiers & Modifier.VOLATILE) != 0)
      quals.add(CAstQualifier.VOLATILE);

    return quals;
  }

  public static CAstOperator mapAssignOperator(Operator op) {
    if (op == Assignment.Operator.PLUS_ASSIGN)
      return CAstOperator.OP_ADD;
    else if (op == Assignment.Operator.BIT_AND_ASSIGN)
      return CAstOperator.OP_BIT_AND;
    else if (op == Assignment.Operator.BIT_OR_ASSIGN)
      return CAstOperator.OP_BIT_OR;
    else if (op == Assignment.Operator.BIT_XOR_ASSIGN)
      return CAstOperator.OP_BIT_XOR;
    else if (op == Assignment.Operator.DIVIDE_ASSIGN)
      return CAstOperator.OP_DIV;
    else if (op == Assignment.Operator.REMAINDER_ASSIGN)
      return CAstOperator.OP_MOD;
    else if (op == Assignment.Operator.TIMES_ASSIGN)
      return CAstOperator.OP_MUL;
    else if (op == Assignment.Operator.LEFT_SHIFT_ASSIGN)
      return CAstOperator.OP_LSH;
    else if (op == Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)
      return CAstOperator.OP_RSH;
    else if (op == Assignment.Operator.MINUS_ASSIGN)
      return CAstOperator.OP_SUB;
    else if (op == Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)
      return CAstOperator.OP_URSH;
    Assertions.UNREACHABLE("Unknown assignment operator");
    return null;
  }

  protected static CAstOperator mapBinaryOpcode(InfixExpression.Operator operator) {
    if (operator == InfixExpression.Operator.PLUS)
      return CAstOperator.OP_ADD;
    // separate bitwise and logical AND / OR ? '&' / '|' ?
    if (operator == InfixExpression.Operator.AND)
      return CAstOperator.OP_BIT_AND;
    if (operator == InfixExpression.Operator.OR)
      return CAstOperator.OP_BIT_OR;
    if (operator == InfixExpression.Operator.XOR)
      return CAstOperator.OP_BIT_XOR;

    // TODO: shouldn't get here (conditional and handled differently); however should separate bitwise & logical '&' / '|', maybe.
    if (operator == InfixExpression.Operator.CONDITIONAL_AND)
      return CAstOperator.OP_REL_AND;
    if (operator == InfixExpression.Operator.CONDITIONAL_OR)
      return CAstOperator.OP_REL_OR;

    if (operator == InfixExpression.Operator.DIVIDE)
      return CAstOperator.OP_DIV;
    if (operator == InfixExpression.Operator.EQUALS)
      return CAstOperator.OP_EQ;
    if (operator == InfixExpression.Operator.GREATER_EQUALS)
      return CAstOperator.OP_GE;
    if (operator == InfixExpression.Operator.GREATER)
      return CAstOperator.OP_GT;
    if (operator == InfixExpression.Operator.LESS_EQUALS)
      return CAstOperator.OP_LE;
    if (operator == InfixExpression.Operator.LESS)
      return CAstOperator.OP_LT;
    if (operator == InfixExpression.Operator.REMAINDER)
      return CAstOperator.OP_MOD;
    if (operator == InfixExpression.Operator.TIMES)
      return CAstOperator.OP_MUL;
    if (operator == InfixExpression.Operator.NOT_EQUALS)
      return CAstOperator.OP_NE;
    if (operator == InfixExpression.Operator.LEFT_SHIFT)
      return CAstOperator.OP_LSH;
    if (operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED)
      return CAstOperator.OP_RSH;
    if (operator == InfixExpression.Operator.MINUS)
      return CAstOperator.OP_SUB;
    if (operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)
      return CAstOperator.OP_URSH;
    Assertions.UNREACHABLE("Java2CAstTranslator.JavaTranslatingVisitorImpl.mapBinaryOpcode(): unrecognized binary operator.");
    return null;
  }

  /**
   * Returns true if type is char, byte, short, int, or long. Return false otherwise (including boolean!)
   * 
   * @param type
   */
  public static boolean isLongOrLess(ITypeBinding type) {
    String t = type.getBinaryName();
    return t.equals("C") || t.equals("B") || t.equals("S") || t.equals("I") || t.equals("J");
  }

  /**
   * If isLongOrLess(type), returns Integer(0). If a float or double, returns Double(0.0) Otherwise (including boolean), returns
   * CAstSymbol.NULL_DEFAULT_VALUE.
   * 
   * @param type
   */
  public static Object defaultValueForType(ITypeBinding type) {
    if (isLongOrLess(type))
      return new Integer(0);
    else if (type.getBinaryName().equals("D") || type.getBinaryName().equals("F"))
      return new Double(0.0);
    else
      return CAstSymbol.NULL_DEFAULT_VALUE;
  }

  public static ITypeBinding promoteTypes(ITypeBinding t1, ITypeBinding t2, AST ast) {
    // JLS 5.6.2
    ITypeBinding doble = ast.resolveWellKnownType("double");
    if (t1.equals(doble) || t2.equals(doble))
      return doble;
    ITypeBinding flotando = ast.resolveWellKnownType("float");
    if (t1.equals(flotando) || t2.equals(flotando))
      return flotando;
    ITypeBinding largo = ast.resolveWellKnownType("long");
    if (t1.equals(largo) || t2.equals(largo))
      return largo;
    return ast.resolveWellKnownType("int");
  }

  public static ITypeBinding getDeclaringClassOfNode(ASTNode n) {
    ASTNode current = n;
    while (current != null) {
      if (current instanceof TypeDeclaration)
        return ((TypeDeclaration) current).resolveBinding();
      else if (current instanceof AnonymousClassDeclaration)
        return ((AnonymousClassDeclaration) current).resolveBinding();
      else if (current instanceof EnumDeclaration)
        return ((EnumDeclaration) current).resolveBinding();
      current = current.getParent();
    }
    Assertions.UNREACHABLE("Couldn't find declaring class of node");
    return null;
  }

  static String anonTypeName(ITypeBinding ct) {
    String binName = ct.getBinaryName();
    String dollarSignNumber = binName.substring(binName.indexOf('$'));
    return "<anonymous subclass of " + ct.getSuperclass().getBinaryName() + ">" + dollarSignNumber;
  }

  /**
   * If a type variable, return the bound (getTypeVariablesBase()). If a parameterized type, return the generic type.
   * 
   * @param returnType
   * @param ast
   */
  public static ITypeBinding getErasedType(ITypeBinding returnType, AST ast) {
    if (returnType.isTypeVariable() || returnType.isCapture())
      return getTypesVariablesBase(returnType, ast);
    return returnType.getTypeDeclaration(); // Things like "Collection<? extends Bla>" are parameterized types...
  }

  public static ITypeBinding getTypesVariablesBase(ITypeBinding returnType, AST ast) {
    assert returnType.isTypeVariable() || returnType.isCapture();
    if (returnType.getTypeBounds().length > 0)
      return returnType.getTypeBounds()[0]; // TODO: why is there more than one bound?
    else
      return ast.resolveWellKnownType("java.lang.Object");
  }

  public static InfixExpression.Operator mapAssignOperatorToInfixOperator(Assignment.Operator op) {
    if (op == Assignment.Operator.PLUS_ASSIGN)
      return InfixExpression.Operator.PLUS;
    else if (op == Assignment.Operator.BIT_AND_ASSIGN)
      return InfixExpression.Operator.AND;
    else if (op == Assignment.Operator.BIT_OR_ASSIGN)
      return InfixExpression.Operator.OR;
    else if (op == Assignment.Operator.BIT_XOR_ASSIGN)
      return InfixExpression.Operator.XOR;
    else if (op == Assignment.Operator.DIVIDE_ASSIGN)
      return InfixExpression.Operator.DIVIDE;
    else if (op == Assignment.Operator.REMAINDER_ASSIGN)
      return InfixExpression.Operator.REMAINDER;
    else if (op == Assignment.Operator.TIMES_ASSIGN)
      return InfixExpression.Operator.TIMES;
    else if (op == Assignment.Operator.LEFT_SHIFT_ASSIGN)
      return InfixExpression.Operator.LEFT_SHIFT;
    else if (op == Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)
      return InfixExpression.Operator.RIGHT_SHIFT_SIGNED;
    else if (op == Assignment.Operator.MINUS_ASSIGN)
      return InfixExpression.Operator.MINUS;
    else if (op == Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)
      return InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;
    Assertions.UNREACHABLE("Unknown assignment operator");
    return null;
  }

  private static void getMethodInClassOrSuperclass(IMethodBinding met, ITypeBinding klass, boolean superclassonly,
      HashMap<ITypeBinding, IMethodBinding> overridden) {
    if (!superclassonly) {
      for (IMethodBinding ourmet : klass.getDeclaredMethods())
        if (met.overrides(ourmet)) {
          overridden.put(ourmet.getMethodDeclaration().getReturnType(), ourmet.getMethodDeclaration());
          break; // there can only be one per class so don't bother looking for more
        }
    }

    for (ITypeBinding iface : klass.getInterfaces())
      getMethodInClassOrSuperclass(met, iface, false, overridden);

    ITypeBinding superclass = klass.getSuperclass();
    if (superclass != null)
      getMethodInClassOrSuperclass(met, superclass, false, overridden);
  }

  public static Collection<IMethodBinding> getOverriddenMethod(IMethodBinding met) {
    HashMap<ITypeBinding, IMethodBinding> overridden = new HashMap<>();
    if (met == null)
      return null;
    getMethodInClassOrSuperclass(met, met.getDeclaringClass(), true, overridden);
    if (overridden.size() == 0)
      return null;
    return overridden.values();
  }

  public static boolean sameErasedSignatureAndReturnType(IMethodBinding met1, IMethodBinding met2) {
    if (!met1.getReturnType().getErasure().isEqualTo(met2.getReturnType().getErasure()))
      return false;

    ITypeBinding[] params1 = met1.getParameterTypes();
    ITypeBinding[] params2 = met2.getParameterTypes();
    if (params1.length != params2.length)
      return false;

    for (int i = 0; i < params1.length; i++)
      if (!params1[i].getErasure().isEqualTo(params2[i].getErasure()))
        return false;

    return true;
  }

}
