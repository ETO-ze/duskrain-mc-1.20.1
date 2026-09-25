"""Client-only release staging. Never copies worlds, runtimes, server files or private data."""
import argparse
import hashlib
import shutil
import zipfile
from mod_stack import ROOT, locked, obtain, selected, inspect_jar, write_json, read_json
from client_visual_bundle import client_entries, visual_defaults

VERSION = '2.2.0-preview'


def package(output):
    lock = locked()
    jar = ROOT / f'mod/build/libs/duskrain-{VERSION}.jar'
    if not jar.exists():
        raise ValueError('Build the release JAR first')
    meta = inspect_jar(jar)
    if meta['mod_ids'] != ['duskrain']:
        raise ValueError('Wrong first-party artifact')
    with zipfile.ZipFile(jar) as z:
        if f'version="{VERSION}"' not in z.read('META-INF/mods.toml').decode():
            raise ValueError('Wrong version inside release JAR')
        if any(n.startswith('journeymap/') for n in z.namelist()):
            raise ValueError('JourneyMap API must not be redistributed inside DuskRain')
    for item in client_entries(lock):
        obtain(item)
    output = output.resolve()
    if output.exists():
        raise ValueError('Choose a new stage directory; existing release was preserved: ' + str(output))
    output.mkdir(parents=True)
    client = client_entries(lock)
    (output/'mods').mkdir()
    shutil.copy2(jar, output/'mods'/jar.name)
    write_json(output/'client-mods.lock.json', dict(version=VERSION,minecraft='1.20.1',forge='47.4.10',java=17,
               entries=client,optional=[e for e in lock['entries'] if 'client' in e['sides'] and e not in client]))
    shutil.copy2(ROOT/'tools/install-client-mods.ps1', output/'install-client-mods.ps1')
    shutil.copytree(ROOT/'assets/v22-client-defaults',output/'defaults')
    for name,content in visual_defaults().items():
        path=output/'defaults'/name
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(content,encoding='utf8')
    for name in ('客户端使用说明.md','第三方模组与许可证.md'):
        shutil.copy2(ROOT/'docs'/name,output/name)
    write_json(output/'dependency-report.json', {e['project']:e['dependencies'] for e in client})
    write_json(output/'license-report.json', [dict(project=e['project'],authors=e['authors'],license=e['license'],
               source=e['source_url'],homepage=e['homepage'],delivery=e['redistribution'],includes_jar=False,
               permission_review=e['permission_review']) for e in client])
    (output/'安装说明.txt').write_text('DuskRain 2.2.0-preview 客户端模组\n'
        '先在启动器创建 Minecraft 1.20.1 + Forge 47.4.10 独立实例，选择 Java 17。\n'
        '关闭该实例后，将本包解压到实例目录，运行 install-client-mods.ps1。\n'
        '脚本仅下载锁定的客户端组件；第三方 JAR 从官方 Modrinth 获取并验证哈希。\n'
        '发现冲突版本会停止，不会删除你的其他模组或覆盖配置。\n'
        '外置登录：https://skin.duskrain.cn/authlib-injector\n'
        '服务器：nbc.rainplay.cn:42741；群号：205255670。\n'
        'J 地图；G 菜单；Z/X/C 技能；R 御剑；F8 小地图；F9 HUD。\n'
        'Complementary Reimagined r5.3 默认从官方源下载，初始为轻岚档；G → 显示设置 → 画质选择三档或关闭。\n'
        '原创中式模型与材质已内置 DuskRain 模组，生存世界保持原版方块贴图。\n'
        '没有附带世界、服务端、Java、账号或会话。声音增强和 3D Skin Layers 为可选下载。\n',encoding='utf8')
    hashes = {p.relative_to(output).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
              for p in sorted(output.rglob('*')) if p.is_file()}
    write_json(output/'checksums.json',hashes)
    archive = output.parent / (output.name + '.zip')
    if archive.exists():
        raise ValueError('Archive already exists')
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for p in sorted(output.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(output).as_posix())
    with zipfile.ZipFile(archive) as z:
        if z.testzip():raise ValueError('Archive CRC failure')
        for name, digest in hashes.items():
            if hashlib.sha256(z.read(name)).hexdigest()!=digest:raise ValueError('Archive hash failure')
        if any(n.startswith(('server/','world/','runtime/','saves/','.private/')) for n in z.namelist()):
            raise ValueError('Non-client files detected')
    return {'archive':str(archive),'size':archive.stat().st_size,'sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),
            'third_party_download_bytes':sum(e['size'] for e in client),'client_mods':sum(e['kind']=='mod' for e in client)+1,
            'crc_verified':True,'worlds_included':False,'third_party_jars_rehosted':False}


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=__import__('pathlib').Path,default=ROOT/f'dist/DuskRain-{VERSION}-client-mods')
    args=p.parse_args()
    report=package(args.output)
    write_json(ROOT/('docs/qa/'+args.output.name+'-package.json'),report)
    print(report)
