(function(window, document) {
    'use strict';
    window.BJYOSliderCaptcha = function(options) {
        var root = document.getElementById('slider-captcha');
        var dialog = document.getElementById('slider-captcha-dialog');
        var trigger = document.getElementById('slider-captcha-trigger');
        var triggerIcon = document.getElementById('slider-trigger-icon');
        var triggerLabel = document.getElementById('slider-trigger-label');
        var triggerStatus = document.getElementById('slider-trigger-status');
        var background = document.getElementById('slider-background');
        var piece = document.getElementById('slider-piece');
        var overlay = document.getElementById('slider-captcha-loading');
        var track = document.getElementById('slider-track');
        var progress = document.getElementById('slider-progress');
        var tip = document.getElementById('slider-tip');
        var handle = document.getElementById('slider-handle');
        var status = document.getElementById('slider-status');
        var refresh = document.getElementById('slider-refresh');
        var challenge, proof, expiresAt = 0, left = 0, requestId = 0;
        var loading = false, verifying = false, dragging = false, pointerId, startX, startLeft;
        var expireTimer, closeTimer;

        function maxLeft() { return Math.max(1, track.clientWidth - handle.offsetWidth); }
        function offset() { return Math.round(left / maxLeft() * 268); }
        function render() {
            trigger.classList.toggle('verified', Boolean(proof));
            track.classList.toggle('verified', Boolean(proof));
            trigger.disabled = Boolean(proof);
            triggerIcon.textContent = proof ? '✓' : '◇';
            triggerLabel.textContent = proof ? '安全验证已通过' : '点击进行安全验证';
            handle.disabled = loading || verifying || !challenge || Boolean(proof);
            refresh.disabled = loading || verifying;
            overlay.hidden = !loading;
            background.hidden = piece.hidden = !challenge;
            tip.textContent = proof ? '验证通过' : verifying ? '正在验证…' : '按住滑块，拖动完成拼图';
            handle.style.left = left + 'px';
            progress.style.width = (left + 20) + 'px';
            handle.setAttribute('aria-valuenow', offset());
            if (challenge) {
                piece.style.top = (challenge.pieceY / 160 * 100) + '%';
                piece.style.width = (52 / 320 * 100) + '%';
                piece.style.left = (offset() / 320 * 100) + '%';
            }
        }
        function cancelDrag() {
            dragging = false;
            if (pointerId !== undefined && handle.hasPointerCapture(pointerId)) handle.releasePointerCapture(pointerId);
            pointerId = undefined;
        }
        function reset() {
            requestId++;
            cancelDrag();
            clearTimeout(expireTimer); clearTimeout(closeTimer);
            challenge = proof = undefined; expiresAt = 0; left = 0;
            loading = verifying = false; status.textContent = triggerStatus.textContent = '';
            render();
        }
        function request(url, data) {
            return new Promise(function(resolve, reject) {
                window.jQuery.ajax({
                    url: options.ctx + url, type: data ? 'POST' : 'GET', cache: false,
                    dataType: 'json', contentType: data ? 'application/json' : undefined,
                    data: data ? JSON.stringify(data) : undefined, timeout: 10000,
                    success: function(result) { if (result.code === 0) resolve(result); else reject(new Error('verification_failed')); },
                    error: function() { reject(new Error('network_error')); }
                });
            });
        }
        function preload(src) {
            return new Promise(function(resolve, reject) {
                var image = new Image(); image.onload = resolve; image.onerror = reject; image.src = src;
            });
        }
        async function load() {
            reset();
            var id = requestId;
            loading = true; render();
            try {
                var result = await request('captcha/slider');
                if (id !== requestId) return;
                var enabled = result.captchaEnabled !== false;
                options.onEnabled(enabled);
                if (!enabled) { root.hidden = true; dialog.close(); return; }
                var data = result.data;
                if (!data || !/^slider_[0-9a-f]{32}$/.test(data.uuid) || data.imageWidth !== 320
                    || data.imageHeight !== 160 || data.pieceSize !== 52 || !Number.isInteger(data.pieceY)
                    || data.pieceY < 0 || data.pieceY > 108
                    || ![data.backgroundImage, data.pieceImage].every(function(src) { return typeof src === 'string' && src.startsWith('data:image/png;base64,'); }))
                    throw new Error('invalid_challenge');
                await Promise.all([preload(data.backgroundImage), preload(data.pieceImage)]);
                if (id !== requestId) return;
                challenge = data; background.src = data.backgroundImage; piece.src = data.pieceImage;
            } catch (error) {
                if (id === requestId) status.textContent = '加载失败，请点击刷新重试';
            } finally { if (id === requestId) { loading = false; render(); } }
        }
        function open() {
            if (proof) return;
            if (!dialog.open) dialog.showModal();
            trigger.setAttribute('aria-expanded', 'true'); render();
            if (!challenge && !loading) load();
        }
        async function verify() {
            if (!challenge || loading || verifying || proof) return;
            var id = requestId, uuid = challenge.uuid, position = offset();
            verifying = true; status.textContent = ''; render();
            try {
                var result = await request('captcha/slider/verify', { uuid: uuid, offset: position });
                if (id !== requestId) return;
                if (typeof result.data !== 'string' || !/^[A-Za-z0-9_-]{32}$/.test(result.data)) throw new Error('invalid_proof');
                proof = { uuid: uuid, code: result.data }; expiresAt = Date.now() + 120000;
                closeTimer = setTimeout(function() { dialog.close(); }, 450);
                expireTimer = setTimeout(function() { reset(); triggerStatus.textContent = '验证已过期，请重新验证'; }, 120000);
            } catch (error) {
                if (id === requestId) { challenge = undefined; left = 0; status.textContent = '验证失败或已过期，请刷新重试'; }
            } finally { if (id === requestId) { verifying = false; render(); } }
        }
        trigger.addEventListener('click', open);
        refresh.addEventListener('click', load);
        document.getElementById('slider-captcha-close').addEventListener('click', function() { dialog.close(); });
        dialog.addEventListener('click', function(event) {
            var bounds = dialog.getBoundingClientRect();
            if (event.target === dialog && (event.clientX < bounds.left || event.clientX > bounds.right
                || event.clientY < bounds.top || event.clientY > bounds.bottom)) dialog.close();
        });
        dialog.addEventListener('close', function() {
            trigger.setAttribute('aria-expanded', 'false'); cancelDrag(); if (!proof) reset();
        });
        handle.addEventListener('pointerdown', function(event) {
            if (!challenge || loading || verifying || proof || (event.pointerType === 'mouse' && event.button !== 0)) return;
            dragging = true; pointerId = event.pointerId; startX = event.clientX; startLeft = left;
            handle.setPointerCapture(pointerId); event.preventDefault();
        });
        function move(event) {
            if (!dragging || event.pointerId !== pointerId) return;
            left = Math.max(0, Math.min(maxLeft(), startLeft + event.clientX - startX)); render(); event.preventDefault();
        }
        handle.addEventListener('pointermove', move);
        handle.addEventListener('pointerup', function(event) {
            if (!dragging || event.pointerId !== pointerId) return;
            move(event); cancelDrag(); verify();
        });
        handle.addEventListener('pointercancel', cancelDrag);
        handle.addEventListener('lostpointercapture', cancelDrag);
        handle.addEventListener('keydown', function(event) {
            if (!challenge || loading || verifying || proof) return;
            if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
                left = Math.max(0, Math.min(maxLeft(), left + (event.key === 'ArrowRight' ? 1 : -1) * maxLeft() / 268 * (event.shiftKey ? 10 : 1)));
                render(); event.preventDefault();
            } else if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); verify(); }
        });
        window.addEventListener('resize', function() { cancelDrag(); left = 0; render(); });
        this.open = open;
        this.reset = reset;
        this.getProof = function() { if (proof && Date.now() >= expiresAt) reset(); return proof; };
        load();
    };
})(window, document);
