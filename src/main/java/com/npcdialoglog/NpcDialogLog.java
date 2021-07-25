package com.npcdialoglog;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
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
	/**
	 * The number of ticks overhead text should last for.
	 */
	private final int TIMEOUT_TICKS = 5;
	/**
	 * The map of the last time an actor had overhead text set
	 */
	private final Map<Actor, Integer> lastMessageTickTime = new HashMap<>();
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
	private Actor actorInteractedWith = null;
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
		if (actorInteractedWith != null)
		{
			checkWidgetDialogs();
		}

		for (Iterator<Actor> iterator = lastMessageTickTime.keySet().iterator(); iterator.hasNext(); )
		{
			Actor actor = iterator.next();
			if (client.getTickCount() - lastMessageTickTime.get(actor) > TIMEOUT_TICKS)
			{
				actor.setOverheadText(null);
				iterator.remove();
			}
		}
	}

	/**
	 * Check if the player has cleared the dialog by sending another message
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		//for if the player clears the overhead text themselves by sending a public chat message
		if (client.getLocalPlayer() != null && event.getType() == ChatMessageType.PUBLICCHAT && event.getName().equals(client.getLocalPlayer().getName()))
		{
			if (client.getLocalPlayer().getOverheadText() != null)
			{
				lastMessageTickTime.remove(client.getLocalPlayer());
			}
		}
	}

	/**
	 * Check if the player has entered dialog with an npc
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
		actorInteractedWith = event.getTarget();
	}

	/**
	 * Checks for the dialog widget and adds a
	 * message to chat if the dialog
	 * is from the player or an npc
	 */
	private void checkWidgetDialogs()
	{
		if (npcDialogLogConfig.displayNpcDialog() || npcDialogLogConfig.displayNpcOverheadText())
		{
			final Dialog npcDialog = getWidgetDialogSafely();

			// Check if the NPC has dialog
			if (npcDialog.getText() != null && (lastNpcDialog == null || !lastNpcDialog.getText().equals(npcDialog.getText())))//check if this is a valid dialog box and it is not a duplicate
			{
				lastNpcDialog = npcDialog;
				if (npcDialog.getName() != null)
				{
					if(npcDialogLogConfig.displayNpcOverheadText())
					{
						setNpcOverheadDialog(npcDialog);
					}

					lastPlayerDialog = null; //npc has dialog box now so safe to reset player dialog
					if (npcDialogLogConfig.displayNpcDialog())
					{
						addDialogMessage(npcDialog.getName(), npcDialog.getText());
					}
				}
			}
		}

		if (npcDialogLogConfig.displayPlayerDialog() || npcDialogLogConfig.displayPlayerOverheadText())
		{
			final Dialog playerDialog = getWidgetDialogSafely(WidgetID.DIALOG_PLAYER_GROUP_ID, WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());//using the npc children id as they seem to be the same

			// Check if the player has dialog
			if (playerDialog.getText() != null && (lastPlayerDialog == null || !lastPlayerDialog.getText().equals(playerDialog.getText())))//check if this is a valid dialog box and it is not a duplicate
			{
				lastPlayerDialog = playerDialog;
				if (playerDialog.getName() != null)
				{
					if (client.getLocalPlayer() != null && npcDialogLogConfig.displayPlayerOverheadText())
					{
						lastMessageTickTime.put(client.getLocalPlayer(), client.getTickCount());
						client.getLocalPlayer().setOverheadText(playerDialog.getText());
					}

					lastNpcDialog = null; //player has dialog box now so safe reset npc dialog
					if(npcDialogLogConfig.displayPlayerDialog())
					{
						addDialogMessage(playerDialog.getName(), playerDialog.getText());
					}
				}
			}
		}
	}

	/**
	 * Sets the overhead dialogue of the npc with the name in {@code Dialog}.
	 * Defaults to the current npc the player is talking to.
	 * If the current npc doesn't match the closest match is used.
	 *
	 * @param npcDialog The dialog to put overhead
	 */
	private void setNpcOverheadDialog(Dialog npcDialog)
	{
		if (actorInteractedWith.getName() == null || !actorInteractedWith.getName().equals(npcDialog.getName()))
		{

/*			Actor foundActor = Arrays.stream(client.getCachedNPCs())
				.filter(cachedNpc -> Text.sanitizeMultilineText(cachedNpc.getName()).equals(npcDialog.getName()))
				.findFirst()
				.orElse(null);*/

			Actor foundActor = null;
			//look for npc that matches the name in the dialog
			client.getCachedNPCs();
			client.getNpcs();
			for (Actor npc : client.getNpcs())
			{
				if (npc.getName() != null && Text.sanitizeMultilineText(npc.getName()).equals(npcDialog.getName()))
				{
					foundActor = npc;
					break;
				}
			}
			if (foundActor != null)
			{
				lastMessageTickTime.put(foundActor, client.getTickCount());
				foundActor.setOverheadText(npcDialog.getText());
			}
			else
			{
				lastMessageTickTime.put(actorInteractedWith, client.getTickCount());
				actorInteractedWith.setOverheadText(npcDialog.getText()); //fallback on setting overhead text on interaction npc
			}
		}
		else
		{
			lastMessageTickTime.put(actorInteractedWith, client.getTickCount());
			actorInteractedWith.setOverheadText(npcDialog.getText());
		}
	}

	/**
	 * Adds NPC/Player dialogue to chat as a Console message using the set public chat colors
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
			.type(ChatMessageType.CONSOLE)
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
