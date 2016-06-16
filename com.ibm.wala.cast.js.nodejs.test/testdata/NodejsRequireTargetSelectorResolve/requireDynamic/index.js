var lib1 = myRequire('lib1', true);
lib1();

function myRequire(name, local) {
	var prefix = '';
	if (local) prefix = './';
	
	return require(prefix+name);
}