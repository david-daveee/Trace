import csv, json, re, math
from pathlib import Path
root=Path(__file__).resolve().parent.parent
rows={}
for sheet,r,c,value,formula in csv.reader((root/'.tools/workbook.tsv').open(encoding='utf-8'),delimiter='\t'):
    if sheet=='План': rows.setdefault(int(r),{})[int(c)]=(value,formula)
p={'name':'Шейко · КМС / МС','description':'Подготовительный цикл · 4 недели · Б. И. Шейко','maxima':{'squat':180,'bench':130,'deadlift':190},'step':2.5,'restSeconds':120,'days':[]}
week=''; day=None; checked=0
for r,cells in sorted(rows.items()):
    if r<6: continue
    if 1 in cells: week=cells[1][0]
    name=cells.get(4,('', ''))[0]
    if not name: continue
    if cells.get(3,('', ''))[0]=='1':
        day={'name':week+' · День '+str(1+sum(d['name'].startswith(week+' ·') for d in p['days'])),'groups':[]}
        p['days'].append(day)
    assert day is not None
    for c in range(5,24,2):
        desc=cells.get(c+1,('', ''))[0]
        if not desc: continue
        m=re.fullmatch(r'[хx×](\d+)[хx×](\d+)',desc)
        assert m,(r,c,desc)
        g={'exercise':name,'sets':int(m[1]),'reps':int(m[2]),'sourceRow':r}
        val,formula=cells.get(c,('', ''))
        g['kg']=float(val) if val else -1
        if formula:
            expr=formula.replace('$','')
            m=re.fullmatch(r'(?:ROUND\()?B([123])\*([0-9.]+)(?:/2.5,0.0\)\*2.5)?',expr)
            assert m,(r,c,formula)
            lift=['squat','bench','deadlift'][int(m[1])-1]
            g.update(lift=lift,factor=float(m[2]),round=2.5 if expr.startswith('ROUND') else 0)
            raw=p['maxima'][lift]*g['factor']; calc=math.floor(raw/2.5+0.5)*2.5 if g['round'] else raw
            assert abs(calc-g['kg'])<.00001,(r,c,calc,g['kg'])
            checked+=1
        day['groups'].append(g)
out=root/'app/src/main/assets';out.mkdir(parents=True,exist_ok=True)
(out/'sheiko.json').write_text(json.dumps(p,ensure_ascii=False,indent=2),encoding='utf-8')
print('Days:',len(p['days']),'Groups:',sum(len(d['groups']) for d in p['days']),'Sets:',sum(g['sets'] for d in p['days'] for g in d['groups']),'Weight formulas verified:',checked)
