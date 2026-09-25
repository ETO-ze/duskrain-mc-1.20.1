"""Regression: inspect delivered archives, not only the packaging source selection."""
import hashlib
import io
import json
from pathlib import Path
import sys
import unittest
import zipfile

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from mod_stack import ROOT, locked
from client_visual_bundle import client_entries


class ClientDeliveryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pack=zipfile.ZipFile(ROOT/'dist/release-r3/DuskRain-2.2.0-PCL2-Visual-r3.mrpack')
        cls.alternative=zipfile.ZipFile(ROOT/'dist/release-r3/DuskRain-2.2.0-client-Visual-r3.zip')
        cls.index=json.loads(cls.pack.read('modrinth.index.json'))

    @classmethod
    def tearDownClass(cls):
        cls.pack.close()
        cls.alternative.close()

    def test_shader_is_required_in_shaderpacks_not_mods(self):
        rows=self.index['files']
        shaders=[e for e in rows if e['path'].startswith('shaderpacks/')]
        self.assertEqual(len(shaders),1)
        shader=next(e for e in locked()['entries'] if e['kind']=='shader')
        self.assertEqual(shaders[0]['path'],'shaderpacks/'+shader['file'])
        self.assertEqual(shaders[0]['env'],{'client':'required','server':'unsupported'})
        self.assertEqual(shaders[0]['hashes']['sha512'],shader['sha512'])
        self.assertEqual(shaders[0]['downloads'],[shader['url']])
        self.assertEqual(len(rows),16)

    def test_shader_config_matches_runtime_definition(self):
        with zipfile.ZipFile(io.BytesIO(self.pack.read('client-overrides/mods/duskrain-2.2.0-preview.jar'))) as jar:
            definition=json.loads(jar.read('assets/duskrain/visual-profiles.json'))
        prefix='client-overrides/'
        state=json.loads(self.pack.read(prefix+'config/duskrain-visual.json'))
        preset=next(p for p in definition['presets'] if p['id']==state['profile'])
        config=dict(line.split('=',1) for line in self.pack.read(prefix+'shaderpacks/'+state['pack']+'.txt').decode().splitlines())
        self.assertEqual(config,preset['shader'])
        self.assertIn('enableShaders=true',self.pack.read(prefix+'config/oculus.properties').decode())
        self.assertEqual(state['pack'],definition['pack'])

    def test_original_textures_embedded_without_vanilla_overrides(self):
        with zipfile.ZipFile(io.BytesIO(self.pack.read('client-overrides/mods/duskrain-2.2.0-preview.jar'))) as jar:
            names=jar.namelist()
            for name in ('quartz_block_side.png','quartz_block_side_n.png','quartz_block_side_s.png'):
                self.assertIn('assets/duskrain/textures/block/'+name,names)
            self.assertFalse(any(n.startswith('assets/minecraft/textures/block/') for n in names))

    def test_no_worlds_credentials_or_rehosted_dependencies(self):
        for archive in (self.pack,self.alternative):
            self.assertIsNone(archive.testzip())
            names=archive.namelist()
            for name in names:
                segments=Path(name).parts
                self.assertFalse(set(segments)&{'world','saves','.private','playerdata','runtime'})
                self.assertNotIn('accounts.json',name)
                if name.endswith('.jar'):
                    self.assertTrue(name.endswith('/duskrain-2.2.0-preview.jar'))

    def test_zip_installs_same_components_and_checksums(self):
        manifest=json.loads(self.alternative.read('client-mods.lock.json'))
        self.assertEqual(manifest['entries'],client_entries(locked()))
        self.assertFalse(any(e['kind']=='shader' for e in manifest['optional']))
        sums=json.loads(self.alternative.read('checksums.json'))
        for name,digest in sums.items():
            self.assertEqual(hashlib.sha256(self.alternative.read(name)).hexdigest(),digest,name)
        for name in ('config/oculus.properties','config/duskrain-visual.json','options.txt'):
            self.assertEqual(self.pack.read('client-overrides/'+name).splitlines(),self.alternative.read('defaults/'+name).splitlines())


if __name__=='__main__':unittest.main()
