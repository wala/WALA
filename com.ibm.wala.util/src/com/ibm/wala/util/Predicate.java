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
package com.ibm.wala.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Interface for defining an arbitrary predicate on {@link Object}s.
 */
public abstract class Predicate<T> {
  @SuppressWarnings("rawtypes")
  public static final Predicate FALSE = new Predicate() {
    @Override
    public boolean test(Object o) {
      return false;
    }
  };

  @SuppressWarnings("rawtypes")
  public static final Predicate TRUE = FALSE.not();

  public static <T> Predicate<T> truePred() {
    return TRUE;
  }

  public static <T> Predicate<T> falsePred() {
    return FALSE;
  }

  /** Test whether an {@link Object} satisfies this {@link Predicate} */
  public abstract boolean test(T t);

  /** Return a predicate that is a negation of this predicate */
  public Predicate<T> not() {
    final Predicate<T> originalPredicate = this;
    return new Predicate<T>() {
      @Override
      public boolean test(T t) {
        return !originalPredicate.test(t);
      }
    };
  }

  /**
   * Return a predicate that is a conjunction of this predicate and another predicate
   */
  public Predicate<T> and(final Predicate<T> conjunct) {
    final Predicate<T> originalPredicate = this;
    return new Predicate<T>() {
      @Override
      public boolean test(T t) {
        return originalPredicate.test(t) && conjunct.test(t);
      }
    };
  }

  /**
   * Return a predicate that is a conjunction of this predicate and another predicate
   */
  public Predicate<T> or(final Predicate<T> disjunct) {
    final Predicate<T> originalPredicate = this;
    return new Predicate<T>() {
      @Override
      public boolean test(T t) {
        return originalPredicate.test(t) || disjunct.test(t);
      }
    };
  }
  
  /**
   * Create the predicate "is an element of c"
   */
  public static <T> Predicate<T> isElementOf(final Collection<T> c) {
    return new Predicate<T>() {
      @Override
      public boolean test(T t) {
        return c.contains(t);
      }
    };
  }
  
  /**
   * Filter a collection: generate a new list from an existing collection, consisting of the elements satisfying some predicate.
   * 
   * @throws IllegalArgumentException if src == null
   */
  public static <T> List<T> filter(Iterator<T> src, Predicate<T> pred) throws IllegalArgumentException {
    if (src == null) {
      throw new IllegalArgumentException("src == null");
    }
    ArrayList<T> result = new ArrayList<>();
    for (; src.hasNext();) {
      T curElem = src.next();
      if (pred.test(curElem))
        result.add(curElem);
    }
    return result;
  }
}

