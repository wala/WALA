/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.viz;

import com.ibm.wala.util.warnings.WalaException;

/**
 * utilities for ghostview
 * 
 * @author sfink
 */
public class GVUtil {
  
  

  /**
   * Launch a process to view a postscript file
   */
  public static Process launchGV(String psFile, String gvExe) throws WalaException {
    // set up a viewer for the ps file.
    final GSViewLauncherImpl gv = new GSViewLauncherImpl();
    gv.setGvExe(gvExe);
    gv.setPsfile(psFile);
    gv.run();
    if (gv.getProcess() == null) {
      throw new WalaException(" problem spawning process ");
    }
    return gv.getProcess();
  }

}
