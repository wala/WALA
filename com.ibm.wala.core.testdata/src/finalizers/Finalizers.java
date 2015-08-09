package finalizers;

public class Finalizers {

  private void sillyMethod() {
    
  }
  
  @Override
  public void finalize() {
    sillyMethod();
  }
  
  public static void main(String[] args) {
    new Finalizers().sillyMethod();
  }
  
}
