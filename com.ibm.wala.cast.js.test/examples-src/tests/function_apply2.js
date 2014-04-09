// test use of arguments array
function useArgs() {
	return arguments[2];
}
var o = {}
function theThree() {}
var a = useArgs;
var b = a.apply(o, [o,o,theThree]);
b();