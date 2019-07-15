#!/usr/bin/env python3
def main():
    import subprocess
    import os
    from pathlib import Path
    import shutil
    import argparse

    import toml

    root = (Path(__file__).parent).resolve()

    parser = argparse.ArgumentParser()
    parser.add_argument('platform')
    parser.add_argument('channel')
    args = parser.parse_args()

    # More variables
    if False:
        pass
    elif args.platform == 'linux':
        java_path = '/usr/lib/jvm/java-11-openjdk'
        java_toolchain = java_path
        java_platform = 'linux'
        jfx_platform = 'linux'
        itch_platform = 'linux'
        exe_ext = ''
        so_ext = 'so'
        cxx = 'g++'
        c_include = '/usr/include'
        c_lib = '/usr/lib'
        butler_root = Path('/')
    elif args.platform == 'windows':
        java_path = '/usr/lib/jvm/java-11-openjdk'
        java_toolchain = '/jdk-11.0.2'
        java_platform = 'win32'
        jfx_platform = 'win'
        itch_platform = 'windows'
        exe_ext = '.exe'
        so_ext = 'dll'
        cxx = 'x86_64-w64-mingw32-g++'
        c_include = '/usr/x86_64-w64-mingw32/sys-root/mingw/include/c++'
        c_lib = '/usr/x86_64-w64-mingw32/sys-root/mingw/lib'
        butler_root = Path('/')
    elif args.platform == 'mac':
        java_path = '/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home'  # noqa
        java_toolchain = java_path
        java_platform = 'darwin'
        jfx_platform = 'mac'
        itch_platform = 'osx'
        exe_ext = ''
        so_ext = 'dylib'
        cxx = 'g++-8'
        c_include = '/usr/local/include'
        c_lib = '/usr/local/lib'
        butler_root = Path.cwd()
    else:
        raise AssertionError

    # Tools
    def c(*pargs, **kwargs):
        print(pargs, kwargs)
        env_source = kwargs.get('env')
        if env_source:
            env = os.environ.copy()
            env.update(env_source)
            kwargs['env'] = env
        subprocess.check_call(*pargs, **kwargs)

    def mvn(*extra):
        c([
            'mvn', *extra,
            '-B',
            '-Dstyle.color=never',
            f'-Dmaven.repo.local={root / ".m2" / "repository"}',
            '-e',
            '-global-toolchains', 'build/toolchains.xml',
        ], env=dict(JAVA_HOME=java_path))

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
    path = root / '_build'
    shutil.rmtree(path, ignore_errors=True)
    path.mkdir(parents=True, exist_ok=True)

    if args.platform != 'mac':
        os.makedirs('/root/.m2', exist_ok=True)

    # Build
    template('build/toolchains.xml', 'build/toolchains.xml', dict(
        java_home=java_toolchain,
    ))

    mvn('clean')
    c([
        'python3',
        'native/build.py',
        java_toolchain,
        java_platform,
        cxx,
        so_ext,
        c_include,
        c_lib
    ])
    c(['python3', 'graphics/build.py'])
    mvn(
        'package',
        f'-DimageOutput={path / "java"}',
        f'-Djavafx.platform={jfx_platform}',
    )
    shutil.copy(
        (
            root / 'nearestneighborimageviewagent' / 'target' /
            'nearestneighborimageviewagent-1.0.0.jar'
        ),
        path / 'java',
    )

    # Prepare itch deploys + deploy
    if args.platform == 'linux':
        c([
            'strip',
            '-p', '--strip-unneeded',
            path / 'java/lib/server/libjvm.so'
        ])
    run_args = [
        '-p',
        'modules',
        '--add-opens',
        'javafx.graphics/com.sun.prism=com.zarbosoft.pyxyzygy.nearestneighborimageview',  # noqa
        '--add-exports=javafx.graphics/com.sun.javafx.sg.prism=com.zarbosoft.pyxyzygy.nearestneighborimageview',  # noqa
        '--add-opens', 'javafx.controls/javafx.scene.control=com.zarbosoft.pyxyzygy.app',  # noqa
        '--add-exports=javafx.graphics/com.sun.glass.ui=com.zarbosoft.pyxyzygy.app',  # noqa
        '--add-opens', 'gifencoder/com.squareup.gifencoder=com.zarbosoft.pyxyzygy.app',  # noqa
        f'-javaagent:..{os.sep}nearestneighborimageviewagent-1.0.0.jar',
        '-m', 'com.zarbosoft.pyxyzygy.app/com.zarbosoft.pyxyzygy.app.GUIMain'
    ]
    with open(path / '.itch.toml', 'w') as out:
        toml.dump({'actions': [{
            'name': 'run',
            'path': f'java/bin/java{exe_ext}',
            'args': run_args
        }]}, out)
    linux_run = path / 'run.sh'
    with open(linux_run, 'w') as out:
        out.write('#!/usr/bin/bash\n')
        out.write('cd java/bin\n')
        out.write(f'./java{exe_ext}')
        for arg in run_args:
            out.write(f' \\\n\t{arg}')
        out.write('\n')
    linux_run.chmod(0o755)
    with open(path / 'run.bat', 'w', newline='\r\n') as out:
        out.write('cd java\\bin\n')
        out.write(f'java{exe_ext}')
        for arg in run_args:
            out.write(f' ^\n\t\t{arg}')
        out.write('\n')

    for b, ds, fs in os.walk(path):
        for f in fs:
            print(b, f)
    c([
        butler_root / 'butler/butler', 'validate',
        '--platform', itch_platform,
        '--arch', 'amd64',
        path,
    ])
    print(f'Pushing {args.platform}')
    for base, ds, fs in os.walk(path):
        for f in fs:
            print(Path(base) / f)
    c([
        butler_root / 'butler/butler', 'push',
        path,
        f'rendaw/pyxyzygy:{itch_platform}-{args.channel}',
    ])


main()
