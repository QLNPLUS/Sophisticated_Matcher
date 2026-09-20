# Sophisticated Matcher

Sophisticated Matcher adds a matcher item for Sophisticated Storage and Sophisticated Backpacks.
It stores one selected top-level NBT entry and lets Sophisticated filter upgrades match that entry
without matching the item ID.

## Forge 1.20.1

- Minecraft: 1.20.1
- Loader: Forge 47.x
- Sophisticated Core: 1.3.6.1514 or newer
- Java: 17

The Forge 1.20.1 branch is the primary release branch. Other loader and Minecraft branches are
kept as separate worktrees and are not part of this stable 1.0.0 release.

## Features

- Right-click the matcher item to open a furnace-style GUI.
- Put an item in the preview slot without consuming it.
- Replace the preview item, but never extract it from the matcher.
- Choose one top-level NBT entry from the dropdown.
- Save the selected entry as the filter value.
- The saved matcher no longer stores the complete preview item NBT.
- The saved NBT entry is shown in the item tooltip.
- Use the saved entry in Sophisticated filter upgrades with item ID matching disabled.

Only the outermost NBT compound is selectable in the Forge 1.20.1 implementation. Nested NBT
path matching is not included yet.

## Usage

1. Obtain a Sophisticated Matcher item.
2. Right-click it and place an example item in the preview slot.
3. Select the NBT entry to match.
4. Press the save button.
5. Put the matcher into a Sophisticated Storage or Sophisticated Backpacks filter upgrade.
6. Disable item ID matching and enable NBT matching for the upgrade.

The preview item is a ghost copy. It is not consumed, and it cannot be taken out of the GUI.

## Building

Use JDK 17 and run the Gradle wrapper with the build task.

    ./gradlew build --no-daemon

The release artifact is:

    build/libs/sophisticated-matcher-forge-1.20.1-1.0.0.jar

## CurseForge publishing

The repository contains .github/workflows/publish-curseforge.yml. Configure these GitHub
repository settings before publishing:

- Repository variable CURSEFORGE_PROJECT_ID
- Repository secret CURSEFORGE_TOKEN

Publishing is triggered by a published GitHub Release or by a manual workflow run.

## License

All rights reserved. See LICENSE.
