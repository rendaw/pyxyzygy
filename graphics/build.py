#!/usr/bin/env python3
import subprocess
import os
from pathlib import Path


def c(*pargs, **kwargs):
    print(pargs, kwargs)
    subprocess.check_call(*pargs, **kwargs)


here = Path(__file__).parent
print('Operating in {}'.format(here))

resources = (
    here / '../core/target/resources/com/zarbosoft/pyxyzygy/core').resolve()
os.makedirs(resources, exist_ok=True)
dest_icons = resources / 'icons'
source_icons = here / 'icons'

os.makedirs(dest_icons, exist_ok=True)

for icon in source_icons.glob('*.svg'):
    if icon.name.startswith('appicon'):
        sizes = [16, 32, 64]
    else:
        sizes = [16]
    for size in sizes:
        size = str(size)
        name = icon.stem
        if len(sizes) > 1:
            name = name + size
        name = name + '.png'
        c([
            'inkscape',
            icon,
            '-z',
            '-e', dest_icons / name,
            '-w', size, '-h', size,
        ])