#!/bin/env python
def main():
    from subprocess import check_call
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('test')
    args = parser.parse_args()

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

    compile_obj('implementation.cxx')
    compile_obj('bench_{}.cxx'.format(args.test))
    c([
        'clang++',
        '-ggdb',
        '-L/usr/lib',
        '-o', 'bench_{}'.format(args.test),
        'bench_{}.cxx.o'.format(args.test),
        'implementation.cxx.o',
        '-lpng',
    ])
    c([
        'valgrind',
        '--tool=callgrind',
        '--callgrind-out-file={}_callgrind'.format(args.test),
        './bench_{}'.format(args.test)
    ])


main()