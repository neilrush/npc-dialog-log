package com.npcdialoglog;

/**
 * Where logged dialog is sent
 */
public enum DialogOutput
{
	CHAT,
	SIDE_PANEL,
	BOTH;

	public boolean showsChat()
	{
		return this == CHAT || this == BOTH;
	}

	public boolean showsPanel()
	{
		return this == SIDE_PANEL || this == BOTH;
	}
}
