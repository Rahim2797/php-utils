# Properties Fixture Scenarios

This folder mirrors IDE-oriented PHP stimuli from the Kotlin test suite while keeping the original top-level fixture files intact for existing path-based integration tests.

## Grouped samples

- `Laravel/`: model and helper-backed Laravel scenarios
- `CrossFile/`: cross-file return, property, contract, and union scenarios
- `Core/`: local receiver, array literal, imported alias, and legacy identifier scenarios
- `Completion/`: PHPDoc completion and insertion scenarios
- `Docs/`: custom documentation rendering scenarios
- `Inspection/`: contradiction and signature-suppression scenarios

## Kotlin-only tests

These tests stay in Kotlin because they do not map cleanly to a project file stimulus:

- `MagicTypeTargetResolverGuardTest`
- `PropertiesDumbModeGuardsTest`
- `PropertiesStubLibraryRootsProviderTest`
- `PropertiesTypeInspectorTest`

`PropertiesArrayAccessTypeProviderTest` dumb-mode assertions are represented by the same PHP samples used for normal editor cases.
