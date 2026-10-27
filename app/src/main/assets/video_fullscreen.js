(function () {
    var candidates = [], documents = [];
    var known = '.vjs-fullscreen-control,.jw-icon-fullscreen,[data-plyr="fullscreen"],.ytp-fullscreen-button,.mejs__fullscreen-button button,.mejs-fullscreen-button button,.fp-fullscreen,.dplayer-full-icon,.art-icon-fullscreen';
    function visible(el) {
        var r = el.getBoundingClientRect();
        var style = el.ownerDocument.defaultView.getComputedStyle(el);
        return r.width > 0 && r.height > 0 && style.display !== 'none' && style.visibility !== 'hidden';
    }
    function collect(doc) {
        documents.push(doc);
        Array.prototype.forEach.call(doc.querySelectorAll('video'), function (v) {
            if (!visible(v)) return;
            var r = v.getBoundingClientRect();
            candidates.push({video:v, area:r.width*r.height, playing:!v.paused});
        });
        Array.prototype.forEach.call(doc.querySelectorAll('iframe'), function (frame) {
            try { if (visible(frame) && frame.contentDocument) collect(frame.contentDocument); } catch (_) {}
        });
    }
    function control(scope, generic) {
        var list = Array.prototype.slice.call(scope.querySelectorAll(known));
        if (generic) {
            Array.prototype.forEach.call(scope.querySelectorAll('button,[role="button"],a,[tabindex]'), function (el) {
                var label = ['aria-label','title','data-tooltip','data-title'].map(function(a){return el.getAttribute(a)||'';}).join(' ')+' '+(el.textContent||'').trim();
                var icon = el.querySelector('svg use');
                if (icon) label += ' '+(icon.getAttribute('href')||icon.getAttribute('xlink:href')||'');
                if (/exit|çık|kapat|küçült|restore|minimize/i.test(label)) return;
                if (/full[\s_-]*screen|tam[\s_-]*ekran|genişlet|expand|⛶|\[\s*\]/i.test(label)) list.push(el);
            });
        }
        return list.filter(function(el){return visible(el) && !el.disabled && el.getAttribute('aria-disabled')!=='true';})[0];
    }
    collect(document);
    if (documents.some(function(d){return d.fullscreenElement || d.webkitFullscreenElement;})) return 'already-fullscreen';
    candidates.sort(function(a,b){return Number(b.playing)-Number(a.playing)||b.area-a.area;});
    var video = candidates.length ? candidates[0].video : null;
    if (video) {
        var win = video.ownerDocument.defaultView;
        video.dispatchEvent(new win.MouseEvent('mousemove',{bubbles:true}));
        var scope=video.closest('.video-js,.jwplayer,.plyr,.html5-video-player,.mejs__container,.mejs-container,.flowplayer,.dplayer,.art-video-player') || video.parentElement;
        // Search only near this video, never arbitrary page controls.
        for(var i=0;scope && i<4 && scope!==video.ownerDocument.body && scope!==video.ownerDocument.documentElement;i++,scope=scope.parentElement){
            scope.dispatchEvent(new win.MouseEvent('mousemove',{bubbles:true}));
            var button=control(scope,true);
            if(button){button.click();return 'player-control-requested';}
        }
    }
    // Some players expose their controls before creating their video element.
    for(var d=0;d<documents.length;d++){
        var exact=control(documents[d],false);
        if(exact){exact.click();return 'player-control-requested';}
    }
    if(!video)return 'no-accessible-player';
    var request=video.requestFullscreen||video.webkitRequestFullscreen||video.webkitEnterFullscreen;
    if(!request)return 'unsupported';
    try{var result=request.call(video);if(result&&result.catch)result.catch(function(){});return 'video-fullscreen-requested';}
    catch(_){return 'rejected';}
})();
