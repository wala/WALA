/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.util;

import java.util.Iterator;

import com.ibm.wala.cast.loader.CAstAbstractLoader;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.warnings.Warning;

public class Util {

  public static void checkForFrontEndErrors(IClassHierarchy cha) throws WalaException {
    StringBuffer message = null;
    for(IClassLoader loader : cha.getLoaders()) {
      if (loader instanceof CAstAbstractLoader) {
        Iterator<ModuleEntry> errors = ((CAstAbstractLoader)loader).getModulesWithParseErrors();
        if (errors.hasNext()) {
          if (message == null) {
            message = new StringBuffer("front end errors:\n");
          }
          while (errors.hasNext()) {
            ModuleEntry errorModule = errors.next();
            for(Warning w : (((CAstAbstractLoader)loader).getMessages(errorModule))) {
              message.append("error in ").append(errorModule.getName()).append(":\n");
              message.append(w.toString()).append("\n");
            }
          }
        }
        // clear out the errors to free some memory
        ((CAstAbstractLoader)loader).clearMessages();
      }
    }
    if (message != null) {
      message.append("end of front end errors\n");
      throw new WalaException(String.valueOf(message));
    }
  }
  

}
