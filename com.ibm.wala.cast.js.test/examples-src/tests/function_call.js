function foo(x) { return x; }

function bar(x) { return x + 1; }

if (p > 3) {
	z = foo;	
} else {
	z = bar;
}
  
z.call(null, 3);

