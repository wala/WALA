var x;

function f() {
	do {
		if (f()) {
			x++;
			break;
		}
	} while (false);
}

f();