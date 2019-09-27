function base() {
    print(7);
}

function bad() {
    print("bad");
    throw 17;
}

function good() {
    print("good");
    return 5;
}

function oo1() {
	throw "other one";	
}

function oo2() {
	throw "other two";
}

function e(a) {
    var c=1;
    var o=1;
    try {
	    try {
		var s=a.f();
	    } finally {
		try {
		    a.one()
		    o=0
		} finally {
		    if (o==1) oo1();
		} 
	    } 
    } finally {
	try {
	    a.two();
	    c = 0 
	} finally {
	    if (c==1) oo2();
	}
    }
    return "done"
}

e({f: base, two: good, one: bad})

