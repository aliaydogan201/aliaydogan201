(function () {
    if (window.__alicoStage) return window.__alicoStage.refresh();
    var target = null, style = null, touched = [], scrollX = 0, scrollY = 0, episodePanel = null, mode = "ready", selectedUrl = null;
    var attr = 'data-alico-stage';
    var wrappers = '.video-js,.jwplayer,.plyr,.html5-video-player,.mejs__container,.mejs-container,.flowplayer,.dplayer,.art-video-player';
    function visible(el) {
        var r = el.getBoundingClientRect(), s = getComputedStyle(el);
        return r.width >= 200 && r.height >= 100 && s.display !== 'none' && s.visibility !== 'hidden';
    }
    function find() {
        var list = [];
        document.querySelectorAll('video,iframe,' + wrappers).forEach(function(el) {
            if (!visible(el)) return;
            var tag = el.tagName, src = el.getAttribute('src') || '', identity = src + ' ' + el.id + ' ' + el.className + ' ' + (el.title || '');
            if (/doubleclick|googlesyndication|recaptcha|advert|adserver/i.test(identity)) return;
            if (tag === 'IFRAME' && !el.hasAttribute('allowfullscreen') && !/player|embed|video|vidmoly|youtube|dailymotion/i.test(identity) && !el.closest(wrappers)) return;
            var r = el.getBoundingClientRect();
            var score = r.width * r.height + (tag === 'VIDEO' && !el.paused ? 10000000 : 0);
            var chosen = el.closest(wrappers) || el;
            list.push({el: chosen, score:score});
        });
        list.sort(function(a,b){return b.score-a.score;});
        return list.length ? list[0].el : null;
    }
    function mark(el,value) { touched.push([el,el.getAttribute(attr)]); el.setAttribute(attr,value); }
    function restore() {
        if (style) style.remove();
        touched.forEach(function(pair){if(pair[1]===null)pair[0].removeAttribute(attr);else pair[0].setAttribute(attr,pair[1]);});
        touched=[];style=null;target=null;
        if(episodePanel){episodePanel.remove();episodePanel=null;}
        window.scrollTo(scrollX,scrollY);
        return 'site';
    }
    function apply(el) {
        scrollX=window.scrollX;scrollY=window.scrollY;target=el;
        mark(el,'target');
        if(el===episodePanel)el.setAttribute('data-alico-episodes','true');
        var branch=el;
        while(branch.parentElement){
            var parent=branch.parentElement;
            Array.prototype.forEach.call(parent.children,function(sibling){
                if(sibling!==branch && !/^(SCRIPT|STYLE|LINK|META|HEAD)$/.test(sibling.tagName))mark(sibling,'hidden');
            });
            mark(parent,'ancestor');branch=parent;
            if(parent===document.documentElement)break;
        }
        style=document.createElement('style');
        style.textContent =
            '[data-alico-stage="hidden"]{display:none!important}' +
            '[data-alico-stage="ancestor"]{transform:none!important;filter:none!important;perspective:none!important;contain:none!important;clip-path:none!important;overflow:visible!important;opacity:1!important;visibility:visible!important;background:#000!important}' +
            'html[data-alico-stage],body[data-alico-stage]{margin:0!important;padding:0!important;width:100%!important;height:100%!important;overflow:hidden!important}' +
            '[data-alico-stage="target"]{position:fixed!important;inset:0!important;top:0!important;left:0!important;width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;min-width:0!important;min-height:0!important;margin:0!important;padding:0!important;transform:none!important;aspect-ratio:auto!important;z-index:2147483000!important;visibility:visible!important;opacity:1!important;background:#000!important}' +
            'video[data-alico-stage="target"], [data-alico-stage="target"] video{object-fit:contain!important;width:100%!important;height:100%!important}' +
            'iframe[data-alico-stage="target"]{border:0!important}' +
            '[data-alico-episodes]{overflow:auto!important;box-sizing:border-box!important;padding:24px!important;color:#fff!important;font:20px sans-serif!important}' +
            '[data-alico-episodes] a{display:block!important;background:#512700!important;color:#fff!important;padding:16px!important;margin:10px 0!important;text-decoration:none!important;border:3px solid transparent!important;border-radius:8px!important}' +
            '[data-alico-episodes] a:focus{background:#ffeb3b!important;color:#000!important;border-color:white!important;outline:none!important}';
        (document.head||document.documentElement).appendChild(style);
        // Preserve the original DOM node: moving/reloading an iframe would lose its session.
        return 'ready';
    }
    function episodeLinks() {
        var found=[], seen={};
        document.querySelectorAll('a[href]').forEach(function(a){
            if(a.closest('[data-alico-episodes]'))return;
            var href=a.href, label=(a.getAttribute('title')||a.textContent||'').replace(/\s+/g,' ').trim();
            if(!/^https?:/.test(href)||seen[href])return;
            if(/fragman|trailer|sahne|foto|haber|ozet|özet/i.test(label+' '+href))return;
            var episode=/(?:\d+\s*\.?\s*bölüm|bölüm\s*\d+|episode\s*\d+|\d+[-.]bolum)/i.test(label+' '+href);
            var paging= /[?&](page|activeSeasonNumber|season)=/i.test(href) && /sezon|sayfa|sonraki|önceki|^\d+$/i.test(label);
            if(!episode&&!paging)return;
            if(!label)label=decodeURIComponent(href.split('/').filter(Boolean).pop()).replace(/-/g,' ');
            seen[href]=true;found.push({url:href,label:label.slice(0,180),episode:episode});
        });
        return found;
    }
    function showEpisodes(links){
        mode='episodes';
        episodePanel=document.createElement('div');
        var heading=document.createElement('h2');heading.textContent='Bölümler';episodePanel.appendChild(heading);
        links.forEach(function(link){
            var a=document.createElement('a');a.href=link.url;a.textContent=link.label;
            a.addEventListener('click',function(){selectedUrl=link.url;restore();});
            episodePanel.appendChild(a);
        });
        episodePanel.addEventListener('keydown',function(e){
            if(e.key!=='ArrowDown'&&e.key!=='ArrowUp')return;
            var items=Array.prototype.slice.call(episodePanel.querySelectorAll('a'));
            var i=items.indexOf(document.activeElement);
            var next=e.key==='ArrowDown'?i+1:i-1;
            if(next>=0&&next<items.length){e.preventDefault();items[next].focus();items[next].scrollIntoView({block:'nearest'});}
        });
        document.body.appendChild(episodePanel);apply(episodePanel);
        var first=episodePanel.querySelector('a');if(first)first.focus();
        return 'episodes';
    }
    function refresh(){
        if(target && target.isConnected)return mode;
        if(target)restore();
        // Episode pages prefer the video; series indexes prefer their numbered links.
        var path=location.pathname;
        var episodePage=/\d+[-.]bolum|bolum[-/]\d+|sezon-\d+-bolum-\d+|\/watch|\/embed\//i.test(path) || selectedUrl===location.href;
        if(window.__alicoSeriesMode && !episodePage && !window.__alicoForcePlayer){
            var links=episodeLinks();
            if(links.filter(function(x){return x.episode;}).length>1)return showEpisodes(links);
            // Wait for a list instead of showing the featured video on a series index.
            return 'waiting';
        }
        var el=find();mode='ready';return el?apply(el):'waiting';
    }
    function navigateEpisodeUp(){
        if(mode!=='episodes' || !episodePanel || !episodePanel.isConnected)return 'toolbar';
        var items=Array.prototype.slice.call(episodePanel.querySelectorAll('a'));
        var i=items.indexOf(document.activeElement);
        if(i<=0)return 'toolbar';
        items[i-1].focus();
        items[i-1].scrollIntoView({block:'nearest'});
        return 'moved';
    }
    function focusPlayer(){
        if(mode==='episodes' && episodePanel && episodePanel.isConnected){
            var item=episodePanel.contains(document.activeElement)?document.activeElement:episodePanel.querySelector('a');
            if(item){item.focus();item.scrollIntoView({block:'nearest'});return 'episode-focused';}
        }
        if(!target || !target.isConnected)return 'missing';
        var control=target.matches('iframe,video')?target:target.querySelector('iframe,button,[role="button"],video');
        if(!control)control=target;
        if(!control.hasAttribute('tabindex'))control.setAttribute('tabindex','0');
        control.focus();
        control.scrollIntoView({block:'nearest'});
        return control.tagName==='IFRAME'?'frame-focused':'focused';
    }
    var lastPlayTarget=null;
    function tryPlay(){
        if(!target || !target.isConnected || mode==='episodes')return 'not-player';
        if(lastPlayTarget===target)return 'already-attempted';
        lastPlayTarget=target;
        var scope=target;
        if(target.tagName==='IFRAME'){
            try{scope=target.contentDocument;if(!scope)return 'frame-unavailable';}
            catch(_){return 'frame-unavailable';}
        }
        var video=scope.tagName==='VIDEO'?scope:scope.querySelector('video');
        if(video && !video.paused)return 'already-playing';
        var selector='.vjs-big-play-button,.jw-display-icon-container .jw-icon-display,.jw-display-icon-display,.plyr__control--overlaid,.ytp-large-play-button,.dplayer-play-icon,[data-plyr="play"]';
        var button=scope.querySelector(selector);
        if(button){button.click();return 'play-clicked';}
        if(video){try{var result=video.play();if(result&&result.catch)result.catch(function(){});return 'play-requested';}catch(_){return 'play-rejected';}}
        return 'manual-play-needed';
    }
    function tapPoint(){
        if(!target||!target.isConnected||mode==='episodes')return null;
        var frame=target.tagName==='IFRAME'?target:target.querySelector('iframe');
        if(!frame)return null;
        var r=frame.getBoundingClientRect();
        if(r.width<200||r.height<100)return null;
        return {x:(r.left+r.width/2)/window.innerWidth,y:(r.top+r.height/2)/window.innerHeight};
    }
    function openPlayer(){
        window.__alicoForcePlayer=true;
        if(mode==='episodes')restore();
        var result=refresh();
        if(result==='ready'){focusPlayer();lastPlayTarget=null;}
        return result;
    }
    window.__alicoStage={refresh:refresh,navigateEpisodeUp:navigateEpisodeUp,restore:restore,openPlayer:openPlayer,focusPlayer:focusPlayer,tryPlay:tryPlay,tapPoint:tapPoint};
    return refresh();
})();
