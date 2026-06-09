## Follow-ups

- Koin change MUST delete `SpotIntroViewModelFactory`, `ArtifactDiscoveryViewModelFactory`, `ArtifactDetailViewModelFactory` and replace their `viewModel(factory = ...)` call sites with `koinViewModel { parametersOf(spotId, artifactId) }`.
