# NPC Dialog Log [![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/neilrush/npc-dialog-log/Java%20CI%20with%20Gradle/master?logo=github)](https://github.com/neilrush/npc-dialog-log/actions) [![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/neilrush/npc-dialog-log?include_prereleases&logo=github)](https://github.com/neilrush/npc-dialog-log/releases) [![Plugin Installs](http://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/installs/plugin/npc-dialog-log)](https://runelite.net/plugin-hub/neilrush)

This RuneLite plugin adds dialog from NPCs, the player and message boxes (objects, items, signs) to the chat as public chat.
Great for the player that holds spacebar during quests and needs to go back to see what to do.
This plugin also adds the option to display dialog overhead for immersion 🙂.

![.](https://i.imgur.com/sDbYp9N.gif)

## Configuration

![.](https://i.imgur.com/lo1IcYz.png)

| Option               | Description                                  |
|----------------------|----------------------------------------------|
| Dialog Output        | Where logged dialog is shown: the chat box, a side panel, or both. The side panel keeps the last 500 lines of the current session in memory; click a name or line to copy it. |
| Player Dialog        | Adds dialog said by the player to the chat.  |
| NPC Dialog           | Adds dialog said by Npcs to the chat.        |
| Message Box Dialog   | Adds dialog from message boxes without a speaker (objects, items, signs) to the chat. |
| Player Overhead Text | Displays dialog said by the player overhead. |
| NPC Overhead Text    | Displays dialog said by Npcs overhead.       |
| Player Overhead Color | Color of dialog displayed over the head of the player. |
| NPC Overhead Color   | Color of dialog displayed over the head of Npcs. |
| Ignored NPCs         | Comma separated list of NPC names whose dialog is not logged or shown overhead. Supports `*` as a wildcard, e.g. `Banker*`. |
