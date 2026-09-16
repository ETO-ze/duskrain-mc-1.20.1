"""Create an owner-only snapshot on the skin host without stopping authentication."""
import datetime
import hashlib
import json
import os
import shutil
import sqlite3
import tarfile
from pathlib import Path

os.umask(0o077)
root = Path('/srv/duskrain-skin').resolve()
assert root.is_dir() and (root / 'data/drasl.db').is_file()
backup_root = root / 'private/backups'
backup_root.mkdir(parents=True, exist_ok=True, mode=0o700)
stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
snapshot = backup_root / stamp
snapshot.mkdir(mode=0o700)

# Take SQLite's consistent online snapshot, including committed WAL content.
data = snapshot / 'data'
data.mkdir()
with sqlite3.connect((root / 'data/drasl.db').as_uri() + '?mode=ro', uri=True) as src:
    with sqlite3.connect(data / 'drasl.db') as dst:
        src.backup(dst)
        assert dst.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
for path in (root / 'data').iterdir():
    if path.name.startswith('drasl.db'):
        continue
    target = data / path.name
    if path.is_dir():
        shutil.copytree(path, target)
    else:
        shutil.copy2(path, target)
for name in ('config', 'branding', 'assets', 'tls'):
    if (root / name).is_dir():
        shutil.copytree(root / name, snapshot / name, symlinks=True)
for name in ('compose.yaml', 'renew-certificates.sh', 'duskrain-skin-renew.service',
             'duskrain-skin-renew.timer', 'nginx-ip.conf', 'nginx-domain.conf'):
    if (root / name).is_file():
        shutil.copy2(root / name, snapshot / name)
archive = backup_root / (stamp + '.tar.gz')
with tarfile.open(archive, 'x:gz') as tar:
    tar.add(snapshot, arcname='duskrain-skin')
archive.chmod(0o600)
result = {'archive': str(archive), 'bytes': archive.stat().st_size,
          'sha256': hashlib.sha256(archive.read_bytes()).hexdigest(),
          'sqlite_integrity': 'ok', 'signed_profile_key_preserved': (data / 'key.pkcs8').exists()}
(backup_root / (stamp + '.json')).write_text(json.dumps(result, indent=2), encoding='utf8')
print(json.dumps(result))
