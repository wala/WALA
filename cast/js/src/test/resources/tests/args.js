
function a() {
	if (arguments.length >= 1) {
		arguments[0]();
		if (arguments.length >= 3) {
			arguments[1]();
		}
	    if (arguments.length >= 3) {
		    arguments[1]();
	    } else {
			arguments[0]();			
		}
	}
}

function x() {
	print("x");
}

function q() {
	print("q");
}

function y() {
	print("y");
}

function z() {
	print("z");
}

function wrong() {
	print("wrong");
}

a(x, q);
a(y, z, wrong);
