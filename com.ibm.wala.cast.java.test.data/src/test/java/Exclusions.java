
public class Exclusions {

  public static class Included {
    private final int f;
    
    private Included(int f) {
      this.f = f;
    }
    
    public int doit() {
      return f + 1;
    }
  }

  public static class Excluded {
    private final int f;
    
    private Excluded(int f) {
      this.f = f;
    }
    
    public int doit() {
      return f + 1;
    }
  }

  public void run(String[] args) {
    int i = new Included(5).doit();
  }

  public static void main(String[] args) {
    new Exclusions().run(args);
  }

}
