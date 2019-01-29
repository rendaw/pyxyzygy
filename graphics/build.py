#!/usr/bin/env python3
import subprocess
import os
from pathlib import Path

resources = Path('../app/target/resources/com/zarbosoft/shoedemo')
os.makedirs(resources, exist_ok=True)

dest_icons = resources / 'icons'
source_icons = Path.cwd() / 'icons'

os.makedirs(dest_icons, exist_ok=True)

for icon in source_icons.glob('*.svg'):
    if icon.name.startswith('appicon'):
        sizes = [16, 32, 64]
    else:
        sizes = [16]
    for size in sizes:
        size = str(size)
        name = icon.stem
        if sizes:
            name = name + size
        name = name + '.png'
        subprocess.check_call([
            'inkscape',
            icon,
            '-z',
            '-e', dest_icons / name,
            '-w', size, '-h', size,
        ])