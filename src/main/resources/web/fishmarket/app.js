import Reveal from "reveal.js";
import "reveal.js/css/reveal.css";

Reveal.initialize();
var runningTimers = new Array(Reveal.getTotalSlides());

Reveal.addKeyBinding( { keyCode: 13, key: 'Enter', description: 'Start timer' }, function() {
	const idx = Reveal.getIndices().h;
	if (runningTimers[idx]) {
		console.log("Timer already running");
		return;
	}
	var timeLeft = 30;
	var timer = Reveal.getCurrentSlide().getElementsByClassName('timer')[0];
	timer.classList.add('running')
	runningTimers[idx] = setInterval(function() {
		timeLeft -= 1;
		if (timeLeft == 0) {
			clearInterval(runningTimers[idx]);
			timer.classList.replace('running', 'done')
		}
		timer.innerText = timeLeft;
	}, 1000)
} )

Reveal.addKeyBinding( { keyCode: 82, key: 'R', description: 'Reset timer' }, function() {
	const idx = Reveal.getIndices().h;
	clearInterval(runningTimers[idx]);
	runningTimers[idx] = false;
	var timer = Reveal.getCurrentSlide().getElementsByClassName('timer')[0];
	timer.classList.remove('running', 'done')
	timer.innerText = 30;
} )
