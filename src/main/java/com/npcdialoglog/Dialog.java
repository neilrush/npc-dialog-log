package com.npcdialoglog;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a runescape dialog box with a {@code name} and {@code text}
 */
@Data
@AllArgsConstructor
class Dialog
{
	private final String name;
	private final String text;
}
