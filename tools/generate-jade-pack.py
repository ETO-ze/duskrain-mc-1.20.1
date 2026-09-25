"""Package only original DuskRain assets; vanilla stays vanilla in every world."""
from pathlib import Path
import zipfile,json,hashlib
from generate_architecture import main
R=Path(__file__).resolve().parents[1]
main()
source=R/'mod/src/main/resources/assets/duskrain'
pack=R/'assets/DuskRain-Jade-City.zip'
with zipfile.ZipFile(pack,'w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':'DuskRain · 自定义青瓦白玉 · 不替换原版材质'}},ensure_ascii=False))
 z.writestr('CREDITS.txt','Original DuskRain architectural artwork. CC0-1.0. Palace lantern reference: https://www.dpm.org.cn/light/368370.html (reference only; no museum images included).\n')
 for p in sorted((source/'textures/block').glob('*.png')):z.write(p,'assets/duskrain/textures/block/'+p.name)
assert not any(n.startswith('assets/minecraft/') for n in zipfile.ZipFile(pack).namelist())
print('Isolated architecture pack',pack.stat().st_size,hashlib.sha256(pack.read_bytes()).hexdigest())
