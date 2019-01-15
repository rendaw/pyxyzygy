#!/bin/env python
def main():
    from subprocess import check_call
    from pathlib import Path
    import os

    module = 'mynative'

    package = ['com', 'zarbosoft', 'shoedemo']

    os.chdir(Path(__file__).parent)
    java = Path('/usr/lib/jvm/java-8-openjdk')
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

    def compile_obj(name):
        c([
            'clang++',
            '-c',
            '-Wall', '-pedantic',
            '-o', '{}.o'.format(name),
            name,
            '-I/c/jdk1.3.1/include',
            '-I/c/jdk1.3.1/include/win32',
            '-I/usr/include',
            '-I' + str(java / 'include'),
            '-I' + str(java / 'include/linux'),
        ])

    compile_obj('header_wrap.cxx')
    compile_obj('implementation.cpp')
    c([
        'clang++',
        '-shared',
        '-L/usr/lib',
        '-o', java_resource_dest / '{}.so'.format(module),
        'header_wrap.cxx.o',
        'implementation.cpp.o',
        '-lpng',
    ])


main()