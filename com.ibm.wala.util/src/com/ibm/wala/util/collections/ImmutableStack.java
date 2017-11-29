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

package com.ibm.wala.util.collections;

import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Iterator;

/**
 * An immutable stack of objects. The {@link #push(Object)} and {@link #pop()}
 * operations create new stacks.
 * 
 * @param <T>
 */
public class ImmutableStack<T> implements Iterable<T> {

	private static final ImmutableStack<Object> EMPTY = new ImmutableStack<>(
			new Object[0]);

	private static final int MAX_SIZE = Integer.MAX_VALUE;

	public static int getMaxSize() {
		return MAX_SIZE;
	}

	@SuppressWarnings("unchecked")
	public static final <T> ImmutableStack<T> emptyStack() {
		return (ImmutableStack<T>) EMPTY;
	}

	final private T[] entries;

	private final int cachedHashCode;

	protected ImmutableStack(T[] entries) {
		this.entries = entries;
		this.cachedHashCode = Arrays.hashCode(entries);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o != null && o instanceof ImmutableStack) {
			ImmutableStack other = (ImmutableStack) o;
			return Arrays.equals(entries, other.entries);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return cachedHashCode;
		// return Util.hashArray(this.entries);
	}

	@SuppressWarnings("unused")
	public ImmutableStack<T> push(T entry) {
		if (entry == null) {
			throw new IllegalArgumentException("null entry");
		}
		if (MAX_SIZE == 0) {
			return emptyStack();
		}
		int size = entries.length + 1;
		T[] tmpEntries = null;
		if (size <= MAX_SIZE) {
			tmpEntries = makeInternalArray(size);
			System.arraycopy(entries, 0, tmpEntries, 0, entries.length);
			tmpEntries[size - 1] = entry;
		} else {
			tmpEntries = makeInternalArray(MAX_SIZE);
			System.arraycopy(entries, 1, tmpEntries, 0, entries.length - 1);
			tmpEntries[MAX_SIZE - 1] = entry;

		}
		return makeStack(tmpEntries);
	}

	@SuppressWarnings("unchecked")
	protected T[] makeInternalArray(int size) {
		return (T[]) new Object[size];
	}

	protected ImmutableStack<T> makeStack(T[] tmpEntries) {
		return new ImmutableStack<>(tmpEntries);
	}

	/**
	 * @return the element on the top of the stack
	 * @throws EmptyStackException
	 *             if stack is empty
	 */
	public T peek() {
		if (entries.length == 0) {
			throw new EmptyStackException();
		}
		return entries[entries.length - 1];
	}

	/**
	 * @throws EmptyStackException
	 *             if stack is empty
	 */
	public ImmutableStack<T> pop() {
		if (entries.length == 0) {
			throw new EmptyStackException();
		}
		int size = entries.length - 1;
		T[] tmpEntries = makeInternalArray(size);
		System.arraycopy(entries, 0, tmpEntries, 0, size);
		return makeStack(tmpEntries);
	}

	public boolean isEmpty() {
		return entries.length == 0;
	}

	public int size() {
		return entries.length;
	}

	public T get(int i) {
		try {
			return entries[i];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("invalid i: " + i, e);
		}
	}

	@Override
	public String toString() {
		String objArrayToString = Arrays.toString(entries);
		assert entries.length <= MAX_SIZE : objArrayToString;
		return objArrayToString;
	}

	public boolean contains(T entry) {
		if (entry == null) {
			return false;
		}
		for (T entrie : entries) {
			if (entrie != null && entrie.equals(entry))
				return true;
		}
		return false;
	}

	/**
	 * @return <code>true</code> iff {@code other.size() = k}, {@code k <= this.size()}, and the
	 *         top k elements of this equal other
	 * @throws IllegalArgumentException
	 *             if other == null
	 */
	public boolean topMatches(ImmutableStack<T> other)
			throws IllegalArgumentException {
		if (other == null) {
			throw new IllegalArgumentException("other == null");
		}
		if (other.size() > size()) {
			return false;
		}
		for (int i = other.size() - 1, j = this.size() - 1; i >= 0; i--, j--) {
			if (!other.get(i).equals(get(j)))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public ImmutableStack<T> reverse() {
		T[] tmpEntries = (T[]) new Object[entries.length];
		for (int i = entries.length - 1, j = 0; i >= 0; i--, j++) {
			tmpEntries[j] = entries[i];
		}
		return new ImmutableStack<>(tmpEntries);
	}

	@SuppressWarnings("unchecked")
	public ImmutableStack<T> popAll(ImmutableStack<T> other) {
		if (!topMatches(other)) {
			throw new IllegalArgumentException("top does not match");
		}
		int size = entries.length - other.entries.length;
		T[] tmpEntries = (T[]) new Object[size];
		System.arraycopy(entries, 0, tmpEntries, 0, size);
		return new ImmutableStack<>(tmpEntries);
	}

	@SuppressWarnings("unchecked")
	public ImmutableStack<T> pushAll(ImmutableStack<T> other) {
		if (other == null) {
			throw new IllegalArgumentException("null other");
		}
		int size = entries.length + other.entries.length;
		T[] tmpEntries = null;
		if (size <= MAX_SIZE) {
			tmpEntries = (T[]) new Object[size];
			System.arraycopy(entries, 0, tmpEntries, 0, entries.length);
			System.arraycopy(other.entries, 0, tmpEntries, entries.length,
					other.entries.length);
		} else {
			tmpEntries = (T[]) new Object[MAX_SIZE];
			// other has size at most MAX_SIZE
			// must keep all in other
			// top MAX_SIZE - other.size from this
			int numFromThis = MAX_SIZE - other.entries.length;
			System.arraycopy(entries, entries.length - numFromThis, tmpEntries,
					0, numFromThis);
			System.arraycopy(other.entries, 0, tmpEntries, numFromThis,
					other.entries.length);
		}
		return new ImmutableStack<>(tmpEntries);
	}

	@Override
  public Iterator<T> iterator() {
		if (entries.length == 0) {
			return EmptyIterator.instance();
		}
		return new ArrayIterator<>(entries);
	}

	/**
	 * return a new stack with the top replaced with t
	 * 
	 * @throws EmptyStackException
	 */
	public ImmutableStack<T> replaceTop(T t) {
		if (isEmpty()) {
			throw new EmptyStackException();
		}
		int size = entries.length;
		T[] tmpEntries = makeInternalArray(size);
		System.arraycopy(entries, 0, tmpEntries, 0, entries.length - 1);
		tmpEntries[size - 1] = t;
		return makeStack(tmpEntries);
	}
}
