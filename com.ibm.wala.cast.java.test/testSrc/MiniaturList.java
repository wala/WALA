public class MiniaturList {
	MiniaturList next;

	int data;

	public MiniaturList remove(int elt) {
		MiniaturList xp = null;
		MiniaturList head = this;
		MiniaturList x = head;

		while (x != null) {
			if (x.data == elt) {
				if (xp == null)
					head = x.next;
				else
					xp.next = x.next;
				break;
			}
			xp = x;
			x = x.next;
		}
		return head;
	}

	public static MiniaturList cons(int elt, MiniaturList l) {
		MiniaturList ret = new MiniaturList();
		ret.data = elt;
		ret.next = l;
		return ret;
	}

	public boolean contains(int elt) {
		MiniaturList head = this;
		while (head != null) {
			if (head.data == elt)
				return true;
			head = head.next;
		}
		return false;
	}

	public static void main(String[] args) {
		MiniaturList l1 = cons(1, cons(2, cons(3, cons(2, null))));
		MiniaturList l2 = cons(5, null);

		l1 = l1.remove(2);
		//assert 2 !in l1.*next.data

		System.out.println(l1.contains(3));
		System.out.println(l2.contains(6));

	}
}
