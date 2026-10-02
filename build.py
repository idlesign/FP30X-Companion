#!/usr/bin/env python3
"""Build debug APKs or verified Google Play release packages."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parent
APP_GRADLE = ROOT / "app" / "build.gradle.kts"
BUILD_ROOT = ROOT / "build"
GRADLE_OUTPUTS = BUILD_ROOT / "work" / "outputs"
GRADLE_DEBUG_APK = GRADLE_OUTPUTS / "apk/debug/app-debug.apk"
GRADLE_RELEASE_APK = GRADLE_OUTPUTS / "apk/release/app-release-unsigned.apk"
GRADLE_RELEASE_AAB = GRADLE_OUTPUTS / "bundle/release/app-release.aab"
OUTPUTS = BUILD_ROOT / "outputs"
DEBUG_OUTPUT = OUTPUTS / "debug"
RELEASE_OUTPUT = OUTPUTS / "release"
PUB_SOURCE = ROOT / "pub"
PUBLICATION_OUTPUT = BUILD_ROOT / "staging" / "publication"
PACKAGE = PUBLICATION_OUTPUT / "package"
RELEASE = PACKAGE / "release"
DEFAULT_KEY_PROPERTIES = Path.home() / ".android" / "fp30x-companion-upload.properties"


def store_asset_instructions() -> str:
    lines = ["Release store images (not generated automatically):"]
    for locale in ("en-US", "ru-RU"):
        images = PUB_SOURCE / "metadata" / "android" / locale / "images"
        lines.extend(
            (
                f"  Screenshots: {images / 'phoneScreenshots'}",
                f"  Store icon:  {images / 'icon.png'}",
                f"  Feature art: {images / 'featureGraphic.png'}",
            )
        )
    lines.append("Each screenshot directory must contain 2 to 8 real PNG files.")
    return "\n".join(lines)


class BuildError(RuntimeError):
    """A build prerequisite or generated artifact is invalid."""


@dataclass(frozen=True)
class AppMetadata:
    application_id: str
    min_sdk: int
    target_sdk: int
    version_code: int
    version_name: str


@dataclass(frozen=True)
class SigningConfig:
    store_file: Path
    store_password: str
    key_alias: str
    key_password: str


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Build an installable debug APK or a verified Google Play release. "
            "Debug builds increment the SemVer patch component and versionCode; "
            "release builds increment minor, reset patch to zero, and increment "
            "versionCode."
        ),
        epilog=store_asset_instructions(),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--debug",
        action="store_true",
        help="increment patch and versionCode, then build a debug APK",
    )
    return parser.parse_args(argv)


def assignment(text: str, name: str, quoted: bool) -> str:
    value = r'"([^"]+)"' if quoted else r"([0-9]+)"
    match = re.search(
        rf"^[ \t]*{re.escape(name)}[ \t]*=[ \t]*{value}",
        text,
        flags=re.MULTILINE,
    )
    if match is None:
        raise BuildError(f"Could not read {name} from {APP_GRADLE}")
    return match.group(1)


def read_metadata(text: str | None = None) -> AppMetadata:
    source = APP_GRADLE.read_text(encoding="utf-8") if text is None else text
    return AppMetadata(
        application_id=assignment(source, "applicationId", quoted=True),
        min_sdk=int(assignment(source, "minSdk", quoted=False)),
        target_sdk=int(assignment(source, "targetSdk", quoted=False)),
        version_code=int(assignment(source, "versionCode", quoted=False)),
        version_name=assignment(source, "versionName", quoted=True),
    )


def version_parts(version_name: str) -> tuple[int, int, int]:
    match = re.fullmatch(r"([0-9]+)\.([0-9]+)\.([0-9]+)(?:[-+].*)?", version_name)
    if match is None:
        raise BuildError(
            f"versionName must use MAJOR.MINOR.PATCH format, got {version_name!r}"
        )
    return tuple(int(part) for part in match.groups())


def bump_patch(version_name: str) -> str:
    major, minor, patch = version_parts(version_name)
    return f"{major}.{minor}.{patch + 1}"


def bump_minor(version_name: str) -> str:
    major, minor, _patch = version_parts(version_name)
    return f"{major}.{minor + 1}.0"


def replace_versions(text: str, version_name: str, version_code: int) -> str:
    updated, code_count = re.subn(
        r'(^[ \t]*versionCode[ \t]*=[ \t]*)[0-9]+',
        rf"\g<1>{version_code}",
        text,
        count=1,
        flags=re.MULTILINE,
    )
    updated, name_count = re.subn(
        r'(^[ \t]*versionName[ \t]*=[ \t]*)"[^"]+"',
        rf'\g<1>"{version_name}"',
        updated,
        count=1,
        flags=re.MULTILINE,
    )
    if code_count != 1 or name_count != 1:
        raise BuildError(f"Could not update version fields in {APP_GRADLE}")
    return updated


def select_jdk() -> dict[str, str]:
    environment = os.environ.copy()
    configured = environment.get("JAVA_HOME")
    candidates = [
        Path(configured).expanduser() if configured else None,
        Path.home() / ".local/jdk-21",
        Path.home() / ".local/jdk/jdk-17.0.20.1+1",
        Path.home() / ".local/jdk/jdk-17",
        Path("/usr/lib/jvm/java-17-openjdk-amd64"),
        Path("/usr/lib/jvm/java-21-openjdk-amd64"),
    ]
    java_home = next(
        (candidate for candidate in candidates if candidate and (candidate / "bin/javac").is_file()),
        None,
    )
    if java_home is None:
        javac = shutil.which("javac", path=environment.get("PATH"))
        if javac:
            java_home = Path(javac).resolve().parent.parent
    if java_home is None:
        raise BuildError("A full JDK is required; JAVA_HOME does not contain bin/javac.")
    environment["JAVA_HOME"] = str(java_home)
    environment["PATH"] = f"{java_home / 'bin'}{os.pathsep}{environment.get('PATH', '')}"
    return environment


def require_tools(environment: dict[str, str], names: tuple[str, ...]) -> None:
    missing = [name for name in names if shutil.which(name, path=environment["PATH"]) is None]
    if missing:
        raise BuildError(f"Required tools not found: {', '.join(missing)}")


def run(
    command: list[str | Path],
    environment: dict[str, str],
    *,
    capture: bool = False,
    quiet: bool = False,
) -> subprocess.CompletedProcess[str]:
    normalized = [str(part) for part in command]
    return subprocess.run(
        normalized,
        cwd=ROOT,
        env=environment,
        check=True,
        text=True,
        capture_output=capture,
        stdout=subprocess.DEVNULL if quiet else None,
    )


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        result[name.strip()] = value.strip()
    return result


def read_signing_config() -> SigningConfig:
    properties_path = Path(
        os.environ.get("FP30X_UPLOAD_KEY_PROPERTIES", str(DEFAULT_KEY_PROPERTIES))
    ).expanduser()
    if not properties_path.is_file():
        raise BuildError(
            f"Signing properties not found: {properties_path}\n"
            "Restore the upload keystore and properties before building a release."
        )
    properties = read_properties(properties_path)
    required = ("storeFile", "storePassword", "keyAlias", "keyPassword")
    missing = [name for name in required if not properties.get(name)]
    if missing:
        raise BuildError(f"Signing properties are incomplete: {', '.join(missing)}")
    store_file = Path(properties["storeFile"]).expanduser()
    if not store_file.is_absolute():
        store_file = properties_path.parent / store_file
    if not store_file.is_file():
        raise BuildError(f"Upload keystore is missing: {store_file}")
    return SigningConfig(
        store_file=store_file,
        store_password=properties["storePassword"],
        key_alias=properties["keyAlias"],
        key_password=properties["keyPassword"],
    )


def require_debug_apk() -> Path:
    if not GRADLE_DEBUG_APK.is_file():
        raise BuildError(f"Gradle did not produce {GRADLE_DEBUG_APK}")
    return GRADLE_DEBUG_APK


def require_release_apk() -> Path:
    if not GRADLE_RELEASE_APK.is_file():
        raise BuildError(f"Gradle did not produce {GRADLE_RELEASE_APK}")
    return GRADLE_RELEASE_APK


def replace_output_directory(temporary: Path, destination: Path) -> None:
    if destination.exists():
        shutil.rmtree(destination)
    temporary.replace(destination)


def publish_debug_apk(source: Path) -> Path:
    OUTPUTS.mkdir(parents=True, exist_ok=True)
    temporary = OUTPUTS / ".debug.tmp"
    if temporary.exists():
        shutil.rmtree(temporary)
    temporary.mkdir()
    artifact = temporary / "app-debug.apk"
    shutil.copy2(source, artifact)
    replace_output_directory(temporary, DEBUG_OUTPUT)
    return DEBUG_OUTPUT / artifact.name


def stage_publication_sources() -> None:
    if PUBLICATION_OUTPUT.exists():
        shutil.rmtree(PUBLICATION_OUTPUT)

    def ignored(directory: str, names: list[str]) -> set[str]:
        current = Path(directory)
        result = {
            name for name in names
            if name in ("__pycache__", ".gitkeep")
            or name.endswith((".pyc", ".pyo"))
        }
        if current == PUB_SOURCE:
            result.update(
                name for name in names
                if name == "release"
                or name.startswith("FP30X-Companion-Google-Play-")
            )
        return result

    shutil.copytree(PUB_SOURCE, PACKAGE, ignore=ignored)


def sign_bundle(
    metadata: AppMetadata,
    signing: SigningConfig,
    environment: dict[str, str],
) -> tuple[Path, str]:
    if not GRADLE_RELEASE_AAB.is_file():
        raise BuildError(f"Gradle did not produce {GRADLE_RELEASE_AAB}")
    RELEASE.mkdir(parents=True, exist_ok=True)
    artifact = f"FP30X-Companion-{metadata.version_name}-v{metadata.version_code}.aab"
    signed = RELEASE / artifact
    for previous in RELEASE.glob("FP30X-Companion-*-v*.aab"):
        if previous != signed:
            previous.unlink()
    temporary = signed.with_suffix(signed.suffix + ".tmp")
    temporary.unlink(missing_ok=True)
    signing_environment = environment.copy()
    signing_environment["FP30X_STORE_PASSWORD"] = signing.store_password
    signing_environment["FP30X_KEY_PASSWORD"] = signing.key_password
    run(
        [
            "jarsigner",
            "-keystore",
            signing.store_file,
            "-storepass:env",
            "FP30X_STORE_PASSWORD",
            "-keypass:env",
            "FP30X_KEY_PASSWORD",
            "-sigalg",
            "SHA256withRSA",
            "-digestalg",
            "SHA-256",
            "-signedjar",
            temporary,
            GRADLE_RELEASE_AAB,
            signing.key_alias,
        ],
        signing_environment,
        quiet=True,
    )
    temporary.replace(signed)
    signed.chmod(0o644)
    run(["jarsigner", "-verify", signed], environment, quiet=True)

    certificate = RELEASE / "upload-certificate.pem"
    certificate.unlink(missing_ok=True)
    run(
        [
            "keytool",
            "-exportcert",
            "-rfc",
            "-keystore",
            signing.store_file,
            "-storepass:env",
            "FP30X_STORE_PASSWORD",
            "-alias",
            signing.key_alias,
            "-file",
            certificate,
        ],
        signing_environment,
        quiet=True,
    )
    certificate.chmod(0o644)
    listed = run(
        [
            "keytool",
            "-list",
            "-v",
            "-keystore",
            signing.store_file,
            "-storepass:env",
            "FP30X_STORE_PASSWORD",
            "-alias",
            signing.key_alias,
        ],
        {**signing_environment, "LANG": "C"},
        capture=True,
    )
    match = re.search(r"^[ \t]*SHA256:[ \t]*(.+)$", listed.stdout, re.MULTILINE)
    if match is None:
        raise BuildError("Could not read the upload certificate SHA-256 fingerprint.")
    return signed, match.group(1).strip()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def java_version(environment: dict[str, str]) -> str:
    result = run(["java", "-version"], environment, capture=True)
    lines = (result.stderr or result.stdout).splitlines()
    return lines[0] if lines else "unknown"


def write_release_metadata(
    metadata: AppMetadata,
    signed: Path,
    certificate_sha256: str,
    environment: dict[str, str],
) -> str:
    artifact_sha256 = sha256(signed)
    (RELEASE / "SHA256SUMS.txt").write_text(
        f"{artifact_sha256}  {signed.name}\n", encoding="utf-8"
    )
    manifest = {
        "applicationId": metadata.application_id,
        "artifact": signed.name,
        "builtAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "minSdk": metadata.min_sdk,
        "sha256": artifact_sha256,
        "sizeBytes": signed.stat().st_size,
        "targetSdk": metadata.target_sdk,
        "uploadCertificateSha256": certificate_sha256,
        "versionCode": metadata.version_code,
        "versionName": metadata.version_name,
    }
    (RELEASE / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    build_info = f"""# Release build information

