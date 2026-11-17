"""Package only the built APK, public build metadata and third-party notices."""
from pathlib import Path
import json
import os
import shutil
import zipfile

out = Path("ci-out")
out.mkdir(exist_ok=True)
shutil.copyfile("app/build/outputs/apk/debug/app-debug.apk", out / "input.apk")
with zipfile.ZipFile(out / "THIRD-PARTY-NOTICES.zip", "w", zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(Path("third_party").rglob("*")):
        if path.is_file():
            archive.write(path, path.as_posix())
    inventory = Path("app/build/reports/licenses/dependencies.txt")
    archive.write(inventory, "resolved-dependencies.txt")
(out / "BUILD-INFO.json").write_text(json.dumps({
    "app": os.environ["APP"],
    "commit": os.environ["GITHUB_SHA"],
    "run_url": f"{os.environ['GITHUB_SERVER_URL']}/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
    "api_base_url": os.environ["API_BASE_URL"],
    "version_code": 1000000 + int(os.environ["GITHUB_RUN_NUMBER"]),
    "version_name": f"1.0-ci.{os.environ['GITHUB_RUN_NUMBER']}-test",
    "synthetic_data_only": True,
}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
