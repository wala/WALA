/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertEquals;

import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.util.PlatformUtil;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;

public class FileProviderTest {

  @Test
  public void testValidFile() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///c:/my/File.jar");
    String expected = "/c:/my/File.jar";
    // exercise:
    String actual = new FileProvider().filePathFromURL(url);
    // verify:
    assertEquals(expected, actual);
  }

  @Test
  public void testURLWithInvalidURIChars() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///[Eclipse]/File.jar");
    // exercise:
    String actual = new FileProvider().filePathFromURL(url);
    // verify:
    assertThat(
        actual,
        PlatformUtil.onWindows()
            ? matchesPattern("\\A/[A-Z]:/\\[Eclipse]/File.jar\\z")
            : equalTo("/[Eclipse]/File.jar"));
  }

  @Test
  public void testURLWithSpace() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///With%20Space/File.jar");
    // exercise:
    String actual = new FileProvider().filePathFromURL(url);
    // verify:
    assertThat(
        actual,
        PlatformUtil.onWindows()
            ? matchesPattern("\\A/[A-Z]:/With Space/File\\.jar\\z")
            : equalTo("/With Space/File.jar"));
  }
}
