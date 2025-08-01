import "../common/modernizr-custom-webp"

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

window.globals = {
        // Debugging by adding 3 days and 15 hours and 40 minutes
        //nowOffset: 1000 * 60 * 60 * 24 * 1 + 1000 * 60 * 60 * 1 + 1000 * 60 * 40
		nowOffset: 0
};
