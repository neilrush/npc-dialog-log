package com.npcdialoglog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel listing the dialog of the current session
 */
class NpcDialogLogPanel extends PluginPanel
{
	/**
	 * The number of entries kept before the oldest are dropped
	 */
	static final int MAX_ENTRIES = 500;

	private static final Color[] ENTRY_COLORS = {ColorScheme.DARKER_GRAY_COLOR, ColorScheme.DARK_GRAY_COLOR};

	private static final int COPIED_POPUP_MS = 750;

	private final JPanel entries = new JPanel();

	private Popup copiedPopup;

	private final Timer copiedPopupTimer = new Timer(COPIED_POPUP_MS, e -> hideCopiedPopup());

	/**
	 * The entry lines from the same speaker are added to
	 */
	private JPanel lastEntry;

	private String lastName;

	NpcDialogLogPanel()
	{
		copiedPopupTimer.setRepeats(false);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

		final JLabel title = new JLabel("Dialog Log");
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setFont(FontManager.getRunescapeBoldFont());
		header.add(title, BorderLayout.WEST);

		final JButton clear = new JButton("Clear");
		clear.setToolTipText("Clear the dialog log");
		clear.addActionListener(e -> clear());
		header.add(clear, BorderLayout.EAST);

		entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
		entries.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel body = new JPanel(new BorderLayout());
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.add(header, BorderLayout.NORTH);
		body.add(entries, BorderLayout.CENTER);

		add(body, BorderLayout.NORTH); //north so entries keep their preferred height
	}

	/**
	 * Adds a line of dialog to the log, grouped with the last entry if the speaker is the same
	 *
	 * @param name the name of the speaker or null if there is none
	 * @param text the dialog text
	 */
	void addEntry(String name, String text)
	{
		if (lastEntry != null && Objects.equals(lastName, name))
		{
			lastEntry.add(buildText(text, lastEntry.getBackground()));
		}
		else
		{
			lastEntry = buildEntry(name, text, ENTRY_COLORS[entries.getComponentCount() % ENTRY_COLORS.length]);
			lastName = name;
			entries.add(lastEntry);
		}

		if (entries.getComponentCount() > MAX_ENTRIES)
		{
			while (entries.getComponentCount() > MAX_ENTRIES)
			{
				entries.remove(0);
			}
			recolorEntries();
		}

		entries.revalidate();
		entries.repaint();

		SwingUtilities.invokeLater(() ->
		{
			entries.revalidate(); //wrapped text only knows its height after the first layout
			final JScrollBar bar = getScrollPane().getVerticalScrollBar();
			bar.setValue(bar.getMaximum());
		});
	}

	void clear()
	{
		lastEntry = null;
		lastName = null;
		entries.removeAll();
		entries.revalidate();
		entries.repaint();
	}

	/**
	 * Reapplies the alternating colors after entries are dropped
	 */
	private void recolorEntries()
	{
		for (int i = 0; i < entries.getComponentCount(); i++)
		{
			final Component entry = entries.getComponent(i);
			final Color color = ENTRY_COLORS[i % ENTRY_COLORS.length];
			entry.setBackground(color);
			if (entry instanceof JPanel)
			{
				for (Component child : ((JPanel) entry).getComponents())
				{
					if (child instanceof JTextArea)
					{
						child.setBackground(color);
					}
				}
			}
		}
	}

	private JPanel buildEntry(String name, String text, Color background)
	{
		final JPanel entry = new JPanel();
		entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
		entry.setBackground(background);
		entry.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		if (name != null)
		{
			final JLabel nameLabel = new JLabel(name);
			nameLabel.setFont(FontManager.getRunescapeBoldFont());
			nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
			nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			makeCopyable(nameLabel, name);
			entry.add(nameLabel);
		}

		entry.add(buildText(text, background));

		return entry;
	}

	private JTextArea buildText(String text, Color background)
	{
		final JTextArea textArea = new JTextArea(text);
		textArea.setFont(FontManager.getRunescapeSmallFont());
		textArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		textArea.setBackground(background);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		textArea.setFocusable(false);
		textArea.setHighlighter(null);
		textArea.setBorder(null);
		textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
		makeCopyable(textArea, text);
		return textArea;
	}

	/**
	 * Copies the text to the clipboard when the component is clicked
	 */
	private void makeCopyable(JComponent component, String text)
	{
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		component.setToolTipText("Click to copy");
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					copyToClipboard(text);
					showCopiedPopup(component, e.getPoint());
				}
			}
		});
	}

	private static void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	private void showCopiedPopup(Component owner, Point point)
	{
		hideCopiedPopup();

		final JLabel label = new JLabel("Copied");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.WHITE);
		label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE),
			BorderFactory.createEmptyBorder(2, 5, 2, 5)));

		final Point screen = new Point(point);
		SwingUtilities.convertPointToScreen(screen, owner);
		copiedPopup = PopupFactory.getSharedInstance().getPopup(owner, label, screen.x + 10, screen.y + 15);
		copiedPopup.show();
		copiedPopupTimer.restart();
	}

	private void hideCopiedPopup()
	{
		if (copiedPopup != null)
		{
			copiedPopup.hide();
			copiedPopup = null;
		}
	}
}
