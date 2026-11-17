"""Package the selected APK and the exact public configuration compiled into it."""
from pathlib import Path
import json
import os
import re
import shutil
import zipfile

variant = os.environ["BUILD_VARIANT"]
if variant not in {"debug", "release"}:
    raise ValueError("Invalid build variant")
output_dir = Path("app/build/outputs/apk") / variant
metadata = json.loads((output_dir / "output-metadata.json").read_text(encoding="utf-8"))
[element] = metadata["elements"]
config_files = list((Path("app/build/generated/source/buildConfig") / variant).rglob("BuildConfig.java"))
[config_file] = config_files
compiled = dict((name, json.loads(value)) for name, value in re.findall(
    r'public static final String (\w+) = ("(?:[^"\\]|\\.)*");', config_file.read_text(encoding="utf-8")))
out = Path("ci-out")
out.mkdir(exist_ok=True)
shutil.copyfile(output_dir / element["outputFile"], out / "input.apk")
with zipfile.ZipFile(out / "THIRD-PARTY-NOTICES.zip", "w", zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(Path("third_party").rglob("*")):
        if path.is_file():
            archive.write(path, path.as_posix())
    archive.write("app/build/reports/licenses/dependencies.txt", "resolved-dependencies.txt")
(out / "BUILD-INFO.json").write_text(json.dumps({
    "app": os.environ["APP"], "build_variant": variant,
    "application_id": metadata["applicationId"],
    "commit": os.environ["GITHUB_SHA"],
    "run_url": f"{os.environ['GITHUB_SERVER_URL']}/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
    "api_base_url": compiled["API_BASE_URL"],
    "privacy_policy_url": compiled.get("PRIVACY_POLICY_URL"),
    "privacy_consent_url": compiled.get("PRIVACY_CONSENT_URL"),
    "privacy_regulation_url": compiled.get("PRIVACY_REGULATION_URL"),
    "consent_version": compiled.get("CONSENT_VERSION"),
    "version_code": element["versionCode"], "version_name": element["versionName"],
    "synthetic_data_only": variant == "debug",
}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
