function nrWrapper() {
	var r,a,c,s;
	try{
		a=this,r=i(arguments),c="function"==typeof n?n(r,a):n||{}
	} catch (u) {
		h([u,"",[r,a,o],c])
	}
	f(e+"start",[r,a,o],c);
	try {
		return s=t.apply(a,r)
	} catch(p){
		throw f(e+"err",[r,a,p],c),p
	} finally {
		f(e+"end",[r,a,s],c)
	}
}

nrWrapper()
