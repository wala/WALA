function three(a, b, c) {
	return a;
}

function two_a(a, b) {
	return three(a, b, "string");
}

function two_b(a, b) {
	return three(a, b, 17);
}

var x = "one";
var y = "two";

two_a(x, y);
two_a(y, x);

for(var i = 0; i < 2; i++) {
	two_b(i==0?x:y, i==0?y:x);
}

