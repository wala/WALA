public class Breaks {

  private static class Ref {
    String[] classes;

    String[] getClasses() {
      return classes;
    }

    private Ref(String[] classes) {
      this.classes = classes;
    }
  }

  private void testBreakFromIf(String objectClass, Ref reference) {
    objectClassCheck:
    if (objectClass != null) {
      String[] classes = reference.getClasses();
      int size = classes.length;
      for (int i = 0; i < size; i++) {    
	if (classes[i] == objectClass)
	  break objectClassCheck;
      }
      return;
    }
    if (objectClass == null) {
      reference.classes = null;
    }
  }

  public static void main(String[] args) {
    (new Breaks()).testBreakFromIf("whatever", new Ref(args));
  }

}