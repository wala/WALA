package slice;

import java.io.IOException;

public class JustThrow {
  
  private static int throwException() {
    throw new RuntimeException();
  }
    public static void main(String[] argv) throws IOException{
      doNothing(throwException());
    }
    
    private static void doNothing(int x) {

    }

}
