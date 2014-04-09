

    (function _top_level () {

        
		var jQuery = window.jQuery = window.$ = function (selector, context) {
            return new jQuery.fn.init(selector, context);
        };


		var undefined;

        jQuery.extend = jQuery.fn.extend = function _extend () {
            var target = arguments[0] || {},
                i = 1,
                length = arguments.length,
                deep = false,
                options;
            if (target.constructor == Boolean) {
                deep = target;
                target = arguments[1] || {};
                i = 2;
            }
            if (typeof target != "object" && typeof target != "function") target = {};
            if (length == i) {
                target = this;
                --i;
            }
            for (; i < length; i++) if ((options = arguments[i]) != null) 
            	for (var name in options) {
                  (function _forin_body (name) {
            		var src = target[name],
                    	copy = options[name];
            		if (target === copy) return;
            		if (deep && copy && typeof copy == "object" && !copy.nodeType) {
            			target[name] = jQuery.extend(deep, src || (copy.length != null ? [] : {}), copy);
            		}
            		else if (copy !== undefined) target[name] = copy;	      
            		else target[name] = copy;
             	})(name);
            }
            return target;
        };

        jQuery.extend({
            speed: function _speed (speed, easing, fn) {
                var opt = speed && speed.constructor == Object ? speed : {
                    complete: fn || !fn && easing || jQuery.isFunction(speed) && speed,
                    duration: speed,
                    easing: fn && easing || easing && easing.constructor != Function && easing
                };
                opt.duration = (opt.duration && opt.duration.constructor == Number ? opt.duration : jQuery.fx.speeds[opt.duration]) || jQuery.fx.speeds.def;
                opt.old = opt.complete;
                opt.complete = function _complete () {
                    if (opt.queue !== false) jQuery(this).dequeue();
                    if (jQuery.isFunction(opt.old)) opt.old.call(this);
                };
                return opt;
            },			
        });

    })();

