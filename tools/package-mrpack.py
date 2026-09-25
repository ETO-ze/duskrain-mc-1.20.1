"""Build a client-only Modrinth pack with official URLs, not redistributed third-party JARs."""
import hashlib
import json
import zipfile
import argparse
from pathlib import Path
from mod_stack import ROOT, locked, write_json
from client_visual_bundle import client_entries, install_path, visual_defaults

VERSION='2.2.0-preview'


def package(target):
    lock=locked()
    rows=client_entries(lock)
    files=[dict(path=install_path(e),hashes={'sha1':e['sha1'],'sha512':e['sha512']},
                env={'client':'required','server':'unsupported'},downloads=[e['url']],fileSize=e['size']) for e in rows]
    index=dict(formatVersion=1,game='minecraft',versionId=VERSION,name='DuskRain · 烟雨仙途',
               summary='含原创材质、三档配置与 Complementary Reimagined 官方光影下载；仅客户端。',files=files,
               dependencies={'minecraft':'1.20.1','forge':'47.4.10'})
    jar=ROOT/f'mod/build/libs/duskrain-{VERSION}.jar'
    target=target.resolve()
    if target.exists():raise ValueError('Existing pack preserved')
    target.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('modrinth.index.json',json.dumps(index,ensure_ascii=False,indent=2))
        z.write(jar,'client-overrides/mods/'+jar.name)
        z.write(ROOT/'deploy/pcl2/Setup.ini','client-overrides/PCL/Setup.ini')
        presets=ROOT/'assets/v22-client-defaults'
        for p in sorted(presets.rglob('*')):
            if p.is_file():z.write(p,'client-overrides/'+p.relative_to(presets).as_posix())
        for name,content in visual_defaults().items():
            z.writestr('client-overrides/'+name,content)
        for name in ('客户端使用说明.md','第三方模组与许可证.md'):
            z.write(ROOT/'docs'/name,'client-overrides/DuskRain说明/'+name)
    with zipfile.ZipFile(target) as z:
        if z.testzip():raise ValueError('CRC failed')
        parsed=json.loads(z.read('modrinth.index.json'))
        assert len(parsed['files'])==len(rows)
        assert all(set(f['hashes'])=={'sha1','sha512'} and f['env']['server']=='unsupported' for f in parsed['files'])
    report=dict(path=str(target),size=target.stat().st_size,sha256=hashlib.sha256(target.read_bytes()).hexdigest(),
                third_party_jars_rehosted=False,crc_verified=True,client_only=True,launcher_import_test='pending')
    write_json(ROOT/('docs/qa/'+target.stem+'-package.json'),report)
    print(report)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,default=ROOT/f'dist/DuskRain-{VERSION}.mrpack')
    package(parser.parse_args().output)
