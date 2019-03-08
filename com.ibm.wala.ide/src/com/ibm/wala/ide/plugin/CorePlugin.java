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
package com.ibm.wala.ide.plugin;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** The main plugin class to be used in the desktop. */
public class CorePlugin extends Plugin {

  public static final boolean IS_ECLIPSE_RUNNING;

  static {
    boolean result = false;
    try {
      result = Platform.isRunning();
    } catch (Throwable exception) {
      // Assume that we aren't running.
    }
    IS_ECLIPSE_RUNNING = result;
  }

  public static final boolean IS_RESOURCES_BUNDLE_AVAILABLE;

  static {
    boolean result = false;
    if (IS_ECLIPSE_RUNNING) {
      try {
        Bundle resourcesBundle = Platform.getBundle("org.eclipse.core.resources");
        result =
            resourcesBundle != null
                && (resourcesBundle.getState()
                        & (Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED))
                    != 0;
      } catch (Throwable exception) {
        // Assume that it's not available.
      }
    }
    IS_RESOURCES_BUNDLE_AVAILABLE = result;
  }

  // The shared instance.
  private static CorePlugin plugin;

  /** The constructor. */
  public CorePlugin() {
    plugin = this;
  }

  /**
   * This method is called upon plug-in activation
   *
   * @throws IllegalArgumentException if context is null
   */
  @Override
  public void start(BundleContext context) throws Exception {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    super.start(context);
  }

  /** This method is called when the plug-in is stopped */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  /** Returns the shared instance. */
  public static CorePlugin getDefault() {
    return plugin;
  }
}
