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

    win_java = os.environ['BUILD_WIN_JAVA']
    win_include = os.environ['BUILD_WIN_INCLUDE']
    win_lib = os.environ['BUILD_WIN_LIB']
    win_cxx = os.environ['BUILD_WIN_CXX']

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
        '-Wall', '-pedantic',
        '-O3',
    ]

    def compile(key, cc, suffix, includes, libs):
        c([
            cc,
        ] + general_flags + includes + libs + [
            '-shared',
            '-fPIC',
            '-static-libgcc',
            '-static-libstdc++',
            '-o', java_resource_dest / '{}.{}'.format(module, suffix),
            'header_wrap.cxx',
            'implementation.cxx',
            '-lpng',
            '-lz',
        ])

    compile(
        'lx64',
        'g++',
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
        win_cxx,
        'dll',
        [
            f'-I{win_include}',
            f'-I{win_java}/include',
            f'-I{win_java}/include/win32',
        ],
        [
            f'-L{win_lib}',
            '-static',
        ],
    )


main()