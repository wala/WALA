package javaonepointseven;

/**
 * @author Linghui Luo
 * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/underscores-literals.html">Java 7 docs</a>
 */
public class UnderscoresInNumericLiterals {

  public void test() {
    long creditCardNumber = 1234_5678_9012_3456L;
    long socialSecurityNumber = 999_99_9999L;
    float pi = 3.14_15F;
    long hexBytes = 0xFF_EC_DE_5E;
    long hexWords = 0xCAFE_BABE;
    long maxLong = 0x7fff_ffff_ffff_ffffL;
    byte nybbles = 0b0010_0101;
    long bytes = 0b11010010_01101001_10010100_10010010;
  }
  
  public static void main(String[] args) {
	  new UnderscoresInNumericLiterals().test();
  }

}
