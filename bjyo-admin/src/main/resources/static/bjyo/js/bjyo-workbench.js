(function () {
    'use strict';
    document.addEventListener('click', function (event) {
        if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        var link = event.target.closest('a[data-bjyo-tab]');
        if (!link || !window.frameElement || typeof window.createMenuItem !== 'function') return;
        event.preventDefault();
        window.createMenuItem(link.getAttribute('href'), link.getAttribute('data-bjyo-tab'));
    });
}());
