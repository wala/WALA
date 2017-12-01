package lambda;

import java.util.Arrays;
import java.util.Comparator;

public class SortingExample {  
	private final String[] strings;
	
	public SortingExample(int n) {
		this.strings = new String[n];
		for(int i = 0; i < n; i++) {
			strings[i] = "str" + i;
		}
	}
 
  private String[] sort(Comparator<String> c) {
		String[] strs = strings.clone();
		Arrays.sort(strs, c);
		return strs;
	}
	
  private static int id0(int x) { return x; }

  private static int id1(int x) { return id0(x); }

  public String[] sortForward() {
		return sort( (String l, String r) -> id1(l.compareTo(r)));
	}

  private static int id2(int x) { return x; }

	 public String[] sort(int v) {
	    return sort( (String l, String r) -> id2(l.compareTo(r) * v));
	  }

   private int id3(int x) { return x; }

   public String[] sort(String v) {
     return sort( (String l, String r) -> id3(l.compareTo(v) + v.compareTo(r)));
   }

   private static int id4(int x) { return x; }
   
	public String[] sortBackward() {
		return sort( (String l, String r) -> id4(r.compareTo(l)));
	}
	
	public static void main(String[] args) {
		SortingExample x = new SortingExample(10);
		System.err.println( Arrays.toString( x.sortForward() ));
    System.err.println( Arrays.toString( x.sort(1) ));
		System.err.println( Arrays.toString( x.sortBackward() ));
    System.err.println( Arrays.toString( x.sort(-1) ));
    System.err.println( Arrays.toString( x.sort( "something" ) ));
	}
}
