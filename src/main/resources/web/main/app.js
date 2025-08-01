import "../common/modernizr-custom-webp"
import jQuery from "jquery"

window.$ = jQuery
window.jQuery = jQuery
// Countdown requires jQuery and can only be loaded via require, not import
require("jquery-countdown")

// Detect if webp is supported by the web browser
Modernizr.on('webp', function (isSupported) {
    let body = document.getElementsByTagName('body')[0];
    if (isSupported) {
        // Has WebP support
        body.classList.add('webp');
    }
    else {
        // No WebP support
        body.classList.add('no-webp');
    }
});
