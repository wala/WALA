public class NullArrayInit {
  String[] x = {null};

  public static void main(String[] args) {
    new NullArrayInit();
    Object a[] = new Object[] {null,null};
    Object b[] = {null};
    String c[] = {null};
    String d[] = {null,null};
    String e[] = {null,"hello",null};
    String f[] = new String[] {null};
    String g[] = new String[] {null,null,null};
    String j[][] = { {null,null}, {null} };
  }
}
