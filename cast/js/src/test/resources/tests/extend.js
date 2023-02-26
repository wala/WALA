function extend(dest, src) {
	for(var p in src)
		dest[p] = src[p];
}

var obj = {};
extend(obj, {
	foo: function foo() {},
	bar: function bar() {}
});
obj.bar();