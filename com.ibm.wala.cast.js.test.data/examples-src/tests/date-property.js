var t = new String("this is a long string");

var bar = t.substring(0, 10);

var d = new Date("October 13, 1975 11:13:00");

var document = { };

document[d] = function _fun(x) {
    return x;
}

var f = document[d]

f(bar);
