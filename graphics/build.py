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
    here / '../app/target/resources/com/zarbosoft/pyxyzygy/app').resolve()

for category in [
    dict(
        type='icons',
        exceptions=[
            dict(
                prefix='appicon',
                sizes=[16, 32, 64],
            ),
            dict(
                prefix='arrow-collapse',
                sizes=[16, 32],
            ),
            dict(
                prefix='cursor-move',
                sizes=[16, 32],
            ),
            dict(
                prefix='call-merge',
                sizes=[16, 32],
            ),
            dict(
                prefix='stamper',
                sizes=[16, 32],
            ),
        ],
        size=16,
    ),
]:
    dest = resources / category['type']
    source = here / category['type']
    os.makedirs(dest, exist_ok=True)

    def process(icon, name, size):
        size = str(size)
        c([
            'inkscape',
            icon,
            '-z',
            '-e', dest / (name + '.png'),
            '-w', size, '-h', size,
        ])

    for icon in source.glob('*.svg'):
        done = False
        name = icon.stem
        for exception in category.get('exceptions', []):
            if not name.startswith(exception['prefix']):
                continue
            for size in exception['sizes']:
                process(icon, name + str(size), size)
            done = True
        if done:
            continue
        process(icon, name, category['size'])