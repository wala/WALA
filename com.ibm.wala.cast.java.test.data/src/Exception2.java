import java.io.*;

public final class Exception2 {

  public static void main(String[] args) {
    Exception2 e2= new Exception2();
    FileInputStream fis = null;
    FileOutputStream fos = null;

    try {
      fis = new FileInputStream( args[0] ); 
      fos = new FileOutputStream( args[1] );

      int data;
      while ( (data = fis.read()) != -1 ) {
	fos.write(data);
      }
    } catch (FileNotFoundException e) {
      System.err.println( "File not found" ); 
      // whatever
    } catch (IOException e) {
      System.err.print( "I/O problem " ); 
      System.err.println( e.getMessage() ); 
    } finally {
      if (fis != null) {
	try {
	  fis.close();
	} catch (IOException e) {
	  System.exit(-1);
	}
      }
      if (fos != null) {
	try {
	  fos.close();
	} catch (IOException e) {
	  System.exit(-1);
	}
      }
    }
  }
}
