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
package com.ibm.wala.util.collections;

import java.util.Collection;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** */
public class ArraySetMultiMap<K, V> extends AbstractMultiMap<K, V> {

  /** */
  private static final long serialVersionUID = -3475591699051060160L;

  public static final ArraySetMultiMap<?, ?> EMPTY =
      new ArraySetMultiMap<>() {

        /** */
        private static final long serialVersionUID = 1839857029830528896L;

        @Override
        public boolean put(Object key, @Nullable Object val) {
          throw new RuntimeException();
        }

        @Override
        public boolean putAll(Object key, Collection<? extends Object> vals) {
          throw new RuntimeException();
        }
      };

  public ArraySetMultiMap() {
    super(false);
  }

  public ArraySetMultiMap(boolean create) {
    super(create);
  }

  @Override
  protected Set<V> createSet() {
    return new ArraySet<>();
  }

  @Override
  protected Set<V> emptySet() {
    return ArraySet.<V>empty();
  }

  @Override
  public ArraySet<V> get(@Nullable K key) {
    return (ArraySet<V>) super.get(key);
  }

  public static <K, V> ArraySetMultiMap<K, V> make() {
    return new ArraySetMultiMap<>();
  }
}
