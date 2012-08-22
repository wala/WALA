function A(){
	
}
A.prototype.foo = function foo_of_A(){
	console.log("foo_of_A");
}

function B(){
	
}
B.prototype.foo = function foo_of_B(){
	console.log("foo_of_B");
}

function test1(){
	var a = new A
	console.log("calling foo_of_A");
	a.foo() 
}

function test2(){
	var b = new B
	console.log("calling foo_of_B");
	b.foo() 
}

test1()
test2()