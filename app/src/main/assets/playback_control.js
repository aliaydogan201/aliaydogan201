// Search accessible documents only; cross-origin frames remain protected.
var videos = [];
function scan(doc) {
    try {
        Array.prototype.forEach.call(doc.querySelectorAll('video'), function(video) {
            var rect = video.getBoundingClientRect();
            if (rect.width > 0 && rect.height > 0 && !video.ended) videos.push(video);
        });
        Array.prototype.forEach.call(doc.querySelectorAll('iframe'), function(frame) {
            try { if (frame.contentDocument) scan(frame.contentDocument); } catch (_) {}
        });
    } catch (_) {}
}
scan(document);
videos.sort(function(a,b) {
    if (a.paused !== b.paused) return a.paused ? 1 : -1;
    return b.clientWidth*b.clientHeight - a.clientWidth*a.clientHeight;
});
var video = videos[0];
if (!video) return 'unavailable';
try {
    if (action === 'pause') video.pause();
    else { var result = video.play(); if (result && result.catch) result.catch(function(){}); }
    return action === 'pause' && !video.paused ? 'unavailable' : 'ok';
} catch (_) { return 'unavailable'; }
