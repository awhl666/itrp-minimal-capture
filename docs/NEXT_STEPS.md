# Next steps

This is the suggested future work list for maintaining the project after the initial GitHub upload.

## Highest priority

### 1. Add checksums

Add SHA-256 checksums for all jars in `builds/` and release assets.

Why:

- Users can verify downloads.
- Future maintainers can detect accidental jar replacement.

Suggested file:

```text
docs/CHECKSUMS_SHA256.txt
```

### 2. Improve download table

README should eventually include a table like:

```text
Loader | Minecraft | File | Status
```

Why:

- Users should not need to inspect filenames manually.

### 3. Add issue templates

Add templates for:

- Bug report
- Version request
- Crash report

Why:

- Future debugging needs Minecraft version, loader version, dependency list, and logs.

## Medium priority

### 4. Re-add or regenerate Gradle wrapper jar

The wrapper jar is currently absent from GitHub source because the maintenance upload used GitHub Contents API and that API rejected the wrapper binary.

Options:

- Use normal git tooling from a desktop environment to add `gradle-wrapper.jar`.
- Regenerate wrapper with a trusted Gradle installation.
- Keep requiring local Gradle and document that clearly.

### 5. Add GitHub Actions build workflow

A CI workflow can help detect compile breakage, especially when adding more versions.

Suggested path:

```text
.github/workflows/build.yml
```

Start with NeoForge 1.21.1 only, then expand.

### 6. Preserve Fabric generated source/project layout

Currently the repository includes the Fabric multi-version automation script and published jars. If long-term Fabric development is expected, add a clearer Fabric source/project template so maintainers can edit Fabric code without reconstructing the temporary build workspace.

## Lower priority

### 7. Add license

Add a license once the owner chooses one.

Common choices:

- MIT: simple permissive license.
- LGPL/GPL: stronger copyleft requirements.
- All rights reserved: not open source, only source-visible.

### 8. Add screenshots

Add screenshots of the mod UI to README for users.

### 9. Add changelog

Suggested file:

```text
CHANGELOG.md
```

## AI/human maintainer guidance

For any next instruction:

1. Read `docs/MAINTAINER_CONTEXT.md` first.
2. Read the relevant source files.
3. Do not assume all Minecraft 1.21.x versions have identical APIs.
4. Build and launch-test before updating tested status.
5. Update docs and GitHub Release after producing artifacts.