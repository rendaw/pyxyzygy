#!/bin/env python3
def main():
    from subprocess import check_call
    from pathlib import Path
    import os
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('java_home')
    parser.add_argument('java_platform')
    parser.add_argument('cc')
    parser.add_argument('suffix')
    parser.add_argument('include')
    parser.add_argument('lib')
    args = parser.parse_args()

    module = 'mynative'

    package = ['com', 'zarbosoft', 'shoedemo']

    os.chdir(Path(__file__).parent)
    java_dest = Path() / '..' / 'app' / 'target' / 'src' / '/'.join(package)
    os.makedirs(java_dest, exist_ok=True)
    java_resource_dest = (
        Path() / '..' / 'app' / 'target' / 'resources' / '/'.join(package)
    )
    os.makedirs(java_resource_dest, exist_ok=True)

    def c(*pargs, **kwargs):
        print(pargs, kwargs)
        check_call(*pargs, **kwargs)

    c([
        'swig',
        '-java',
        '-c++',
        '-package', '.'.join(package),
        '-outdir', java_dest,
        'header.hpp',
    ])

    output = f'{module}.{args.suffix}'

    with open(java_dest / f'{module}JNI.java') as source:
        sub_java = source.read()
    sub_java = sub_java.replace('##OUTPUT##', output)
    with open(java_dest / f'{module}JNI.java', 'w') as dest:
        dest.write(sub_java)

    general_flags = [
        '-Wall', '-pedantic',
        '-O3',
    ]

    c([
        args.cc,
    ] + general_flags + [
        '-shared',
        '-fPIC',
        f'-L{args.lib}',
        f'-I{args.include}',
        f'-I{args.java_home}/include',
        f'-I{args.java_home}/include/{args.java_platform}',
    ] + (['-static'] if args.java_platform == 'win32' else []) + [
        '-static-libgcc',
        '-static-libstdc++',
        '-o', java_resource_dest / output,
        'header_wrap.cxx',
        'implementation.cxx',
        '-lpng',
        '-lz',
    ])


main()