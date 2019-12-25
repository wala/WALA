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
package com.ibm.wala.cast.java.test;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/** The main plugin class to be used in the desktop. */
public class TestPlugin extends Plugin {

  // The shared instance.
  private static TestPlugin plugin;

  /** The constructor. */
  public TestPlugin() {
    plugin = this;
  }

  /** This method is called upon plug-in activation */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  /** This method is called when the plug-in is stopped */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  /** Returns the shared instance. */
  public static TestPlugin getDefault() {
    return plugin;
  }
}
