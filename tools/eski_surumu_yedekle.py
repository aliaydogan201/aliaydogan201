import argparse, io, json, subprocess, tarfile
import xml.etree.ElementTree as ET
from pathlib import Path
PREFS = ['cinebox','cinebox_web','archive_sync','alico_history','alico_series']
FILES = ['archive_dynamic.json','archive_episodes.json']
def convert(data):
    contents = {}
    allowed = {'files/'+x for x in FILES} | {'shared_prefs/'+x+'.xml' for x in PREFS}
    with tarfile.open(fileobj=io.BytesIO(data),mode='r:*') as tar:
        for member in tar.getmembers():
            name=member.name.removeprefix('./')
            if member.isfile() and name in allowed:
                if member.size>20*1024*1024: raise ValueError('Dosya çok büyük')
                contents[name]=tar.extractfile(member).read()
    if not contents: raise ValueError('Uygulama verisi yok; boş yedek oluşturulmadı.')
    fs={x:json.loads(contents.get('files/'+x,b'{}')) for x in FILES}
    ps={}
    for name in PREFS:
        entries={}
        for item in ET.fromstring(contents.get('shared_prefs/'+name+'.xml',b'<map/>')):
            kind=item.tag
            if kind=='string': value=item.text or ''
            elif kind=='boolean': value=item.attrib['value']=='true'
            elif kind in ('int','long'): value=int(item.attrib['value'])
            elif kind=='float': value=float(item.attrib['value'])
            elif kind=='set': value=[v.text or '' for v in item]
            else: raise ValueError('Geçersiz ayar türü')
            entries[item.attrib['name']]={'type':kind,'value':value}
        ps[name]=entries
    return {'format':'ALICOTV_BACKUP','version':1,'files':fs,'prefs':ps}
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--serial')
    parser.add_argument('--output',default='ALICOTV-eski-surum-yedek.json')
    args=parser.parse_args()
    adb=['adb']+(['-s',args.serial] if args.serial else [])
    subprocess.run(adb+['shell','am','force-stop','com.cinebox.tv'],check=True)
    result=subprocess.run(adb+['exec-out','run-as','com.cinebox.tv','tar','-cf','-','files','shared_prefs'],capture_output=True)
    if result.returncode: raise RuntimeError('ADB/run-as erişilemedi. Uygulamayı kaldırmayın; cihaz yetkisini kontrol edin.')
    data=convert(result.stdout)
    with Path(args.output).open('x',encoding='utf-8') as f: json.dump(data,f,ensure_ascii=False,indent=2)
    print('Yedek kaydedildi:',Path(args.output).resolve())
    print('Ek içerik:',len(data['files']['archive_dynamic.json'].get('movies',[])))
if __name__=='__main__': main()
