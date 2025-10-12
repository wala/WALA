/*
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
public class MiniatureList {
	MiniatureList next;

	int data;

	public MiniatureList remove(int elt) {
		MiniatureList xp = null;
		MiniatureList head = this;
		MiniatureList x = head;

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

	public static MiniatureList cons(int elt, MiniatureList l) {
		MiniatureList ret = new MiniatureList();
		ret.data = elt;
		ret.next = l;
		return ret;
	}

	public boolean contains(int elt) {
		MiniatureList head = this;
		while (head != null) {
			if (head.data == elt)
				return true;
			head = head.next;
		}
		return false;
	}

	public static void main(String[] args) {
    MiniatureList l1 = cons(1, cons(2, cons(3, cons(2, null))));
    MiniatureList l2 = cons(5, null);

		l1 = l1.remove(2);
		//assert 2 !in l1.*next.data

		System.out.println(l1.contains(3));
		System.out.println(l2.contains(6));

	}
}
