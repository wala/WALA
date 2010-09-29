package com.ibm.wala.cast.js.html;

import java.net.URL;

public class IdentityUrlResover implements IUrlResolver{

  public URL resolve(URL input) {
    return input;
  }

  public URL deResolve(URL input) {
    return input;
  }

}
