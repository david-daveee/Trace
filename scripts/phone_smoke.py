"""Bounded smoke test on the authorized connected phone. No history entries are created."""
import subprocess,sys,xml.etree.ElementTree as ET,json,re,time,os
from pathlib import Path
adb=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe')
root=Path(__file__).resolve().parent.parent
def run(*args):
    serial=os.environ.get('ANDROID_SERIAL')
    return subprocess.check_output([adb,*(['-s',serial] if serial else []),*args],encoding='utf-8',errors='replace')
def ui():
    run('shell','uiautomator','dump','/data/local/tmp/podhod-smoke.xml')
    return ET.fromstring(run('shell','cat','/data/local/tmp/podhod-smoke.xml'))
def tap(n):
    nums=list(map(int,re.findall(r'\d+',n.attrib['bounds'])))
    run('shell','input','tap',str((nums[0]+nums[2])//2),str((nums[1]+nums[3])//2))
def state():
    xml=ET.fromstring(run('shell','run-as','com.podhod.app','cat','shared_prefs/podhod.xml'))
    return json.loads(next(n.text for n in xml if n.get('name')=='state'))
def button(tree,text):
    return next((n for n in tree.iter('node') if text in n.get('text','')),None)
def notification():
    run('shell','cmd','statusbar','expand-notifications')
    tree=ui()
    title=button(tree,'Присед ·')
    if title is None: raise RuntimeError('Workout notification not visible')
    parents={c:p for p in tree.iter() for c in p}
    ancestor=title
    while ancestor is not None:
        expand=next((n for n in ancestor.iter('node') if n.get('resource-id')=='android:id/expand_button'),None)
        if expand is not None:
            if expand.get('content-desc')=='Expand':tap(expand)
            break
        ancestor=parents.get(ancestor)
    return ui()
sys.stdout.reconfigure(encoding='utf-8')
initial=state()
assert initial.get('active',{}).get('cursor')==0,'Expected untouched first approach; stop to preserve user state'
old=run('shell','settings','get','global','stay_on_while_plugged_in').strip()
completed=False
try:
    run('shell','settings','put','global','stay_on_while_plugged_in','3')
    run('shell','input','keyevent','KEYCODE_WAKEUP')
    run('shell','am','force-stop','com.podhod.app')
    run('shell','am','start','-W','-n','com.podhod.app/.MainActivity','--ez','workout','true')
    tree=ui();assert button(tree,'0 из 36') is not None,'Session was not resumed'
    run('shell','screencap','-p','/data/local/tmp/podhod-workout.png');run('pull','/data/local/tmp/podhod-workout.png',str(root/'.tools/podhod-workout.png'))
    tree=notification();done=button(tree,'Готово');assert done is not None,'No Done action'
    tap(done);completed=True;time.sleep(.5)
    assert state()['active']['cursor']==1,'Notification did not advance'
    print('PASS: notification Done persisted one approach')
    tree=ui();undo=button(tree,'Назад')
    if undo is None:tree=notification();undo=button(tree,'Назад')
    assert undo is not None,'No Undo action';tap(undo);time.sleep(.5)
    assert state()['active']['cursor']==0,'Undo did not restore approach';completed=False
    print('PASS: notification Undo restored first approach')
    run('shell','cmd','statusbar','collapse')
    run('shell','am','force-stop','com.podhod.app')
    run('shell','am','start','-W','-n','com.podhod.app/.MainActivity','--ez','workout','true')
    tree=ui();assert button(tree,'0 из 36') is not None
    assert len(state()['history'])==len(initial['history'])
    print('PASS: process restart restored session; no test history added')
finally:
    run('shell','settings','put','global','stay_on_while_plugged_in',old)
    if completed:print('ATTENTION: one test approach remains marked; undo it in the app')
