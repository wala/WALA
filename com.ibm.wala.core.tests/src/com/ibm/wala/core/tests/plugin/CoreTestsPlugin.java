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
package com.ibm.wala.core.tests.plugin;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;


/**
 * The main plugin class to be used in the desktop.
 */
public class CoreTestsPlugin extends Plugin {

  // The shared instance.
  private static CoreTestsPlugin plugin;

  /**
   * The constructor.
   */
  public CoreTestsPlugin() {
    plugin = this;
  }

  /**
   * This method is called upon plug-in activation
   * @throws IllegalArgumentException  if context is null
   */
  @Override
  public void start(BundleContext context) throws Exception {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    super.start(context);
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static CoreTestsPlugin getDefault() {
    return plugin;
  }

}
