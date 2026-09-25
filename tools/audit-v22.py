"""Audit the locked stack, nested dependencies and any actual installed side."""
import argparse,hashlib,io,json,sys,tomllib,zipfile
from pathlib import Path
from mod_stack import ROOT,locked,selected,obtain,inspect_jar,write_json


def embedded(path):
    result=[]
    def visit(data,name):
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            if 'META-INF/mods.toml' in z.namelist():
                meta=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))
                result.extend({'id':m['modId'],'version':m.get('version'),'artifact':name} for m in meta.get('mods',[]))
            if 'META-INF/jarjar/metadata.json' in z.namelist():
                for dep in json.loads(z.read('META-INF/jarjar/metadata.json'))['jars']:
                    visit(z.read(dep['path']),name+'!/'+dep['path'])
    visit(path.read_bytes(),path.name)
    return result


def audit(side,directory=None):
    lock=locked();rows=selected(lock,side)
    found={};nested=[]
    for e in rows:
        path,_=obtain(e)
        if e['redistribution']!='official_download_only':raise ValueError('Unreviewed redistribution')
        if not e['authors'] or not e['license']['id'] or not e['sha1'] or not e['sha256'] or not e['sha512']:
            raise ValueError('Incomplete provenance: '+e['project'])
        meta=inspect_jar(path)
        for mid in meta['mod_ids']:
            if mid in found:raise ValueError('Duplicate mod ID: '+mid)
            found[mid]=e['file']
        nested.extend(embedded(path))
    ids={i['id'] for i in nested}|{'forge','minecraft'}
    for e in rows:
        for deps in e['forge_dependencies'].values():
            for d in deps:
                if d.get('mandatory') and d.get('side','BOTH') in ('BOTH',side.upper()) and d['modId'] not in ids:
                    raise ValueError('Forge mandatory dependency missing: '+d['modId'])
    if directory:
        expected={e['file']:e['sha256'] for e in rows}
        own=ROOT/'mod/build/libs/duskrain-2.2.0-preview.jar'
        expected[own.name]=hashlib.sha256(own.read_bytes()).hexdigest()
        actual={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in directory.glob('*.jar')}
        if actual!=expected:raise ValueError('Installed stack differs: '+repr({'missing':sorted(expected.keys()-actual.keys()),'extra':sorted(actual.keys()-expected.keys()),'changed':[n for n in expected.keys()&actual.keys() if expected[n]!=actual[n]]}))
    return dict(side=side,projects=len(rows),java17_bytecode_verified=True,official_hashes_verified=True,
                source_and_license_recorded=True,dependency_ids_resolved=True,nested_artifacts=nested,
                mod_ids=found,installed_directory=str(directory) if directory else None,
                version_ranges='Forge runtime startup is the authoritative range check',passed=True)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--side',choices=['client','server']);p.add_argument('--mods',type=Path)
    a=p.parse_args();result=[audit(s,a.mods) for s in ([a.side] if a.side else ['client','server'])]
    write_json(ROOT/'docs/qa/v22-mod-stack-audit.json',result)
    print(json.dumps([dict(side=r['side'],projects=r['projects'],passed=r['passed']) for r in result]))
