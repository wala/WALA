public class LexicalAccessOfMethodVariablesFromAnonymousClass {

  public static void main(String[] args) {
    new LexicalAccessOfMethodVariablesFromAnonymousClass().run("");
  }

  public void run(final String var) {
    new Object() {
      @Override
      public int hashCode() {
        try { }
        catch (Exception e) {
          String s = var;
        }

        return 0;
      }
    };
  }

}
