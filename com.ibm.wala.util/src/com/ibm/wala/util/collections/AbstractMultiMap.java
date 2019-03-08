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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

abstract class AbstractMultiMap<K, V> implements Serializable, MultiMap<K, V> {

  /** */
  private static final long serialVersionUID = 4064901973301954076L;

  protected final Map<K, Set<V>> map = HashMapFactory.make();

  protected final boolean create;

  protected AbstractMultiMap(boolean create) {
    this.create = create;
  }

  protected abstract Set<V> createSet();

  protected Set<V> emptySet() {
    return Collections.<V>emptySet();
  }

  @Override
  public Set<V> get(K key) {
    Set<V> ret = map.get(key);
    if (ret == null) {
      if (create) {
        ret = createSet();
        map.put(key, ret);
      } else {
        ret = emptySet();
      }
    }
    return ret;
  }

  /*
   * (non-Javadoc)
   *
   * @see AAA.util.MultiMap#put(K, V)
   */
  @Override
  public boolean put(K key, V val) {
    Set<V> vals = map.get(key);
    if (vals == null) {
      vals = createSet();
      map.put(key, vals);
    }
    return vals.add(val);
  }

  /*
   * (non-Javadoc)
   *
   * @see AAA.util.MultiMap#remove(K, V)
   */
  @Override
  public boolean remove(K key, V val) {
    Set<V> elems = map.get(key);
    if (elems == null) return false;
    boolean ret = elems.remove(val);
    if (elems.isEmpty()) {
      map.remove(key);
    }
    return ret;
  }

  @Override
  public Set<V> removeAll(K key) {
    return map.remove(key);
  }

  /*
   * (non-Javadoc)
   *
   * @see AAA.util.MultiMap#keys()
   */
  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  /*
   * (non-Javadoc)
   *
   * @see AAA.util.MultiMap#containsKey(java.lang.Object)
   */
  @Override
  public boolean containsKey(K key) {
    return map.containsKey(key);
  }

  /*
   * (non-Javadoc)
   *
   * @see AAA.util.MultiMap#size()
   */
  @Override
  public int size() {
    int ret = 0;
    for (K key : keySet()) {
      ret += get(key).size();
    }
    return ret;
  }

  /*
   * (non-Javadoc)
   *
   * @see AAA.util.MultiMap#toString()
   */
  @Override
  public String toString() {
    return map.toString();
  }

  /*
   * (non-Javadoc)
   *
   * @see AAA.util.MultiMap#putAll(K, java.util.Set)
   */
  @Override
  public boolean putAll(K key, Collection<? extends V> vals) {
    Set<V> edges = map.get(key);
    if (edges == null) {
      edges = createSet();
      map.put(key, edges);
    }
    return edges.addAll(vals);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }
}
