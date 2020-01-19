function _slice_target_fn(x) {

}

var x = 7;

var o = { f: 7 };

if (o !=  null) {
    o = (function _push_o(x) { return x>0? x: -1; })(o.f);

    var p = { f: function yuck(a, b) { _slice_target_fn(a); _slice_target_fn(b); } };

    p.f(o);
}
