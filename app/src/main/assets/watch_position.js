(function(){
 var videos=[];
 function scan(doc){
  doc.querySelectorAll('video').forEach(function(v){var r=v.getBoundingClientRect();if(r.width>100&&r.height>60&&isFinite(v.duration)&&v.duration>0)videos.push(v);});
  doc.querySelectorAll('iframe').forEach(function(f){try{if(f.contentDocument)scan(f.contentDocument);}catch(_){}});
 }
 scan(document);videos.sort(function(a,b){return Number(!b.paused)-Number(!a.paused)||b.duration-a.duration;});
 if(!videos.length)return null;
 var v=videos[0];
 if(window.__alicoResumeMs>0 && v.readyState>=1){
  var seconds=window.__alicoResumeMs/1000;
  if(seconds<v.duration-5){try{v.currentTime=seconds;}catch(_){return null;}}
  window.__alicoResumeMs=0;
 }
 if(v.currentTime<=0&&!v.ended)return null;
 return JSON.stringify({position:Math.round(v.currentTime*1000),duration:Math.round(v.duration*1000),url:location.href,title:document.title});
})();
