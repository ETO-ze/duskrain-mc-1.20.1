"""Shared visual payload for both client deliveries; shader files stay on official CDN."""
import json
from mod_stack import ROOT, selected


def client_entries(lock):
    rows = selected(lock, 'client')
    shader = next(e for e in lock['entries'] if e['project'] == 'complementary-reimagined')
    return rows + [shader]


def install_path(entry):
    return ('shaderpacks/' if entry['kind'] == 'shader' else 'mods/') + entry['file']


def visual_defaults():
    definition = json.loads((ROOT/'mod/src/main/resources/assets/duskrain/visual-profiles.json').read_text('utf8'))
    preset = next(p for p in definition['presets'] if p['id'] == 'rtx2060')
    pack = definition['pack']
    return {
        'config/oculus.properties': 'enableShaders=true\nshaderPack='+pack+'\n',
        'config/duskrain-visual.json': json.dumps({'profile': preset['id'], 'pack': pack})+'\n',
        'shaderpacks/'+pack+'.txt': '\n'.join(k+'='+v for k,v in preset['shader'].items())+'\n',
        'options.txt': 'lang:zh_cn\nguiScale:2\ntutorialStep:none\nrenderDistance:10\nsimulationDistance:6\nmaxFps:120\nrenderClouds:false\n',
    }
