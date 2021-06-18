package com.npcdialoglog;

import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Npc Dialog Log"
)
public class NpcDialogLog extends Plugin
{
	@Inject
	private Client client;

	private Actor actor = null;
	private Dialog lastNPCDialogue = null;
	private Dialog lastPlayerDialogue = null;

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (actor != null)
		{
			checkWidgetDialogs();
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getTarget() == null || event.getSource() != client.getLocalPlayer())
		{
			return;
		}
		lastNPCDialogue = null;
		lastPlayerDialogue = null;
		actor = event.getTarget();
	}

	private void checkWidgetDialogs()
	{
		final Dialog npcDialog = getWidgetDialogueSafely();
		final Dialog playerDialog = getWidgetDialogueSafely(WidgetID.DIALOG_PLAYER_GROUP_ID, WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());//using the npc children id as they seem to be the same

		// For when the NPC has dialog
		if (npcDialog.text != null && (lastNPCDialogue == null || !lastNPCDialogue.text.equals(npcDialog.text)))
		{
			lastNPCDialogue = npcDialog;
			if (npcDialog.name != null)
			{
				lastPlayerDialogue = null; //npc has dialog box now so safe to reset player dialog
				client.addChatMessage(ChatMessageType.PUBLICCHAT, npcDialog.name, npcDialog.text, npcDialog.name);
			}
		}

		//For when your player has dialogue
		if (playerDialog.text != null && (lastPlayerDialogue == null || !lastPlayerDialogue.text.equals(playerDialog.text)))
		{
			lastPlayerDialogue = playerDialog;
			if (playerDialog.name != null)
			{
				lastNPCDialogue = null; //player has dialog box now so safe reset npc dialog
				client.addChatMessage(ChatMessageType.PUBLICCHAT, playerDialog.name, playerDialog.text, playerDialog.name);
			}
		}
	}

	private Dialog getWidgetDialogueSafely()
	{
		return getWidgetDialogueSafely(WidgetInfo.DIALOG_NPC_TEXT.getGroupId(), WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());
	}

	private Dialog getWidgetDialogueSafely(final int group, final int nameChild, final int textChild)
	{
		return new Dialog(client.getWidget(group, nameChild) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, nameChild).getText()), client.getWidget(group, textChild) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, textChild).getText()));
	}

	protected static class Dialog
	{
		@Getter
		private final String name;
		@Getter
		private final String text;


		public Dialog(String name, String text)
		{
			this.name = name;
			this.text = text;
		}
	}
}
