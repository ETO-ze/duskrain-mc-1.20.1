"""Locked Modrinth supply chain. No updater touches a running Minecraft instance."""
from __future__ import annotations
import concurrent.futures
import hashlib
import json
from pathlib import Path
import time
import urllib.parse
import urllib.request
import zipfile
import tomllib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'assets/third-party'
LOCK = ASSETS / 'modrinth-lock.json'
CACHE = ROOT / '.deps/modrinth-v22'
API = 'https://api.modrinth.com/v2/'
MC = '1.20.1'
FORGE = '47.4.10'


def read_json(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name + '.new')
    temp.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    temp.replace(path)


def request(url):
    if not url.startswith('https://'):
        raise ValueError('HTTPS required: ' + url)
    for attempt in range(3):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'DuskRain/2.2 (github.com/ETO-ze/duskrain-mc-1.20.1)'})
            with urllib.request.urlopen(req, timeout=45) as response:
                if not response.url.startswith('https://'):
                    raise ValueError('Insecure redirect')
                return response.read()
        except (OSError, TimeoutError):
            if attempt == 2:
                raise
            time.sleep(attempt + 1)


def api(path):
    return json.loads(request(API + path))


def digests(data):
    return {name: hashlib.new(name, data).hexdigest() for name in ('sha1', 'sha256', 'sha512')}


def verify(data, item):
    actual = digests(data)
    for name in ('sha1', 'sha512', 'sha256'):
        if item.get(name) and actual[name] != item[name]:
            raise ValueError(f'{item["file"]}: {name} mismatch')
    if item.get('size') is not None and len(data) != item['size']:
        raise ValueError('File size mismatch: ' + item['file'])
    return actual


def safe_filename(name):
    if not name or Path(name).name != name or any(c in name for c in '/\\:'):
        raise ValueError('Unsafe file name: ' + name)
    if name.endswith(('-sources.jar', '-dev.jar', '-javadoc.jar')):
        raise ValueError('Non-runtime artifact: ' + name)
    return name


def obtain(item):
    path = CACHE / safe_filename(item['file'])
    if path.exists():
        data = path.read_bytes()
        verify(data, item)  # Never overwrite an unexpectedly changed cache entry.
    else:
        legacy = ASSETS / path.name
        data = legacy.read_bytes() if legacy.exists() else request(item['url'])
        verify(data, item)
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_suffix(path.suffix + '.download')
        temporary.write_bytes(data)
        temporary.replace(path)
    return path, digests(data)


def inspect_jar(path):
    with zipfile.ZipFile(path) as jar:
        if jar.testzip():
            raise ValueError('Bad JAR CRC: ' + str(path))
        if 'META-INF/mods.toml' not in jar.namelist():
            raise ValueError('Forge mods.toml missing: ' + str(path))
        metadata = tomllib.loads(jar.read('META-INF/mods.toml').decode('utf-8-sig'))
        # Multi-release entries may target newer JVMs without being loaded by Java 17.
        versions = [int.from_bytes(jar.read(n)[:8][6:8], 'big') for n in jar.namelist()
                    if n.endswith('.class') and not n.startswith('META-INF/versions/')]
        maximum = max(versions, default=0)
        if maximum > 61:
            raise ValueError(f'{path.name}: requires class version {maximum}, Java 17 supports 61')
        return {'mod_ids': [m['modId'] for m in metadata.get('mods', [])],
                'forge_dependencies': metadata.get('dependencies', {}), 'class_version_max': maximum,
                'java17_bytecode': True}


def choose(versions, allow_beta=False):
    compatible = [v for v in versions if MC in v['game_versions'] and 'forge' in v['loaders']]
    for kind in (('release', 'beta') if allow_beta else ('release',)):
        candidates = sorted((v for v in compatible if v['version_type'] == kind),
                            key=lambda v: v['date_published'], reverse=True)
        if candidates:
            return candidates[0]
    raise ValueError('No allowed Forge 1.20.1 version (alpha never selected)')


def check_graph(entries):
    by_id = {e['project_id']: e for e in entries}
    if len(by_id) != len(entries):
        raise ValueError('Duplicate project')
    filenames = set()
    for entry in entries:
        if entry['file'] in filenames:
            raise ValueError('Duplicate filename: ' + entry['file'])
        filenames.add(entry['file'])
        for d in entry['dependencies']:
            target = by_id.get(d.get('project_id'))
            if d['dependency_type'] == 'required':
                if target is None:
                    raise ValueError('Missing dependency: ' + str(d))
                if d.get('version_id') and d['version_id'] != target['version_id']:
                    raise ValueError('Dependency version conflict: ' + str(d))
                if not set(entry['sides']).issubset(target['sides']):
                    raise ValueError('Dependency missing on side: ' + str(d))
                if entry['default'] and not target['default']:
                    raise ValueError('Default mod depends on optional mod')
            elif d['dependency_type'] == 'incompatible' and target:
                if not d.get('version_id') or d['version_id'] == target['version_id']:
                    raise ValueError('Incompatible dependency: ' + str(d))


