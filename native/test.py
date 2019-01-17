#!/bin/env python
def main():
    from subprocess import check_call

    def c(*pargs, **kwargs):
        print(*pargs, **kwargs)
        check_call(*pargs, **kwargs)

    def compile_obj(name):
        c([
            'clang++',
            '-c',
            '-ggdb',
            '-Wall', '-pedantic',
            '-o', '{}.o'.format(name),
            name,
            '-I/usr/include',
        ])

    compile_obj('implementation.cpp')
    compile_obj('test_stroke.cxx')
    c([
        'clang++',
        '-ggdb',
        '-L/usr/lib',
        '-o', 'test_stroke',
        'test_stroke.cxx.o',
        'implementation.cpp.o',
        '-lpng',
    ])
    c(['./test_stroke'])


main()