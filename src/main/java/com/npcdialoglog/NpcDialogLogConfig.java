package com.npcdialoglog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("npcDialogLog")
public interface NpcDialogLogConfig extends Config
{
	@ConfigSection(
		name = "Chat Dialog",
		description = "All options that enable chat dialog logging",
		position = 0,
		closedByDefault = false
	)
	String chatDialogSection = "chatDialog";

	@ConfigSection(
		name = "Overhead Text",
		description = "All options that enable overhead text dialog",
		position = 1,
		closedByDefault = false
	)
	String overheadTextSection = "overheadText";

	@ConfigItem(
		keyName = "displayPlayerDialog",
		name = "Player Dialog",
		description = "Add player dialog to chat",
		section  =  chatDialogSection,
		position = 0
	)
	default boolean displayPlayerDialog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "displayNpcDialog",
		name = "NPC Dialog",
		description = "Add NPC dialog to chat",
		section  =  chatDialogSection,
		position = 1

	)
	default boolean displayNpcDialog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "displayPlayerOverheadText",
		name = "Player Overhead Text",
		description = "Add dialog over the head of the player",
		section  =  overheadTextSection,
		position = 0
	)

	default boolean displayPlayerOverheadText()
	{
		return false;
	}

	@ConfigItem(
		keyName = "displayNpcOverheadText",
		name = "NPC Overhead Text",
		description = "Add dialog over the head of npcs",
		section  =  overheadTextSection,
		position = 1
	)

	default boolean displayNpcOverheadText()
	{
		return false;
	}
}
