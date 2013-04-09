package com.ibm.wala.util.io;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import com.ibm.wala.util.PlatformUtil;

public class FileProviderTest {

  @Test
  public void testValidFile() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///c:/my/File.jar");
    String expected = "/c:/my/File.jar";
    // exercise:
    String actual = (new FileProvider()).filePathFromURL(url);
    // verify:
    assertEquals(expected, actual);
  }
  
  
  @Test
  public void testURLWithInvalidURIChars() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///[Eclipse]/File.jar");
    String expected = PlatformUtil.onWindows() ? "/C:/[Eclipse]/File.jar" : "/[Eclipse]/File.jar";
    // exercise:
    String actual = (new FileProvider()).filePathFromURL(url);
    // verify:
    assertEquals(expected, actual);
  }

  @Test
  public void testURLWithSpace() throws MalformedURLException {
    URL url = new URL("file:///With%20Space/File.jar");
    String expected = PlatformUtil.onWindows() ? "/C:/With Space/File.jar" : "/With Space/File.jar";
    // exercise:
    String actual = (new FileProvider()).filePathFromURL(url);
    // verify:
    assertEquals(expected, actual);
  }
  
}
