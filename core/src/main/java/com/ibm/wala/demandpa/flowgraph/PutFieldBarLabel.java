/*
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
package com.ibm.wala.demandpa.flowgraph;

import com.ibm.wala.classLoader.IField;

/**
 * @author Manu Sridharan
 */
public class PutFieldBarLabel implements IFlowLabel {

  private final IField field;

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((field == null) ? 0 : field.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final PutFieldBarLabel other = (PutFieldBarLabel) obj;
    if (field == null) {
      if (other.field != null) return false;
    } else if (!field.equals(other.field)) return false;
    return true;
  }

  private PutFieldBarLabel(final IField field) {
    this.field = field;
  }

  public static PutFieldBarLabel make(IField field) {
    // TODO cache these?
    return new PutFieldBarLabel(field);
  }

  public IField getField() {
    return field;
  }

  @Override
  public PutFieldLabel bar() {
    return PutFieldLabel.make(field);
  }

  @Override
  public void visit(IFlowLabelVisitor v, Object dst) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException("v == null");
    }
    v.visitPutFieldBar(this, dst);
  }

  @Override
  public boolean isBarred() {
    return true;
  }

  @Override
  public String toString() {
    return "putfield_bar[" + field + ']';
  }
}
