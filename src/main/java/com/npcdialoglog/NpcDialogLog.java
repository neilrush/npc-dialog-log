package com.npcdialoglog;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ChatColorConfig;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

@Slf4j
@PluginDescriptor(
	name = "Npc Dialog Log",
	description = "Adds dialog from NPCs, the player and message boxes to the chat as public chat.",
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

	@Inject
	private ClientToolbar clientToolbar;

	private NpcDialogLogPanel panel;
	private NavigationButton navButton;

	/**
	 * The actor that started dialog
	 */
	private Actor actorInteractedWith = null;

	/**
	 * The npc names to ignore dialog from
	 */
	private List<String> ignoredNpcs = Collections.emptyList();

	@Override
	protected void startUp()
	{
		ignoredNpcs = Text.fromCSV(npcDialogLogConfig.ignoredNpcs());
		updatePanel();
	}

	/**
	 * Apply config changes
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(NpcDialogLogConfig.GROUP))
		{
			ignoredNpcs = Text.fromCSV(npcDialogLogConfig.ignoredNpcs());
			updatePanel();
		}
	}

	/**
	 * Add or remove the side panel to match the config
	 */
	private void updatePanel()
	{
		final boolean wanted = npcDialogLogConfig.dialogOutput().showsPanel();

		if (wanted && navButton == null)
		{
			panel = new NpcDialogLogPanel();

			final BufferedImage icon = ImageUtil.resizeImage(ImageUtil.loadImageResource(getClass(), "icon.png"), 16, 16);
			navButton = NavigationButton.builder()
				.tooltip("Npc Dialog Log")
				.icon(icon)
				.priority(10)
				.panel(panel)
				.build();

			clientToolbar.addNavigation(navButton);
		}
		else if (!wanted && navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
			panel = null;
		}
	}

	/**
	 * Adds dialog to the chat and/or the side panel
	 */
	private void logDialog(String name, String message)
	{
		final DialogOutput output = npcDialogLogConfig.dialogOutput();

		if (output.showsChat())
		{
			addDialogMessage(name, message);
		}

		if (output.showsPanel() && panel != null)
		{
			final NpcDialogLogPanel target = panel;
			SwingUtilities.invokeLater(() -> target.addEntry(name, message));
		}
	}

	private boolean isNpcIgnored(String name)
	{
		for (String pattern : ignoredNpcs)
		{
			if (WildcardMatcher.matches(pattern, name))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Expire overhead text every game tick
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
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
	 * Clear the message list if the user is not logged in
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		switch (gameStateChanged.getGameState())
		{
			case CONNECTION_LOST:
			case HOPPING:
			case LOGIN_SCREEN:
				lastMessageTickTime.clear();
				break;
			default:
				break;
		}
	}

	/**
	 * Handle dialog messages and the player clearing overhead text by chatting
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		switch (event.getType())
		{
			case DIALOG:
				onDialogMessage(event);
				break;
			case MESBOX:
				onMessageBoxMessage(event);
				break;
			case PUBLICCHAT:
				//for if the player clears the overhead text themselves by sending a public chat message
				if (event.getName().equals(client.getLocalPlayer().getName()) && client.getLocalPlayer().getOverheadText() != null)
				{
					if (lastMessageTickTime.remove(client.getLocalPlayer()) != null)
					{
						log.debug("Player sent message while dialog was being displayed. Cleared last dialog time.");
					}
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Adds dialog from a npc or player dialog box, sent as name|text
	 */
	private void onDialogMessage(ChatMessage event)
	{
		log.debug("DIALOG message: name='{}' sender='{}' message='{}'", event.getName(), event.getSender(), event.getMessage());

		final String raw = event.getMessage();
		final int separator = raw.indexOf('|');
		if (separator < 0)
		{
			log.debug("DIALOG message without name separator, ignoring");
			return;
		}

		final Dialog dialog = new Dialog(
			Text.sanitizeMultilineText(raw.substring(0, separator)),
			Text.sanitizeMultilineText(raw.substring(separator + 1)));

		if (dialog.getName().isEmpty() || dialog.getText().isEmpty())
		{
			return;
		}

		final boolean isPlayer = Text.sanitize(dialog.getName()).equals(Text.sanitize(client.getLocalPlayer().getName()));

		if (isPlayer)
		{
			if (npcDialogLogConfig.displayPlayerOverheadText())
			{
				lastMessageTickTime.put(client.getLocalPlayer(), client.getTickCount());
				client.getLocalPlayer().setOverheadText(dialog.getText());

				log.debug("Set overhead dialog for player to: " + dialog.getText());
			}

			if (npcDialogLogConfig.displayPlayerDialog())
			{
				logDialog(dialog.getName(), dialog.getText());

				log.debug("Added chat dialog: " + dialog.getName() + ": " + dialog.getText());
			}
		}
		else
		{
			if (isNpcIgnored(dialog.getName()))
			{
				log.debug("Ignored dialog from: " + dialog.getName());
				return;
			}

			if (npcDialogLogConfig.displayNpcOverheadText())
			{
				setNpcOverheadDialog(dialog);
			}

			if (npcDialogLogConfig.displayNpcDialog())
			{
				logDialog(dialog.getName(), dialog.getText());

				log.debug("Added chat dialog: " + dialog.getName() + ": " + dialog.getText());
			}
		}
	}

	/**
	 * Remember the npc the player is talking to
	 */
	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getTarget() == null || event.getSource() != client.getLocalPlayer())
		{
			return;
		}
		actorInteractedWith = event.getTarget();
	}

	/**
	 * Adds dialog from a message box, which has no speaker
	 */
	private void onMessageBoxMessage(ChatMessage event)
	{
		if (!npcDialogLogConfig.displayMessageBoxDialog())
		{
			return;
		}

		final String text = Text.sanitizeMultilineText(event.getMessage());
		if (text.isEmpty())
		{
			return;
		}

		logDialog(null, text);

		log.debug("Added message box dialog: " + text);
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
		if (actorInteractedWith == null || actorInteractedWith.getName() == null || !actorInteractedWith.getName().equals(npcDialog.getName()))
		{

			NPC foundActor = null;
			//look for npc that matches the name in the dialog
			for (NPC npc : client.getTopLevelWorldView().npcs())
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

				log.debug("Found matching actor: " + foundActor.getName() + " " + foundActor.getId());
				log.debug("Set overhead dialog for Npc: " + foundActor.getName() + " to: " + npcDialog.getText());
			}
			else if (actorInteractedWith != null)
			{
				lastMessageTickTime.put(actorInteractedWith, client.getTickCount());
				actorInteractedWith.setOverheadText(npcDialog.getText()); //fallback on setting overhead text on interaction npc

				log.debug("Unable to find matching actor. Fallback to using interaction npc: " + actorInteractedWith.getName());
				log.debug("Set overhead dialog for Npc: " + actorInteractedWith.getName() + " to: " + npcDialog.getText());
			}
			else
			{
				log.debug("Unable to find matching actor and no interaction npc to fall back on for: " + npcDialog.getName());
			}
		}
		else
		{
			lastMessageTickTime.put(actorInteractedWith, client.getTickCount());
			actorInteractedWith.setOverheadText(npcDialog.getText());

			log.debug("Set overhead dialog for Npc: " + actorInteractedWith.getName() + " to: " + npcDialog.getText());
		}
	}

	/**
	 * Adds NPC/Player dialogue to chat as a Console message using the set public chat colors
	 *
	 * @param name    the name of the NPC/Player, or {@code null} for dialog without a speaker
	 * @param message the message to add to chat
	 */
	private void addDialogMessage(String name, String message)
	{
		final ChatMessageBuilder chatMessage = new ChatMessageBuilder();

		if (name != null)
		{
			chatMessage.append(getPublicChatUsernameColor(), name)
				.append(getPublicChatUsernameColor(), ": ");
		}

		chatMessage.append(getPublicChatMessageColor(), message);

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
		boolean isChatboxTransparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;
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
		boolean isChatboxTransparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;
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

	@Provides
	NpcDialogLogConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcDialogLogConfig.class);
	}

	@Override
	protected void shutDown()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
			panel = null;
		}

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			//clear all overhead text
			for (Actor actor : lastMessageTickTime.keySet())
			{
				actor.setOverheadText(null);
			}
		}
	}
}
