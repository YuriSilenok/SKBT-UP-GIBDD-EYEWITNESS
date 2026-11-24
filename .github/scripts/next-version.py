"""Allocate the next published version, independent of CI run numbers.

The main workflow is serialized through its existing workflow concurrency group.
Failed builds and publish=false builds do not create a release and consume no version.
"""
import json
import os
import re
import subprocess


def api(path, binary=False):
    args = ["gh", "api", path]
    if binary:
        args += ["-H", "Accept: application/octet-stream"]
    return json.loads(subprocess.run(args, check=True, capture_output=True, text=True).stdout)


def calculate(releases, metadata):
    versions = [tuple(map(int, match.groups())) for release in releases
                if (match := re.fullmatch(r"v(\d+)\.(\d+)\.(\d+)", release["tag_name"]))]
    if versions:
        major, minor, patch = max(versions)
        name = f"{major}.{minor}.{patch + 1}"
    else:
        name = "1.0.0"
    codes = [item["version_code"] for item in metadata]
    if any(type(code) is not int or code < 1 for code in codes):
        raise ValueError("Invalid version_code in published BUILD-INFO.json")
    code = max(codes, default=0) + 1
    if code > 2100000000:
        raise ValueError("Android versionCode limit reached")
    return name, code


def main():
    repo = os.environ["GITHUB_REPOSITORY"]
    pages = json.loads(subprocess.run(["gh", "api", "--paginate", "--slurp",
        f"repos/{repo}/releases?per_page=100"], check=True, capture_output=True, text=True).stdout)
    releases = [r for page in pages for r in page if not r["draft"]]
    releases.sort(key=lambda r: r.get("published_at") or r["created_at"], reverse=True)
    metadata = []
    # Legacy test-N releases are read until the most recent sequential vX.Y.Z release.
    # That release carries the high-water mark of all earlier version codes.
    for release in releases:
        asset = next((a for a in release["assets"] if a["name"] == "BUILD-INFO.json"), None)
        if asset:
            metadata.append(api(f"repos/{repo}/releases/assets/{asset['id']}", binary=True))
        if asset and re.fullmatch(r"v\d+\.\d+\.\d+", release["tag_name"]):
            break
    name, code = calculate(releases, metadata)
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"version_name={name}\nversion_code={code}\nrelease_tag=v{name}\n")
    with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as output:
        output.write(f"CI_VERSION_NAME={name}\nCI_VERSION_CODE={code}\n")
    print(f"Next published version: {name}; Android versionCode: {code}")


if __name__ == "__main__":
    main()
