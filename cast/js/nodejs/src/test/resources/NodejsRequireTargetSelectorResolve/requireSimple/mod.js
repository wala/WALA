function SomeClass() {
	this.hello = function hello() {}
}

exports.exec = function exec() {
	var c = new SomeClass();
	c.hello();
};