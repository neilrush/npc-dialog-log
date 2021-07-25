package com.npcdialoglog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("npcDialogLog")
public interface NpcDialogLogConfig extends Config
{
	@ConfigItem(
		keyName = "displayPlayerDialog",
		name = "Player Dialog",
		description = "Add player dialog to chat",
		position = 1
	)
	default boolean displayPlayerDialog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "displayNpcDialog",
		name = "NPC Dialog",
		description = "Add NPC dialog to chat",
		position = 2
	)
	default boolean displayNpcDialog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "displayPlayerOverheadText",
		name = "Player Overhead Text",
		description = "Add dialog over the head of the player",
		position = 3
	)

	default boolean displayPlayerOverheadText()
	{
		return false;
	}

	@ConfigItem(
		keyName = "displayNpcOverheadText",
		name = "NPC Overhead Text",
		description = "Add dialog over the head of npcs",
		position = 4
	)

	default boolean displayNpcOverheadText()
	{
		return false;
	}
}
