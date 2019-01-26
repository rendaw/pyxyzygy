#!/bin/env python
def main():
    from subprocess import check_call
    from pathlib import Path
    import os

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
        print(*pargs, **kwargs)
        check_call(*pargs, **kwargs)

    c([
        'swig',
        '-java',
        '-c++',
        '-package', '.'.join(package),
        '-outdir', java_dest,
        'header.hpp',
    ])

    general_flags = [
        '-static-libstdc++',
        '-Wall', '-pedantic',
        '-O3',
    ]

    def compile(key, cc, suffix, includes, libs):
        def compile_obj(name):
            c([
                cc,
            ] + general_flags + [
                '-c',
                '-o', '{}.{}.o'.format(name, key),
                name,
            ] + includes)

        compile_obj('header_wrap.cxx')
        compile_obj('implementation.cxx')
        c([
            cc,
        ] + general_flags + libs + [
            '-shared',
            '-L/usr/lib',
            '-o', java_resource_dest / '{}.{}'.format(module, suffix),
            'header_wrap.cxx.lx64.o',
            'implementation.cxx.lx64.o',
            '-lpng',
        ])

    compile(
        'lx64',
        'clang++',
        'so',
        [
            '-I/usr/include',
            '-I/usr/lib/jvm/java-11-openjdk/include',
            '-I/usr/lib/jvm/java-11-openjdk/include/linux',
        ],
        [
            '-L/usr/lib',
        ],
    )
    compile(
        'wx64',
        'x86_64-w64-mingw32-g++',
        'dll',
        [
            '-I/usr/i686-w64-mingw32/include',
            '-I/home/andrew/temp/ren/shoedemo2/temp/jdk-11.0.2/include',
            '-I/home/andrew/temp/ren/shoedemo2/temp/jdk-11.0.2/include/win32',
        ],
        [
            '-L/usr/lib/gcc/i686-w64-mingw32/',
        ],
    )

main()