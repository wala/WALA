var timerId = 0;

function setTimeout(cb, t) {
	cb();
	return timerId++;
}

function setInterval(cb, t) {
	return setTimeout(function () {
		cb();
		setInterval(cb, t);
	}, t);
}