package com.npcdialoglog;

import javax.inject.Inject;
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
	private String lastNPCText = "";
	private String lastPlayerText = "";

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
		if(event.getSource() != actor && actor != null){
			actor.setOverheadText(null);
		}
		lastNPCText = "";
		lastPlayerText = "";
		actor = event.getTarget();
	}

	private void checkWidgetDialogs()
	{
		final String npcDialogText = getWidgetTextSafely(WidgetInfo.DIALOG_NPC_TEXT);
		final String playerDialogText = getWidgetTextSafely(WidgetID.DIALOG_PLAYER_GROUP_ID, 4);

		// For when the NPC has dialog
		if (npcDialogText != null && !lastNPCText.equals(npcDialogText))
		{
			lastNPCText = npcDialogText;
			client.addChatMessage(ChatMessageType.PUBLICCHAT,actor.getName(), npcDialogText, actor.getName());
		}

		//For when your player has dialogue
		if (playerDialogText != null && !lastPlayerText.equals(playerDialogText))
		{
			lastPlayerText = playerDialogText;
			if (client.getLocalPlayer() != null)
			{
				client.addChatMessage(ChatMessageType.PUBLICCHAT,client.getLocalPlayer().getName(), playerDialogText, client.getLocalPlayer().getName());
			}
		}
	}
	private String getWidgetTextSafely(final WidgetInfo info)
	{
		return getWidgetTextSafely(info.getGroupId(), info.getChildId());
	}

	private String getWidgetTextSafely(final int group, final int child)
	{
		return client.getWidget(group, child) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, child).getText());
	}
}