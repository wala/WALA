/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.config;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** A serializable filter that accepts or rejects {@link String}s. */
public interface StringFilter extends Predicate<String>, Serializable {

  /**
   * Provides a JSON-compatible representation of this filter for use in serialization.
   *
   * <p>JSON-compatible types include {@link String}, {@link List} of a JSON-compatible type, {@link
   * Map} from {@link String} to a JSON-compatible type, etc..
   *
   * @return a JSON-compatible representation of this filter
   */
  Object toJson();
}
