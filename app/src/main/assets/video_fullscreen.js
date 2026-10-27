(function () {
    // Only video/player controls are eligible; never fullscreen the page or iframe.
    var candidates = [];
    function visible(el) {
        var r = el.getBoundingClientRect();
        return r.width > 0 && r.height > 0;
    }
    function collect(doc) {
        var videos = Array.prototype.slice.call(doc.querySelectorAll('video')).filter(visible);
        videos.forEach(function (video) {
            var r = video.getBoundingClientRect();
            candidates.push({video: video, doc: doc, area: r.width * r.height, playing: !video.paused});
        });
        Array.prototype.forEach.call(doc.querySelectorAll('iframe'), function (frame) {
            try { if (visible(frame) && frame.contentDocument) collect(frame.contentDocument); } catch (_) {}
        });
    }
    collect(document);
    candidates.sort(function(a,b) { return Number(b.playing)-Number(a.playing) || b.area-a.area; });
    if (!candidates.length) return 'no-accessible-video';
    var item = candidates[0];
    var player = item.video.closest('.video-js,.jwplayer,.plyr,.html5-video-player,.vjs-player');
    if (player) {
        var control = player.querySelector('.vjs-fullscreen-control,.jw-icon-fullscreen,[data-plyr="fullscreen"],.ytp-fullscreen-button');
        if (control && visible(control)) { control.click(); return 'player-control-requested'; }
    }
    var request = item.video.requestFullscreen || item.video.webkitRequestFullscreen || item.video.webkitEnterFullscreen;
    if (!request) return 'unsupported';
    try {
        var result = request.call(item.video);
        if (result && result.catch) result.catch(function () {});
        return 'video-fullscreen-requested';
    } catch (_) { return 'rejected'; }
})();
