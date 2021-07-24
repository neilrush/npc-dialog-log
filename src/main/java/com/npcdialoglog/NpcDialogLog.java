package com.npcdialoglog;

import com.google.inject.Provides;
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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Npc Dialog Log",
	description = "Adds dialog between the player and NPCs to the chat as public chat.",
	tags = {"chat, quest, npc"}
)
public class NpcDialogLog extends Plugin
{
	@Inject
	NpcDialogLogConfig npcDialogLogConfig;
	@Inject
	private Client client;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private ChatColorConfig chatColorConfig;
	/**
	 * The actor that started dialog
	 */
	private Actor actor = null;

	/**
	 * The last dialog from the NPC
	 */
	private Dialog lastNpcDialog = null;

	/**
	 * The last dialog from the player
	 */
	private Dialog lastPlayerDialog = null;

	/**
	 * check for dialog every game tick
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (actor != null)
		{
			checkWidgetDialogs();
		}
	}

	/**
	 * Checks if the player has entered dialog with an npc
	 */
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

	/**
	 * Checks for the dialog widget and adds a message to chat if the dialog
	 * is from the player or an npc
	 */
	private void checkWidgetDialogs()
	{
		if (npcDialogLogConfig.displayNpcDialog())
		{
			final Dialog npcDialog = getWidgetDialogSafely();

			// Check if the NPC has dialog
			if (npcDialog.getText() != null && (lastNpcDialog == null || !lastNpcDialog.getText().equals(npcDialog.getText())))//check if this is a valid dialog box and it is not a duplicate
			{
				lastNpcDialog = npcDialog;
				if (npcDialog.getName() != null)
				{
					lastPlayerDialog = null; //npc has dialog box now so safe to reset player dialog
					addDialogMessage(npcDialog.getName(), npcDialog.getText());
				}
			}
		}

		if (npcDialogLogConfig.displayPlayerDialog())
		{
			final Dialog playerDialog = getWidgetDialogSafely(WidgetID.DIALOG_PLAYER_GROUP_ID, WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());//using the npc children id as they seem to be the same

			// Check if the player has dialog
			if (playerDialog.getText() != null && (lastPlayerDialog == null || !lastPlayerDialog.getText().equals(playerDialog.getText())))//check if this is a valid dialog box and it is not a duplicate
			{
				lastPlayerDialog = playerDialog;
				if (playerDialog.getName() != null)
				{
					lastNpcDialog = null; //player has dialog box now so safe reset npc dialog
					addDialogMessage(playerDialog.getName(), playerDialog.getText());
				}
			}
		}
	}

	/**
	 * Adds NPC/Player dialogue to chat as game message using the set public chat colors
	 *
	 * @param name    the name of the NPC/Player
	 * @param message the message to add to chat
	 */
	private void addDialogMessage(String name, String message)
	{

		final ChatMessageBuilder chatMessage = new ChatMessageBuilder()
			.append(getPublicChatUsernameColor(), name)
			.append(getPublicChatUsernameColor(), ": ")
			.append(getPublicChatMessageColor(), message);

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(chatMessage.build())
			.build());
	}

	/**
	 * Gets the color for usernames in public chat from chatColorConfig or default from {@code JagexColors}.
	 * <p>
	 * Takes the chatbox mode (opaque/transparent) into account.
	 *
	 * @return the current color of usernames in public chat
	 */
	private Color getPublicChatUsernameColor()
	{
		boolean isChatboxTransparent = client.isResized() && client.getVar(Varbits.TRANSPARENT_CHATBOX) == 1;
		Color usernameColor;

		if (isChatboxTransparent)
		{
			usernameColor = Color.WHITE; //default - is missing from JagexColors

			if (chatColorConfig.transparentPlayerUsername() != null)
			{
				usernameColor = chatColorConfig.transparentPlayerUsername();
			}
		}
		else
		{
			usernameColor = Color.BLACK; //default - is missing from JagexColors

			if (chatColorConfig.opaquePlayerUsername() != null)
			{
				usernameColor = chatColorConfig.opaquePlayerUsername();
			}
		}
		return usernameColor;
	}

	/**
	 * Gets the color for messages in public chat from chatColorConfig or default from {@code JagexColors}.
	 * <p>
	 * Takes the chatbox mode (opaque/transparent) into account.
	 *
	 * @return the current color of messages in public chat
	 */
	private Color getPublicChatMessageColor()
	{
		boolean isChatboxTransparent = client.isResized() && client.getVar(Varbits.TRANSPARENT_CHATBOX) == 1;
		Color messageColor;


		if (isChatboxTransparent)
		{
			messageColor = JagexColors.CHAT_PUBLIC_TEXT_TRANSPARENT_BACKGROUND;//default

			if (chatColorConfig.transparentPublicChat() != null)
			{
				messageColor = chatColorConfig.transparentPublicChat();
			}
		}
		else
		{

			messageColor = JagexColors.CHAT_PUBLIC_TEXT_OPAQUE_BACKGROUND;//default

			if (chatColorConfig.opaquePublicChat() != null)
			{
				messageColor = chatColorConfig.opaquePublicChat();
			}
		}
		return messageColor;
	}

	/**
	 * Gets sanitized dialog from npc dialog widget
	 *
	 * @return The NPC dialog
	 */
	private Dialog getWidgetDialogSafely()
	{
		return getWidgetDialogSafely(WidgetInfo.DIALOG_NPC_TEXT.getGroupId(), WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());
	}

	/**
	 * Gets sanitized dialog from a dialog widget
	 *
	 * @param group     The group id for the dialog widget
	 * @param nameChild The child id of the name in the dialog widget
	 * @param textChild The child id of the text/message in the dialog widget
	 * @return The sanitized dialog from the dialog widget
	 */
	private Dialog getWidgetDialogSafely(final int group, final int nameChild, final int textChild)
	{
		return new Dialog(client.getWidget(group, nameChild) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, nameChild).getText()), client.getWidget(group, textChild) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, textChild).getText()));
	}

	@Provides
	NpcDialogLogConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcDialogLogConfig.class);
	}
}
