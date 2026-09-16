"""Create the local V14 experience from the installed, preservation-checked package."""
from pathlib import Path
import shutil,json
root=Path(__file__).resolve().parents[2];dest=root/'mod/run-v14-preview';src=root/'dist/DuskRain-2.1.0-preview/server/world'
assert not dest.exists(),'Never overwrite an existing playable world.'
shutil.copytree(src,dest/'saves/DuskRainRemake',ignore=shutil.ignore_patterns('session.lock'))
for folder in ('config','resourcepacks','shaderpacks'):shutil.copytree(root/'mod/run-v14-qa'/folder,dest/folder)
shutil.copy2(root/'mod/run-v14-qa/options.txt',dest/'options.txt')
(root/'docs/qa/v14-local-migration.json').write_text(json.dumps({'world':str(dest/'saves/DuskRainRemake'),'source':str(src),'old_world_preserved':True,'source_original_progress_checked':True},indent=2),encoding='utf8')
print(str(dest))
