# Releasing Simple Icons

Releases are built, signed, and published by `.github/workflows/release.yml`. The workflow only runs for tags in the form `v<SemVer>`, such as `v1.0.1` or `v1.1.0-beta.1`.

## Repository Setup

Create a GitHub Environment named `jetbrains-marketplace`. Configure required reviewers if releases need manual approval, then add these environment secrets:

- `PUBLISH_TOKEN`: a permanent JetBrains Marketplace token.
- `CERTIFICATE_CHAIN`: the PEM certificate chain used to sign the plugin.
- `PRIVATE_KEY`: the corresponding PEM private key.
- `PRIVATE_KEY_PASSWORD`: the private-key password.

Keep multiline PEM values unchanged. Do not store signing material in repository files or workflow variables.
See the JetBrains documentation for [plugin signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html) and [Marketplace uploads](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).

## Release Procedure

1. Update `pluginVersion` in `gradle.properties` using Semantic Versioning.
2. Move user-facing changes from `Unreleased` into a matching dated section in `CHANGELOG.md`.
3. Merge the release commit into `master` and wait for the `CI` workflow to pass.
4. Create and push the matching tag:

   ```bash
   git tag -a v1.0.1 -m "Release 1.0.1"
   git push origin v1.0.1
   ```

The release workflow rejects mismatched versions or missing changelog sections. It runs tests and IntelliJ Plugin Verifier, signs and publishes the plugin, uploads the signed ZIP as a workflow artifact, and creates the GitHub Release. Pre-release versions are published to the Marketplace channel derived from the first pre-release identifier, such as `beta` for `1.1.0-beta.1`.
