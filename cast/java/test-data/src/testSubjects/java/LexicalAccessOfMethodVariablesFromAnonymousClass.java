public class LexicalAccessOfMethodVariablesFromAnonymousClass {

  public static void main(String[] args) {
    new LexicalAccessOfMethodVariablesFromAnonymousClass().run("");
  }

  public void run(final String var) {
    Object o = new Object() {
      @Override
      public int hashCode() {
        try {
	    return var.hashCode();
	} catch (Exception e) {
          String s = var;
        }

        return 0;
      }
    };
    o.hashCode();
    var.hashCode();
  }

}
