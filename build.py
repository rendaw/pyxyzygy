#!/usr/bin/env python3
def main():
    import subprocess
    import os
    from pathlib import Path
    import shutil

    # Tools
    def c(*pargs, **kwargs):
        print(pargs, kwargs)
        subprocess.check_call(*pargs, **kwargs)

    def env(**extra):
        e = os.environ.copy()
        e.update(extra)
        return e

    java_path = '/usr/lib/jvm/java-11-openjdk'

    def mvn(*extra):
        c([
            'mvn', *extra,
            '-Dstyle.color=never',
            '-X', '-e',
            '-global-toolchains', 'app/toolchains.xml',
        ], env=env(JAVA_HOME=java_path))

    def template(source, dest, extra):
        with open(source) as source_:
            text = source_.read()
        text = text.format(**extra)
        with open(dest, 'w') as dest_:
            dest_.write(text)

    # Debug
    print('CWD', Path.cwd())
    print('LS', list(Path.cwd().iterdir()))

    # Set up directories
    root = (Path(__name__).parent).resolve()
    module_path = root / '_build_modules'
    linux_path = root / '_build_linux'
    windows_path = root / '_build_windows'
    for p in [module_path, linux_path, windows_path]:
        shutil.rmtree(p, ignore_errors=True)
        p.mkdir(parents=True, exist_ok=True)

    # Build
    template('app/toolchains.xml', 'app/toolchains.xml', dict(
        java_home='/jdk-11.0.2',
    ))

    mvn('clean')
    c(
        ['python3', 'native/build.py'],
        env=env(
            BUILD_LINUX_JAVA=java_path,
            BUILD_WINDOWS_JAVA='/jdk-11.0.2',
            BUILD_WINDOWS_CXX='x86_64-w64-mingw32-g++',
            BUILD_WINDOWS_INCLUDE='/usr/x86_64-w64-mingw32/sys-root/mingw/include/c++',  # noqa
            BUILD_WINDOWS_LIB='/usr/x86_64-w64-mingw32/sys-root/mingw/lib',
        )
    )
    c(['python3', 'graphics/build.py'])
    mvn(
        'package',
        f'-DmoduleOutput={module_path}',
        f'-DlinuxOutput={linux_path / "java"}',
        f'-DwindowsOutput={windows_path / "java"}',
    )

    # Prepare itch deploys + deploy
    def platform(name, path, ext):
        shutil.copytree(module_path, path / 'modules')

        template('itch/manifest.toml', path / '.itch.toml', dict(
            ext=ext,
        ))
        for b, ds, fs in os.walk(linux_path):
            for f in fs:
                print(b, f)
        c([
            '/butler/butler', 'validate',
            '--platform', name,
            '--arch', 'amd64',
            path,
        ])
        print(f'Pushing {name}')
        for base, ds, fs in os.walk(path):
            for f in fs:
                print(Path(base) / f)
        c([
            '/butler/butler', 'push',
            path,
            f'rendaw/pyxyzygy:{name}',
        ])

    platform('linux', linux_path, '')
    platform('windows', windows_path, '.exe')


main()
