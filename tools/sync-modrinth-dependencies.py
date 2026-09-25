"""Only --update may query new versions and replace the dependency lock."""
import argparse
from mod_stack import locked, obtain, update

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--update', action='store_true')
args = parser.parse_args()
lock = update() if args.update else locked()
if not args.update:
    for item in lock['entries']:
        obtain(item)
print(f'Verified {len(lock["entries"])} locked artifacts. Running instances were not modified.')
