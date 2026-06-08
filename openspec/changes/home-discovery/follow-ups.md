# Follow-ups

- Koin change MUST delete `HomeViewModelFactory` and `CultureInterestViewModelFactory` and replace `viewModel(factory = ...)` call sites with `koinViewModel()` injection.
