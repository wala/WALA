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
	
	public String[] sortForward() {
		return sort( (String l, String r) -> l.compareTo(r));
	}

	public String[] sortBackward() {
		return sort( (String l, String r) -> r.compareTo(l));
	}
	
	public static void main(String[] args) {
		SortingExample x = new SortingExample(10);
		System.err.println( Arrays.toString( x.sortForward() ));
		System.err.println( Arrays.toString( x.sortBackward() ));
	}
}
