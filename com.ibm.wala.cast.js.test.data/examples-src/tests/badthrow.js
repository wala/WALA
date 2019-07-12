function f(n) {
	throw n;
}

function nrWrapper(){
    try{
    	return f(3);
    } catch(p) {
    	throw f(5);
    } finally { 
    	f(2);
    }
}

nrWrapper()
