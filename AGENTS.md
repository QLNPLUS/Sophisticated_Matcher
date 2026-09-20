# Sophisticated Matcher branch rules

## Branch matrix

| Branch | Worktree | Loader | Minecraft | JDK | Gradle | Build plugin |
|---|---|---|---|---:|---|---|
| forge-1.20.1 | D:\\projects\\Sophisticated_Matcher\\forge-1.20.1 | Forge | 1.20.1 | 17 | 8.8 | ForgeGradle 6.0.54 |
| neoforge-1.21.1 | D:\\projects\\Sophisticated_Matcher\\neoforge-1.21.1 | NeoForge | 1.21.1 | 21 | 8.8 | ModDevGradle 2.0.141 |
| neoforge-26.1.2 | D:\\projects\\Sophisticated_Matcher\\neoforge-26.1.2 | NeoForge | 26.1.2 | 25 | 9.2.1 | ModDevGradle 2.0.146 |

forge-1.20.1 is the primary worktree and the stable release branch. It owns the repository .git
directory. The other entries are linked worktrees. Keep directory, local branch, and remote branch
names identical.

## Change propagation

- Develop and verify a change on one version branch before propagation.
- Move cross-version changes with git cherry-pick -x; do not hand-rewrite them on another branch.
- A conflict is diagnostic information and must be resolved explicitly.
- A user report that a version works or asks to sync versions triggers propagation.
- Classify each target as applies, does not apply because it is platform-specific, or needs API
  adaptation, and report that conclusion.
- A bug fixed on a secondary branch must be cherry-picked back to the primary branch first.

## Known platform gaps

- Forge 1.20.1 uses item NBT; newer NeoForge branches use data components where the game API
  requires them.
- Networking, menu registration, client GUI APIs, and loader metadata are platform-specific.
- Do not force these differences into a shared abstraction without first isolating a stable shim.
- The Forge 1.20.1 branch is the only branch covered by the stable 1.0.0 release workflow.

## Releases and CI

Use versioned tags in the form v<version>-<loader>-<minecraft-version>, for example
v1.0.0-forge-1.20.1. Do not rename historical tags without an explicit migration plan.

The CurseForge workflow publishes only the exact Forge 1.20.1 artifact after checking
CURSEFORGE_PROJECT_ID as a repository variable and CURSEFORGE_TOKEN as a repository secret.
Any branch rename must be reflected in the workflow checkout ref and documentation.
