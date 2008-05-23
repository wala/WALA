package string;

public class SimpleStringOps {

  private static void whatever(String s) {
    StringBuffer sb = new StringBuffer();
    sb.append(s.substring(5));
    sb.append(" and other garbage");
    System.out.println(sb.toString());
  }
  
  public static void main(String[] args) {
    if (args.length > 0) {
      String s = args[0];
      for(int i = 1; i < args.length; i++) {
        s = s + args[i];
      }
      
      if (s.length() < 6) {
        s = "a silly prefix " + s;
      }
      
      whatever(s);
    }
  }

}
