package com.ibm.wala.cast.ir.translator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;

/**
 * common functionality for any {@link TranslatorToCAst} making use of native code
 */
public abstract class NativeTranslatorToCAst 
  extends NativeBridge 
  implements TranslatorToCAst
{

  protected final URL sourceURL;

  protected final String sourceFileName;

  protected NativeTranslatorToCAst(CAst Ast, URL sourceURL, String sourceFileName) {
    super(Ast);
    this.sourceURL = sourceURL;
    this.sourceFileName = sourceFileName;
  }

  protected String getLocalFile() {
    return sourceFileName;
  }

  protected String getFile() {
    return sourceURL.getFile();
  }

  protected Position makeLocation(final int fl, final int fc, final int ll, final int lc) {
    return new AbstractSourcePosition() {
      @Override
      public int getFirstLine() {
        return fl;
      }

      @Override
      public int getLastLine() {
        return ll;
      }

      @Override
      public int getFirstCol() {
        return fc;
      }

      @Override
      public int getLastCol() {
        return lc;
      }

      @Override
      public int getFirstOffset() {
        return -1;
      }

      @Override
      public int getLastOffset() {
        return -1;
      }
      
      @Override
      public URL getURL() {
        return sourceURL;
      }

      public InputStream getInputStream() throws IOException {
        return new FileInputStream(sourceFileName);
      }

      @Override
      public String toString() {
        String urlString = sourceURL.toString();
        if (urlString.lastIndexOf(File.separator) == -1)
          return "[" + fl + ":" + fc + "]->[" + ll + ":" + lc + "]";
        else
          return urlString.substring(urlString.lastIndexOf(File.separator) + 1) + "@[" + fl + ":" + fc + "]->[" + ll + ":" + lc
              + "]";
      }

      @Override
      public Reader getReader() throws IOException {
         return new InputStreamReader(getInputStream());
      }
    };
  }

  @Override
  public abstract CAstEntity translateToCAst();

}
