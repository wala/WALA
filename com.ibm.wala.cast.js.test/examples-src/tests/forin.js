function testForIn( x ) {
  var z;
  for(var y in x) {
	if (y in x) {
      z = (x[y])();
	}
  }
}

testForIn({
  foo: function testForIn1() { return 7; },
  bar: function testForIn2() { return "whatever"; }
});

