package com.npcdialoglog;

import java.awt.Color;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ChatColorConfig;
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

	@Inject
	private ChatColorConfig chatColorConfig;

	@Inject
	private ChatMessageManager chatMessageManager;

	private Actor actor = null;
	private Dialog lastNpcDialog = null;
	private Dialog lastPlayerDialog = null;

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
		lastNpcDialog = null;
		lastPlayerDialog = null;
		actor = event.getTarget();
	}

	private void checkWidgetDialogs()
	{
		final Dialog npcDialog = getWidgetDialogSafely();
		final Dialog playerDialog = getWidgetDialogSafely(WidgetID.DIALOG_PLAYER_GROUP_ID, WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());//using the npc children id as they seem to be the same

		// For when the NPC has dialog
		if (npcDialog.getText() != null && (lastNpcDialog == null || !lastNpcDialog.getText().equals(npcDialog.getText())))
		{
			lastNpcDialog = npcDialog;
			if (npcDialog.getName() != null)
			{
				lastPlayerDialog = null; //npc has dialog box now so safe to reset player dialog
				addDialogMessage(npcDialog.getName(), npcDialog.getText());
			}
		}

		//For when your player has dialog
		if (playerDialog.getText() != null && (lastPlayerDialog == null || !lastPlayerDialog.getText().equals(playerDialog.getText())))
		{
			lastPlayerDialog = playerDialog;
			if (playerDialog.getName() != null)
			{
				lastNpcDialog = null; //player has dialog box now so safe reset npc dialog
				addDialogMessage(playerDialog.getName(), playerDialog.getText());
			}
		}
	}

	private void addDialogMessage(String name, String message)
	{
		//boolean isChatboxTransparent = client.isResized() && client.getVar(Varbits.TRANSPARENT_CHATBOX) == 1;

		//Color nameColor = isChatboxTransparent ? chatColorConfig.transparentPlayerUsername() : chatColorConfig.opaquePlayerUsername();
		//Color messageColor = isChatboxTransparent ? chatColorConfig.transparentPublicChat() : chatColorConfig.opaquePublicChat();

		final ChatMessageBuilder chatMessage = new ChatMessageBuilder()
			.append(message);

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.PUBLICCHAT)
			.name(name)
			.runeLiteFormattedMessage(chatMessage.build())
			.build());
	}

	private Dialog getWidgetDialogSafely()
	{
		return getWidgetDialogSafely(WidgetInfo.DIALOG_NPC_TEXT.getGroupId(), WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());
	}

	private Dialog getWidgetDialogSafely(final int group, final int nameChild, final int textChild)
	{
		return new Dialog(client.getWidget(group, nameChild) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, nameChild).getText()), client.getWidget(group, textChild) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, textChild).getText()));
	}
}