| Field | Value |
|---|---|
| Application ID | `{metadata.application_id}` |
| Version name | `{metadata.version_name}` |
| Version code | `{metadata.version_code}` |
| Minimum SDK | `{metadata.min_sdk}` |
| Target / compile SDK | `{metadata.target_sdk}` |
| Artifact | `{signed.name}` |
| Upload certificate SHA-256 | `{certificate_sha256}` |

## Build and verification

The artifact was produced with JDK `{java_version(environment)}`.

The AAB is signed with the dedicated upload key using `SHA256withRSA` and
`SHA-256`. A self-signed certificate is expected for a Google Play upload key;
Google Play App Signing signs delivered APKs with its separate app-signing key.

Verify the artifact checksum with:

```bash
cd build/outputs/release
sha256sum -c SHA256SUMS.txt
```

`upload-certificate.pem` contains only the public certificate and does not contain
the private key.
"""
    (RELEASE / "BUILD_INFO.md").write_text(build_info, encoding="utf-8")
    return artifact_sha256


def packaged_path(path: Path) -> str:
    try:
        return str(path.relative_to(PACKAGE))
    except ValueError:
        return str(path)


def check_publication_text(path: Path, limit: int, errors: list[str]) -> None:
    if not path.is_file():
        errors.append(f"missing {packaged_path(path)}")
        return
    value = path.read_text(encoding="utf-8").strip()
    if not value:
        errors.append(f"empty {packaged_path(path)}")
    if len(value) > limit:
        errors.append(
            f"{packaged_path(path)} has {len(value)} characters, limit {limit}"
        )
    for marker in ("TODO", "REPLACE_ME", "[PUBLISHER", "example.com"):
        if marker in value:
            errors.append(f"placeholder {marker!r} in {packaged_path(path)}")


def png_dimensions(path: Path) -> tuple[int, int]:
    with path.open("rb") as source:
        header = source.read(24)
    if (
        len(header) != 24
        or header[:8] != b"\x89PNG\r\n\x1a\n"
        or header[12:16] != b"IHDR"
    ):
        raise BuildError(f"{path} is not a PNG image")
    return int.from_bytes(header[16:20], "big"), int.from_bytes(
        header[20:24], "big"
    )


def check_publication_png(
    path: Path,
    errors: list[str],
    *,
    expected: tuple[int, int] | None = None,
    phone_screenshot: bool = False,
) -> None:
    if not path.is_file():
        errors.append(f"missing {packaged_path(path)}")
        return
    try:
        width, height = png_dimensions(path)
    except (BuildError, OSError) as error:
        errors.append(str(error))
        return
    if expected is not None and (width, height) != expected:
        errors.append(
            f"{packaged_path(path)} is {width}x{height}, "
            f"expected {expected[0]}x{expected[1]}"
        )
    if phone_screenshot:
        shorter, longer = sorted((width, height))
        if shorter < 320 or longer > 3840 or longer > shorter * 2:
            errors.append(
                f"{packaged_path(path)} has unsupported phone screenshot "
                f"dimensions {width}x{height}"
            )
    if path.stat().st_size > 8 * 1024 * 1024:
        errors.append(f"{packaged_path(path)} exceeds 8 MiB")


def check_phone_screenshots(directory: Path, errors: list[str]) -> None:
    screenshots = []
    if directory.is_dir():
        screenshots = sorted(
            path
            for path in directory.iterdir()
            if path.is_file() and path.suffix.lower() == ".png"
        )
    if not 2 <= len(screenshots) <= 8:
        errors.append(
            f"{packaged_path(directory)} must contain 2 to 8 real PNG screenshots; "
            f"found {len(screenshots)}"
        )
    for screenshot in screenshots:
        check_publication_png(screenshot, errors, phone_screenshot=True)


def validate_store_sources(version_code: int) -> None:
    errors: list[str] = []
    for locale in ("en-US", "ru-RU"):
        base = PUB_SOURCE / "metadata" / "android" / locale
        check_publication_text(base / "title.txt", 30, errors)
        check_publication_text(base / "short_description.txt", 80, errors)
        check_publication_text(base / "full_description.txt", 4000, errors)
        check_publication_text(
            base / "changelogs" / f"{version_code}.txt", 500, errors
        )
        images = base / "images"
        check_publication_png(images / "icon.png", errors, expected=(512, 512))
        check_publication_png(
            images / "featureGraphic.png", errors, expected=(1024, 500)
        )
        check_phone_screenshots(images / "phoneScreenshots", errors)

    policy = PUB_SOURCE / "compliance" / "privacy-policy.html"
    if not policy.is_file() or "idlelink@yandex.ru" not in policy.read_text(
        encoding="utf-8"
    ):
        errors.append(f"{policy} is missing or has no contact address")

    if errors:
        details = "\n".join(f"- {error}" for error in errors)
        raise BuildError(
            "Publication sources are incomplete:\n"
            f"{details}\n\n{store_asset_instructions()}"
        )


def check_signed_aab(path: Path, errors: list[str]) -> None:
    if not path.is_file():
        errors.append(f"missing {packaged_path(path)}")
        return
    try:
        with zipfile.ZipFile(path) as archive:
            names = set(archive.namelist())
            required = ("base/manifest/AndroidManifest.xml", "base/resources.pb")
            for name in required:
                if name not in names:
                    errors.append(f"{path.name} lacks {name}")
            signed = any(
                name.startswith("META-INF/")
                and name.endswith((".RSA", ".EC", ".DSA"))
                for name in names
            )
            if not signed:
                errors.append(f"{path.name} has no JAR signing certificate")
    except zipfile.BadZipFile:
        errors.append(f"{path.name} is not a valid ZIP/AAB")


def validate_package() -> None:
    errors: list[str] = []
    manifest = RELEASE / "manifest.json"
    release: dict[str, object] = {}
    if not manifest.is_file():
        errors.append("missing release/manifest.json")
    else:
        try:
            loaded = json.loads(manifest.read_text(encoding="utf-8"))
            if isinstance(loaded, dict):
                release = loaded
            else:
                errors.append("release/manifest.json must contain a JSON object")
        except (json.JSONDecodeError, OSError) as error:
            errors.append(f"invalid release/manifest.json: {error}")

    version_code = str(release.get("versionCode", ""))
    artifact = str(release.get("artifact", ""))
    for locale in ("en-US", "ru-RU"):
        base = PACKAGE / "metadata" / "android" / locale
        check_publication_text(base / "title.txt", 30, errors)
        check_publication_text(base / "short_description.txt", 80, errors)
        check_publication_text(base / "full_description.txt", 4000, errors)
        if version_code:
            check_publication_text(
                base / "changelogs" / f"{version_code}.txt", 500, errors
            )
        images = base / "images"
        check_publication_png(images / "icon.png", errors, expected=(512, 512))
        check_publication_png(
            images / "featureGraphic.png", errors, expected=(1024, 500)
        )
        check_phone_screenshots(images / "phoneScreenshots", errors)

    policy = PACKAGE / "compliance" / "privacy-policy.html"
    if not policy.is_file() or "idlelink@yandex.ru" not in policy.read_text(
        encoding="utf-8"
    ):
        errors.append("privacy-policy.html is missing or has no contact address")

    aab = RELEASE / artifact if artifact else RELEASE / "missing.aab"
    check_signed_aab(aab, errors)
    if manifest.is_file() and aab.is_file():
        if release.get("sha256") != sha256(aab):
            errors.append("release/manifest.json SHA-256 does not match the AAB")

    if errors:
        details = "\n".join(f"- {error}" for error in errors)
        raise BuildError(f"Publication package validation failed:\n{details}")
    print("Publication package validation passed.")


def create_archive(version_name: str) -> tuple[Path, str]:
    PUBLICATION_OUTPUT.mkdir(parents=True, exist_ok=True)
    archive = PUBLICATION_OUTPUT / f"FP30X-Companion-Google-Play-{version_name}.zip"
    checksum = Path(f"{archive}.sha256")
    root_name = archive.stem
    with zipfile.ZipFile(
        archive, "w", zipfile.ZIP_DEFLATED, compresslevel=9
    ) as output:
        for path in sorted(PACKAGE.rglob("*")):
            if path.is_file():
                output.write(path, Path(root_name) / path.relative_to(PACKAGE))
    digest = sha256(archive)
    checksum.write_text(f"{digest}  {archive.name}\n", encoding="utf-8")
    return archive, digest


def publish_release_outputs(
    signed: Path,
    archive: Path,
    unsigned_apk: Path,
) -> tuple[Path, Path, Path]:
    OUTPUTS.mkdir(parents=True, exist_ok=True)
    temporary = OUTPUTS / ".release.tmp"
    if temporary.exists():
        shutil.rmtree(temporary)
    temporary.mkdir()

    for source in sorted(RELEASE.iterdir()):
        destination = temporary / source.name
        if source.is_dir():
            shutil.copytree(source, destination)
        else:
            shutil.copy2(source, destination)
    for name in ("metadata", "compliance"):
        source = PACKAGE / name
        if source.is_dir():
            shutil.copytree(source, temporary / name)
    shutil.copy2(unsigned_apk, temporary / "app-release-unsigned.apk")
    shutil.copy2(archive, temporary / archive.name)
    archive_checksum = Path(f"{archive}.sha256")
    shutil.copy2(archive_checksum, temporary / archive_checksum.name)

    replace_output_directory(temporary, RELEASE_OUTPUT)
    final_signed = RELEASE_OUTPUT / signed.name
    final_archive = RELEASE_OUTPUT / archive.name
    final_apk = RELEASE_OUTPUT / "app-release-unsigned.apk"
    shutil.rmtree(PUBLICATION_OUTPUT)
    return final_signed, final_archive, final_apk


def debug_build(environment: dict[str, str], current: AppMetadata) -> None:
    next_metadata = AppMetadata(
        application_id=current.application_id,
        min_sdk=current.min_sdk,
        target_sdk=current.target_sdk,
        version_code=current.version_code + 1,
        version_name=bump_patch(current.version_name),
    )
    original_gradle = APP_GRADLE.read_text(encoding="utf-8")
    updated_gradle = replace_versions(
        original_gradle, next_metadata.version_name, next_metadata.version_code
    )
    print(
        f"Debug version: {current.version_name} -> {next_metadata.version_name}; "
        f"versionCode {current.version_code} -> {next_metadata.version_code}.",
        flush=True,
    )
    APP_GRADLE.write_text(updated_gradle, encoding="utf-8")
    try:
        run([ROOT / "gradlew", "assembleDebug"], environment)
        debug_apk = publish_debug_apk(require_debug_apk())
    except BaseException:
        APP_GRADLE.write_text(original_gradle, encoding="utf-8")
        print(
            "Debug build failed; version fields were restored to "
            f"{current.version_name} / {current.version_code}.",
            file=sys.stderr,
        )
        raise
    print(f"\nDebug artifact is ready:\n  APK: {debug_apk}")


def release_build(environment: dict[str, str], current: AppMetadata) -> None:
    next_metadata = AppMetadata(
        application_id=current.application_id,
        min_sdk=current.min_sdk,
        target_sdk=current.target_sdk,
        version_code=current.version_code + 1,
        version_name=bump_minor(current.version_name),
    )
    validate_store_sources(next_metadata.version_code)
    signing = read_signing_config()
    original_gradle = APP_GRADLE.read_text(encoding="utf-8")
    updated_gradle = replace_versions(
        original_gradle, next_metadata.version_name, next_metadata.version_code
    )
    print(
        f"Release version: {current.version_name} -> {next_metadata.version_name}; "
        f"versionCode {current.version_code} -> {next_metadata.version_code}.",
        flush=True,
    )
    APP_GRADLE.write_text(updated_gradle, encoding="utf-8")
    try:
        run(
            [
                ROOT / "gradlew",
                "clean",
                "test",
                "lint",
                "assembleDebug",
                "assembleRelease",
                "bundleRelease",
            ],
            environment,
        )
        debug_apk = require_debug_apk()
        release_apk = require_release_apk()
        stage_publication_sources()
        signed, certificate_sha256 = sign_bundle(next_metadata, signing, environment)
        artifact_sha256 = write_release_metadata(
            next_metadata, signed, certificate_sha256, environment
        )
        validate_package()
        archive, _archive_sha256 = create_archive(next_metadata.version_name)
        debug_apk = publish_debug_apk(debug_apk)
        signed, archive, release_apk = publish_release_outputs(
            signed, archive, release_apk
        )
    except BaseException:
        APP_GRADLE.write_text(original_gradle, encoding="utf-8")
        print(
            "Release failed; version fields were restored to "
            f"{current.version_name} / {current.version_code}.",
            file=sys.stderr,
        )
        raise

    print("\nBuild artifacts are ready:")
    print(f"  Debug:   {debug_apk}")
    print(f"  Release: {release_apk} (unsigned APK)")
    print(f"  AAB:     {signed}")
    print(f"  Archive: {archive}")
    print(f"  SHA-256: {artifact_sha256}")


def fail(message: str) -> NoReturn:
    print(f"Build failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        environment = select_jdk()
        require_tools(
            environment,
            ("java", "javac") if args.debug else ("java", "javac", "keytool", "jarsigner"),
        )
        metadata = read_metadata()
        if args.debug:
            debug_build(environment, metadata)
        else:
            release_build(environment, metadata)
    except BuildError as error:
        fail(str(error))
    except subprocess.CalledProcessError as error:
        fail(f"command exited with code {error.returncode}: {' '.join(error.cmd)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
