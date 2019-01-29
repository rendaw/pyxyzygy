#!/usr/bin/env python3
import subprocess
import os
from pathlib import Path

resources = Path('../app/target/resources/com/zarbosoft/shoedemo/icons')
os.makedirs(resources, exist_ok=True)

source_icons = Path.cwd() / 'icons'

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
            '-e', resources / name,
            '-w', size, '-h', size,
        ])