var x;

function f() {
	do {
		// noinspection JSVoidFunctionReturnValueUsed
		if (f()) {
			x++;
			break;
		}
	} while (false);
}

f();