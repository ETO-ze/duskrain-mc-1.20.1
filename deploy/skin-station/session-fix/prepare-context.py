"""Prepare the pinned upstream source and minimal session-validation patch."""
import argparse, pathlib, shutil, subprocess
p=argparse.ArgumentParser();p.add_argument('output',type=pathlib.Path);a=p.parse_args()
root=a.output.resolve()
if root.exists() and any(root.iterdir()):raise SystemExit('Output must be empty')
root.mkdir(parents=True,exist_ok=True)
source=root/'source'
subprocess.run(['git','clone','--no-checkout','https://github.com/unmojang/drasl.git',str(source)],check=True)
subprocess.run(['git','-C',str(source),'checkout','--detach','823d8a21780d6b15f44aec3de68411a5c40e3fe2'],check=True)
here=pathlib.Path(__file__).resolve().parent
subprocess.run(['git','-C',str(source),'apply','--check',str(here/'validate-stale.patch')],check=True)
subprocess.run(['git','-C',str(source),'apply',str(here/'validate-stale.patch')],check=True)
shutil.copy2(here/'duskrain_session_test.go',source/'duskrain_session_test.go')
shutil.copy2(here/'Dockerfile',root/'Dockerfile')
print('Build context ready:',root)
