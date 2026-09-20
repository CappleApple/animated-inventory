"""Run the packaged mod through an official disposable Forge dedicated server."""
from pathlib import Path
import argparse, os, re, shutil, subprocess
ROOT=Path(__file__).resolve().parents[1]
GAME=ROOT/'run-production-server'
parser=argparse.ArgumentParser(); parser.add_argument('--world',default='validation-world'); options=parser.parse_args()
if not re.fullmatch(r'[A-Za-z0-9_-]+',options.world): raise SystemExit('Use a simple disposable world directory name.')
JAVA=Path(os.environ.get('JAVA_HOME',str(Path.home()/'.gradle/jdks/eclipse_adoptium-17-amd64-windows.2')))/'bin/java.exe'
arguments=GAME/'libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt'
if not arguments.exists(): raise SystemExit('Install the official Forge47.4.10 server into run-production-server first.')
(GAME/'mods').mkdir(exist_ok=True)
for name in ['animatedinventory-forge-1.20.1-1.1.1.jar','animatedinventory-forge-1.20.1-1.1.1-validation.jar']:
    shutil.copy2(ROOT/'build/libs'/name,GAME/'mods'/name)
(GAME/'eula.txt').write_text('eula=true\n')
(GAME/'server.properties').write_text('level-name='+options.world+'\nlevel-type=minecraft:flat\ngenerate-structures=false\nonline-mode=false\nserver-ip=127.0.0.1\nserver-port=25912\nview-distance=2\nsimulation-distance=2\nmax-tick-time=120000\n')
report=GAME/'server-validation.txt'; report.unlink(missing_ok=True)
with (GAME/'launcher-output.log').open('w',encoding='utf-8') as output:
    completed=subprocess.run([str(JAVA),'-Xmx2G','-XX:ActiveProcessorCount=2','-Danimatedinventory.serverValidation=true','@'+str(arguments),'nogui'],cwd=GAME,creationflags=0x08000000,stdout=output,stderr=subprocess.STDOUT)
print('Production server exit code:',completed.returncode)
if completed.returncode or not report.exists() or not report.read_text().startswith('PASS '):
    raise SystemExit('Packaged server validation failed; inspect server-validation.txt and launcher-output.log.')
print(report.read_text().strip())
