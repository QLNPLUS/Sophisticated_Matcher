# Changelog

## 1.3.0 - 2026-09-22

- The Multi NBT Matcher stores up to nine matcher rules in its own slot GUI. Like the single matcher's preview slot, placing a matcher only reads its rule: the matcher itself is never consumed or kept.
- Added a Sophisticated Matcher creative tab holding both matcher items.
- Added crafting recipes: Name Tag + Gold Ingot crafts the NBT Matcher, Name Tag + Diamond crafts the Multi NBT Matcher.
- The GUI uses a standard-sized container background with nine storage slots and the player inventory; no preview or NBT tree window.
- Each stored matcher shows a three-position switch under its slot; click the top, middle, or bottom third to set AND, BUT, or OR. Hovering a position shows a tooltip with its name and meaning.
- The first stored matcher's join state is ignored, so it gets no switch.
- Results fold left to right over the stored matchers: the first matcher decides, each following one combines through its join state (AND keeps both, BUT keeps the first but not the second, OR needs either).
- Shift-click a configured matcher from your inventory to add its rule; click an occupied slot with an empty cursor to clear it.
- The item tooltip lists the stored matchers and their join states.
- Both matcher items can be used in Sophisticated filter upgrades as before.

## 1.2.1 - 2026-09-21

- Renamed the editor action and screen title to NBT Matcher.
- Added Chinese and English translations for match operators, range controls, validation messages, and empty-data states.
- Localized saved rule summaries and matcher tooltips.

## 1.2.0 - 2026-09-21

- Added a dedicated matcher editor with a scrollable data tree and path-based comparison rules.
- Applied the supplied editor layout and updated button and scrollbar behavior.
- Matcher editor input fields now use white text so entered values stay readable.

## 1.1.0 - 2026-09-20

- Added the matcher to the Sophisticated Core creative tab when that tab is available.
- Updated the 1.20.1 Core build dependency to 1.5.1.2335 while keeping compatibility above the previous minimum.
- Fixed the matcher release metadata and publishing version.

## 1.0.0 - 2026-09-20

- Initial Forge 1.20.1 release.
- Added the Sophisticated Matcher item and its furnace-style GUI.
- Added ghost-item preview behavior with replacement-only interaction.
- Added top-level NBT entry selection and NBT-only matching support.
- Added matcher tooltip output for the saved NBT entry.
- Added CurseForge publishing workflow support.
