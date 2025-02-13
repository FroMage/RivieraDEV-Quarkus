const burgerMenuId = 'js-burgerMenu-input';

const forceToggleBurgerMenu = function() {
    const burgerMenu = document.getElementById(burgerMenuId);
    burgerMenu.click();
};

$(function() {
    // Mobile Menu -------
    window.initBurgerMenu = () => {
        const burgerMenu = document.getElementById(burgerMenuId);
        const burgerMenuOpenClass = 'burgerMenu--open';
        if (burgerMenu) {
            burgerMenu.addEventListener('change', function(event) {
                let body = document.getElementsByTagName('body')[0];
                if (event.target.checked) {
                    body.classList.add(burgerMenuOpenClass);
                } else {
                    body.classList.remove(burgerMenuOpenClass);
                }
            });
        }
    };
});
