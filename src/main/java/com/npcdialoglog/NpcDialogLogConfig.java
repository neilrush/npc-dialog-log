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
}
