/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
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
package com.ibm.wala.demandpa.util;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.strings.Atom;

/**
 * Pseudo-field modelling the contents of an array of reference type. Only for
 * convenience; many of the methods don't actually work. Also, a singleton.
 * 
 * @author manu
 * 
 */
public class ArrayContents implements IField {

  private static final ArrayContents theContents = new ArrayContents();

  public static final ArrayContents v() {
    return theContents;
  }

  private ArrayContents() {
  }

  @Override
  public TypeReference getFieldTypeReference() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public boolean isFinal() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return false;
  }

  @Override
  public boolean isPrivate() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return false;
  }

  @Override
  public boolean isProtected() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return false;
  }

  @Override
  public boolean isPublic() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return false;
  }

  @Override
  public boolean isStatic() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return false;
  }

  @Override
  public IClass getDeclaringClass() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Atom getName() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public String toString() {
    return "arr";
  }

  @Override
  public boolean isVolatile() {
    return false;
  }

  @Override
  public ClassHierarchy getClassHierarchy() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public FieldReference getReference() {
    return null;
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    return Collections.emptySet();
  }
}
