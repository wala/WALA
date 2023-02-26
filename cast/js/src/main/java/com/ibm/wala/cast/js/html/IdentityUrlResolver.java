/*
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.html;

import java.net.URL;

public class IdentityUrlResolver implements IUrlResolver {

  @Override
  public URL resolve(URL input) {
    return input;
  }

  @Override
  public URL deResolve(URL input) {
    return input;
  }
}
