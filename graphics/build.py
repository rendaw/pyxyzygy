#!/usr/bin/env python3
import subprocess
import os
from pathlib import Path

resources = Path('../app/target/resources/com/zarbosoft/shoedemo/icons/')
os.makedirs(resources, exist_ok=True)

source_icons = Path.cwd() / 'icons'

for icon in source_icons.glob('*.svg'):
    subprocess.check_call([
        'inkscape',
        icon,
        '-z',
        '-e', resources / (icon.stem + '.png'),
        '-w', '16', '-h', '16',
    ])