def update():
    specs = read_json(ASSETS / 'modrinth-projects.json')['projects']
    pending = list(specs)
    preferences = {s['project_id']: s for s in specs}
    entries = {}
    while pending:
        spec = pending.pop(0)
        pid = spec['project_id']
        if pid in entries:
            existing = entries[pid]
            if spec.get('version_id') and spec['version_id'] != existing['version_id']:
                raise ValueError('Conflicting required version: ' + pid)
            changed = not set(spec['sides']).issubset(existing['sides']) or (spec['default'] and not existing['default'])
            existing['sides'] = sorted(set(existing['sides']) | set(spec['sides']))
            existing['default'] |= spec['default']
            if changed:
                for dep in existing['dependencies']:
                    if dep['dependency_type'] == 'required':
                        pending.append(dict(project_id=dep['project_id'], version_id=dep.get('version_id'),
                                            sides=existing['sides'], default=existing['default']))
            continue
        project = api('project/' + pid)
        if spec.get('version_id'):
            version = api('version/' + spec['version_id'])
        else:
            query = urllib.parse.urlencode({'game_versions': json.dumps([MC]), 'loaders': json.dumps(['forge'])})
            version = choose(api('project/' + pid + '/version?' + query), spec.get('allow_beta', False))
        if version['project_id'] != project['id'] or MC not in version['game_versions']:
            raise ValueError('Wrong project / Minecraft version')
        kind = project['project_type']
        if kind == 'mod' and 'forge' not in version['loaders']:
            raise ValueError('Not a Forge artifact')
        if version['version_type'] == 'alpha' or (version['version_type'] == 'beta' and not spec.get('allow_beta')):
            raise ValueError('Unapproved pre-release: ' + version['id'])
        files = [f for f in version['files'] if f.get('primary')]
        if len(files) != 1:
            raise ValueError('Ambiguous primary file: ' + pid)
        f = files[0]
        deps = version['dependencies']
        for d in deps:
            if not d.get('project_id') and d.get('version_id'):
                d['project_id'] = api('version/' + d['version_id'])['project_id']
        entry = dict(project=project['slug'], project_id=project['id'], version=version['version_number'],
                     version_id=version['id'], version_type=version['version_type'], published=version['date_published'],
                     file=safe_filename(f['filename']), url=f['url'], size=f['size'], sha1=f['hashes']['sha1'],
                     sha512=f['hashes']['sha512'], sides=spec['sides'], default=spec['default'], kind=kind,
                     side='+'.join(spec['sides']), license=project['license'], dependencies=deps,
                     source='modrinth', source_url=project.get('source_url'),
                     homepage='https://modrinth.com/' + kind + '/' + project['slug'],
                     metadata_sides={'client':project.get('client_side'),'server':project.get('server_side')},
                     redistribution='official_download_only', permission_review=spec.get('permission_review','Official source download; no third-party JAR rehosting.'),
                     hash_sources={'sha1':'modrinth-api','sha512':'modrinth-api','sha256':'computed-after-official-hash-verification'},
                     note=spec.get('note',''))
        entry['authors'] = [x['user']['username'] for x in api('team/' + project['team'] + '/members')]
        entries[pid] = entry
        print('Resolved ' + entry['project'] + ' ' + entry['version'], flush=True)
        for d in deps:
            if d['dependency_type'] == 'required':
                if not d.get('project_id'):
                    raise ValueError('Unresolved file dependency: ' + str(d))
                preferred = preferences.get(d['project_id'], {})
                requested = d.get('version_id') or preferred.get('version_id')
                if d.get('version_id') and preferred.get('version_id') and requested != preferred['version_id']:
                    raise ValueError('Pinned baseline conflicts with dependency')
                pending.append(dict(preferred, project_id=d['project_id'], version_id=requested,
                                    sides=sorted(set(spec['sides']) | set(preferred.get('sides',[]))),
                                    default=spec['default'] or preferred.get('default',False)))
    rows = sorted(entries.values(), key=lambda e:e['project'])
    check_graph(rows)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(obtain, rows))
    for row, (path, hashes) in zip(rows, results):
        row.update(hashes)
        if row['kind'] == 'mod':
            row.update(inspect_jar(path))
    lock = dict(schema=1, minecraft=MC, forge=FORGE, java=17,
                updated_at=time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), entries=rows)
    # Publish only after the entire graph, downloads and hashes have passed.
    write_json(LOCK, lock)
    write_json(ASSETS / 'manifest.json', rows)
    write_json(ROOT / 'docs/qa/v22-mod-dependency-graph.json', [dict(mod=e['project'], project_id=e['project_id'],
               version_id=e['version_id'], side=e['sides'], **{t+'_dependencies':[d for d in e['dependencies']
               if d['dependency_type']==t] for t in ('required','optional','incompatible')}) for e in rows])
    return lock


def locked():
    lock = read_json(LOCK)
    if (lock['minecraft'],lock['forge'],lock['java']) != (MC,FORGE,17):
        raise ValueError('Unsupported locked environment')
    check_graph(lock['entries'])
    return lock


def selected(lock, side, optional=False):
    return [e for e in lock['entries'] if e['kind']=='mod' and side in e['sides'] and (e['default'] or optional)]
