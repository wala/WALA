
function a() {
	if (arguments.length >= 1) {
		arguments[0]();
		if (arguments.length >= 2) {
			arguments[1]();
		}
	}
}

function x() {
	print("x");
}

function y() {
	print("y");
}

function z() {
	print("z");
}

a(x);
a(y, z);
