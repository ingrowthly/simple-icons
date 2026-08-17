<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Simple-icons Changelog

## [Unreleased]

## [1.0.1] - 2026-08-17

### Changed

- Indexed glob and path rules to reduce icon lookup overhead
- Made icon loading thread-safe and removed unnecessary runtime dependencies
- Added build-time validation for rule priorities, glob syntax, and SVG references
- Automated verified, signed releases to JetBrains Marketplace from SemVer tags

### Fixed

- Match directory-aware rules against absolute Windows and Unix paths
- Respect rule priority when multiple file extensions match
- Correct missing and case-sensitive file icon references

## [1.0.0] - 2026-03-24

### Added

- Support for file icons

## [0.0.1] - 2024-07-22

### Added

- Initial release of the plugin
- Custom folder icons
