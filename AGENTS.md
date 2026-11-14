User will run build, ask user to build if you need to.

- Adaptive UI: use `LocalWindowSizeClass.current` (`isTablet >= 600dp`, `isExpanded >= 840dp`).
- Git/PRs: use Conventional Commits (`feat:`, `fix:`, `chore:`); pass PR bodies via `--body-file - << 'EOF'` to avoid shell expansion.
- Screenshots: Paparazzi snapshots are recorded locally via `./gradlew recordPaparazziDebug` (user runs build).
