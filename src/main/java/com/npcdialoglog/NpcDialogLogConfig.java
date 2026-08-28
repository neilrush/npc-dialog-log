package com.npcdialoglog;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(NpcDialogLogConfig.GROUP)
public interface NpcDialogLogConfig extends Config
{
	String GROUP = "npcDialogLog";

	/**
	 * The color the game draws overhead text in
	 */
	Color DEFAULT_OVERHEAD_COLOR = Color.YELLOW;

	@ConfigItem(
		keyName = "dialogOutput",
		name = "Dialog Output",
		description = "Where logged dialog is shown: the chat box, the side panel, or both",
		position = -1
	)
	default DialogOutput dialogOutput()
	{
		return DialogOutput.CHAT;
	}

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
		keyName = "displayMessageBoxDialog",
		name = "Message Box Dialog",
		description = "Add dialog from message boxes without a speaker (objects, items, signs) to chat",
		section  =  chatDialogSection,
		position = 2
	)
	default boolean displayMessageBoxDialog()
	{
		return false;
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

	@ConfigItem(
		keyName = "playerOverheadColor",
		name = "Player Overhead Color",
		description = "Color of dialog over the head of the player",
		section  =  overheadTextSection,
		position = 2
	)
	default Color playerOverheadColor()
	{
		return DEFAULT_OVERHEAD_COLOR;
	}

	@ConfigItem(
		keyName = "npcOverheadColor",
		name = "NPC Overhead Color",
		description = "Color of dialog over the head of npcs",
		section  =  overheadTextSection,
		position = 3
	)
	default Color npcOverheadColor()
	{
		return DEFAULT_OVERHEAD_COLOR;
	}

	@ConfigItem(
		keyName = "ignoredNpcs",
		name = "Ignored NPCs",
		description = "Comma separated list of NPC names whose dialog is not logged or shown overhead. Supports * as a wildcard, e.g. Banker*",
		position = 2
	)
	default String ignoredNpcs()
	{
		return "";
	}
}
