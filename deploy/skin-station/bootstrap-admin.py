"""Run only on the skin host; writes credentials into its private directory."""
import json, os, re, secrets, subprocess, urllib.request
from pathlib import Path
root=Path('/srv/duskrain-skin')
secret=root/'private/admin-account.json'
if secret.exists():
    raise SystemExit('Administrator already initialized; credentials preserved.')
logs=subprocess.check_output(['docker','logs','duskrain-skin'],stderr=subprocess.STDOUT).decode()
invite=re.search(r'invite=([A-Za-z0-9_-]+)',logs)
if not invite:raise SystemExit('Initial invitation was not found. No account was changed.')
password=secrets.token_urlsafe(24)
data={'username':'DuskRainDirector','password':password,'inviteCode':invite[1],'requestApiToken':True}
req=urllib.request.Request('http://127.0.0.1:25585/drasl/api/v3/users',data=json.dumps(data).encode(),headers={'Content-Type':'application/json'})
with urllib.request.urlopen(req,timeout=20) as response:result=json.load(response)
user=result['user']
assert user['isAdmin'], 'Expected administrator role'
assert user['players'][0]['uuid']=='6e93fc7d-8abf-3d21-a965-18b49e686b0b','Existing character UUID mismatch'
payload={'username':'DuskRainDirector','password':password,'player_uuid':user['players'][0]['uuid'],'user_uuid':user['uuid'],'api_token':result['apiToken']}
fd=os.open(secret,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
with os.fdopen(fd,'w') as f:json.dump(payload,f,indent=2)
print(json.dumps({'administrator_initialized':True,'existing_player_uuid_preserved':True,'credential_file':str(secret)}))
