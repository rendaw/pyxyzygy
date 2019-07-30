#!/usr/bin/env python3
import subprocess
from pathlib import Path
import sys

from version import version


here = Path(__file__).parent


def cc(args):
    print(args)
    subprocess.check_call(args)


def co(args):
    return subprocess.check_output(args).decode('utf-8').strip()


def write_version(major, minor, patch):
    with open(here / 'version.py', 'w') as dest:
        dest.write(f'version = \'{major}.{minor}.{patch}\'\n')


branch = co(['git', 'symbolic-ref', 'HEAD']).split('/')[-1]
major, minor, patch = map(int, version.split('.'))
if branch == 'experimental':
    write_version(major, minor, patch + 1)
elif branch == 'stable':
    write_version(major, minor + 1, 0)
else:
    print('Feature branch, no version change')

try:
    cc(['git', 'commit'] + sys.argv[1:])
except:  # noqa!
    write_version(major, minor, patch)
    raise