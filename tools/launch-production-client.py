"""Launch the packaged Forge build in the disposable official client installation."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, urllib.request, zipfile

ROOT=Path(__file__).resolve().parents[1]
GAME=ROOT/'run-production-client'
LIB=GAME/'libraries'
CACHE=Path.home()/'.gradle/caches'
JAVA=Path(os.environ.get('JAVA_HOME',str(Path.home()/'.gradle/jdks/eclipse_adoptium-17-amd64-windows.2')))/'bin/java.exe'

def get_json(url):
    with urllib.request.urlopen(url) as response: return json.load(response)

def ensure(download, target):
    target.parent.mkdir(parents=True,exist_ok=True)
    expected=download.get('sha1')
    if target.exists() and (not expected or hashlib.sha1(target.read_bytes()).hexdigest()==expected): return target
    path=download.get('path','')
    candidates=[]
    if path:
        parts=path.split('/')
        if len(parts)>=4:
            group='.'.join(parts[:-3]); artifact,version,filename=parts[-3:]
            candidates.extend((CACHE/'modules-2/files-2.1'/group/artifact/version).glob('*/'+filename))
        candidates.append(CACHE/'forge_gradle/maven_downloader'/path)
    for cached in candidates:
        if cached.exists() and (not expected or hashlib.sha1(cached.read_bytes()).hexdigest()==expected):
            shutil.copy2(cached,target); return target
    with urllib.request.urlopen(download['url']) as response: target.write_bytes(response.read())
    if expected and hashlib.sha1(target.read_bytes()).hexdigest()!=expected: raise ValueError('Checksum mismatch: '+str(target))
    return target

def allowed(item):
    if not item.get('rules'): return True
    result=False
    for rule in item['rules']:
        platform=rule.get('os',{})
        matches=(not platform.get('name') or platform['name']=='windows') and (not platform.get('arch') or platform['arch']=='x86_64')
        if rule.get('features'): matches=False
        if matches: result=rule['action']=='allow'
    return result

def args_for(values):
    result=[]
    for value in values:
        if isinstance(value,str): result.append(value)
        elif allowed(value):
            element=value['value']; result.extend(element if isinstance(element,list) else [element])
    return result

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--prepare-only',action='store_true'); cli=parser.parse_args()
    forge_path=GAME/'versions/1.20.1-forge-47.4.10/1.20.1-forge-47.4.10.json'
    if not forge_path.exists(): raise SystemExit('Install the official Forge47.4.10 client into run-production-client first.')
    forge=json.loads(forge_path.read_text())
    base_path=GAME/'versions/1.20.1/1.20.1.json'
    if not base_path.exists():
        manifest=get_json('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
        base=get_json(next(v['url'] for v in manifest['versions'] if v['id']=='1.20.1'))
        base_path.parent.mkdir(parents=True,exist_ok=True); base_path.write_text(json.dumps(base))
    base=json.loads(base_path.read_text())
    artifacts={}
    for entry in base['libraries']+forge['libraries']:
        if not allowed(entry): continue
        artifact=entry.get('downloads',{}).get('artifact')
        if artifact:
            name=entry['name'].split(':'); key=tuple(name[:2]+name[3:])
            artifacts[key]=ensure(artifact,LIB/artifact['path'])
    client=ensure(base['downloads']['client'],GAME/'versions/1.20.1/1.20.1.jar')
    natives=GAME/'natives'; natives.mkdir(exist_ok=True)
    for path in artifacts.values():
        if path.name.endswith('-natives-windows.jar'):
            with zipfile.ZipFile(path) as archive:
                for name in archive.namelist():
                    if name.lower().endswith('.dll'): (natives/Path(name).name).write_bytes(archive.read(name))
    assets=CACHE/'forge_gradle/assets'
    if not (assets/'indexes'/f"{base['assetIndex']['id']}.json").exists(): raise SystemExit('Run Gradle downloadAssets first.')
    # Forge discovers its patched client and client-extra jars through library_directory.
    classpath=os.pathsep.join(map(str,artifacts.values()))
    values={'natives_directory':str(natives),'launcher_name':'AnimatedInventoryValidation','launcher_version':'1',
        'classpath':classpath,'classpath_separator':os.pathsep,'library_directory':str(LIB),'version_name':forge['id'],
        'auth_player_name':'Validation','game_directory':str(GAME),'assets_root':str(assets),'assets_index_name':base['assetIndex']['id'],
        'auth_uuid':'7117898f154434879c67c386fe34c30d','auth_access_token':'0','clientid':'0','auth_xuid':'0','user_type':'legacy','version_type':'release'}
    def subst(value):
        for key,replacement in values.items(): value=value.replace('${'+key+'}',replacement)
        if '${' in value: raise ValueError('Unresolved launcher substitution: '+value)
        return value
    arguments=[*args_for(base['arguments']['jvm']),*args_for(forge['arguments']['jvm']),'-Xmx2G','-XX:ActiveProcessorCount=2',
        '-Danimatedinventory.clientValidation=true','-Danimatedinventory.productionValidation=true',forge['mainClass'],
        *args_for(base['arguments']['game']),*args_for(forge['arguments']['game']),'--width','960','--height','600']
    arguments=[subst(a) for a in arguments]
    argfile=GAME/'launch.args'; argfile.write_text('\n'.join('"'+a.replace('\\','\\\\').replace('"','\\"')+'"' for a in arguments),encoding='utf-8')
    print('Prepared official Forge47.4.10 production client with',len(artifacts),'libraries',flush=True)
    if cli.prepare_only: return
    mods=GAME/'mods'; mods.mkdir(exist_ok=True)
    for filename in ['animatedinventory-forge-1.20.1-1.1.1.jar','animatedinventory-forge-1.20.1-1.1.1-validation.jar']:
        shutil.copy2(ROOT/'build/libs'/filename,mods/filename)
    (GAME/'config').mkdir(exist_ok=True)
    (GAME/'config/fml.toml').write_text('earlyWindowControl=false\n')
    (GAME/'options.txt').write_text('soundCategory_master:0.0\npauseOnLostFocus:false\nfullscreen:false\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\nlang:en_us\nonboardAccessibility:true\n')
    report=GAME/'client-validation.txt'; report.unlink(missing_ok=True)
    with (GAME/'launcher-output.log').open('w',encoding='utf-8') as output:
        completed=subprocess.run([str(JAVA),'@'+str(argfile)],cwd=GAME,creationflags=0x08000000,stdout=output,stderr=subprocess.STDOUT)
    print('Production client exit code:',completed.returncode,flush=True)
    if completed.returncode or not report.exists() or 'PASS validation completed' not in report.read_text():
        raise SystemExit('Packaged client validation failed; inspect client-validation.txt and logs.')

if __name__=='__main__': main()
