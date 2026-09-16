from pathlib import Path
import sys
root=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(root/'.deps/qa-python'))
import nbtlib,shutil,json
old=root/'mod/run-v13-qa/saves/DuskRainRemake'
new=root/'mod/run-v14-qa/saves/DuskRainRemake'
assert old!=new and new.parent.parent.name=='run-v14-qa'
# Only the isolated, previously empty guild terrain is reset for the authored route corrections.
shutil.copytree(old/'dimensions/duskrain/guild',new/'dimensions/duskrain/guild',dirs_exist_ok=True)
n=nbtlib.load(new/'data/duskrain_guilds.dat')
for g in n['data']['guilds']:
 if int(g['id'])==1:
  assert str(g['name'])=='合欢宗'
  g['palace']=nbtlib.Int(1)
n.save()
observer=root/'mod/run-v14-observer';observer.mkdir(exist_ok=True)
for folder in ('config','resourcepacks','shaderpacks'):
 shutil.copytree(root/'mod/run-v14-qa'/folder,observer/folder,dirs_exist_ok=True)
shutil.copy2(root/'mod/run-v14-qa/options.txt',observer/'options.txt')
print('Isolated guild construction reset; observer client prepared. Original world untouched.')
