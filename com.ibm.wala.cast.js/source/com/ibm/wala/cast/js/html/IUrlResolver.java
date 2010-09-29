package com.ibm.wala.cast.js.html;

import java.net.URL;

/**
 * Used for handling resources that were copied from the web to local files (and still contain references to the web)
 * @author yinnonh
 * @author danielk
 *
 */
public interface IUrlResolver {
  /**
   * From Internet to local
   * @param input
   * @return
   */
  public URL resolve(URL input);
  
  /**
   * From local to Internet
   * @param input
   * @return
   */
  public URL deResolve(URL input);
  
}
