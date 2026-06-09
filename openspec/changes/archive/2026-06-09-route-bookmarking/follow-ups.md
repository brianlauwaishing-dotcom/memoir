# Follow-ups

- Koin change MUST delete `RouteDetailViewModelFactory` and `SavedViewModelFactory` and replace `viewModel(factory = ...)` call sites with `koinViewModel { parametersOf(routeId) }` for RouteDetail and `koinViewModel()` for Saved.
