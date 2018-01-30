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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ITypeBinding;

import com.ibm.wala.cast.java.types.JavaPrimitiveTypeMap;
import com.ibm.wala.cast.java.types.JavaType;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.util.debug.Assertions;

public class JDTTypeDictionary extends CAstTypeDictionaryImpl<ITypeBinding> {

  // TODO: better way of getting type "ObjecT" that doesn't require us to keep AST? although this is similar to
  // polyglot.
  protected final AST fAst; // TAGALONG

  protected final JDTIdentityMapper fIdentityMapper; // TAGALONG

  /**
   * 
   * @param ast Needed to get root type "java.lang.Object"
   */
  public JDTTypeDictionary(AST ast, JDTIdentityMapper identityMapper) {
    fAst = ast;
    fIdentityMapper = identityMapper;
  }

  @Override
  public CAstType getCAstTypeFor(Object astType) {

    ITypeBinding jdtType = JDT2CAstUtils.getErasedType((ITypeBinding) astType, fAst);

    CAstType type = super.getCAstTypeFor(astType); // check cache first
    // Handle the case where we haven't seen an AST decl for some type before
    // processing a reference. This can certainly happen with classes in byte-
    // code libraries, for which we never see an AST decl.
    // In this case, just create a new type and return that.
    if (type == null) {

      if (jdtType.isClass() || jdtType.isEnum() || jdtType.isInterface()) // in JDT interfaces are not classes
        type = new JdtJavaType(jdtType);
      else if (jdtType.isPrimitive()) {
        type = JavaPrimitiveTypeMap.lookupType(jdtType.getName());
      } else if (jdtType.isArray()) {
        type = new JdtJavaArrayType(jdtType);
      } else
        Assertions.UNREACHABLE("getCAstTypeFor() passed type that is not primitive, array, or class?");
      super.map((ITypeBinding)astType, type); // put in cache
    }
    return type;
  }

  private final class JdtJavaArrayType implements CAstType.Array {
    private final ITypeBinding fEltJdtType;

    private final CAstType fEltCAstType;

    private JdtJavaArrayType(ITypeBinding arrayType) {
      super();
      fEltJdtType = arrayType.getComponentType();
      fEltCAstType = getCAstTypeFor(fEltJdtType);
    }

    @Override
    public int getNumDimensions() {
      return 1; // always 1 for Java
    }

    @Override
    public CAstType getElementType() {
      return fEltCAstType;
    }

    @Override
    public String getName() {
      return "[" + fEltCAstType.getName();
    }

    @Override
    public Collection<CAstType> getSupertypes() {
      if (fEltJdtType.isPrimitive())
        return Collections.singleton(getCAstTypeFor(fAst.resolveWellKnownType("java.lang.Object")));
      // TODO: there is no '.isReference()' as in Polyglot: is this right? enum? I think if it's another array it will
      // just ignore it
      // TEST DOUBLE ARRAYS! and maybe ask someone?
      assert fEltJdtType.isArray() || fEltJdtType.isClass() : "Non-primitive, non-reference array element type!";
      Collection<CAstType> supers = new ArrayList<>();
      for (ITypeBinding type : fEltJdtType.getInterfaces()) {
        supers.add(getCAstTypeFor(type));
      }
      if (fEltJdtType.getSuperclass() != null)
        supers.add(getCAstTypeFor(fEltJdtType.getSuperclass()));
      return supers;
    }
  }

  public final class JdtJavaType implements JavaType {
    private final ITypeBinding fType;

    private Collection<CAstType> fSuperTypes = null;

    @Override
    public String toString() {
      return super.toString() + ":" + getName();
    }

    public JdtJavaType(ITypeBinding type) {
      super();
      fType = type;
    }

    @Override
    public String getName() {
      return fIdentityMapper.getTypeRef(fType).getName().toString();
    }

    @Override
    public Collection<CAstType> getSupertypes() {
      if (fSuperTypes == null) {
        buildSuperTypes();
      }
      return fSuperTypes;
    }

    private void buildSuperTypes() {
      // TODO this is a source entity, but it might actually be the root type
      // (Object), so assume # intfs + 1
      ITypeBinding superType = (fType.getSuperclass() == null) ? fAst.resolveWellKnownType("java.lang.Object") : fType
          .getSuperclass();
      int N = fType.getInterfaces().length + 1;

      fSuperTypes = new ArrayList<>(N);
      // Following assumes that noone can call getSupertypes() before we have
      // created CAstType's for every type in the program being analyzed.
      fSuperTypes.add(getCAstTypeFor(superType));
      for (ITypeBinding t : fType.getInterfaces())
        fSuperTypes.add(getCAstTypeFor(t));
    }

    @Override
    public Collection<CAstQualifier> getQualifiers() {
      return JDT2CAstUtils.mapModifiersToQualifiers(fType.getModifiers(), fType.isInterface(), fType.isAnnotation());
    }

    @Override
    public boolean isInterface() {
      return fType.isInterface();
    }
  }

}
