/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package reborncore.client.gui.builder.widget.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class ToolTipLine {

	private Text line;
	private int color;
	private boolean shadowed;

	public ToolTipLine(Text line, int color, boolean shadowed) {
		this.line = line;
		this.color = color;
		this.shadowed = shadowed;
	}

	public ToolTipLine(Text line, int color) {
		this(line, color, false);
	}

	public ToolTipLine(Text line, boolean shadowed) {
		this(line, 0xFFFFFF, shadowed);
	}

	public ToolTipLine(Text line) {
		this(line, 0xFFFFFF, false);
	}

	public ToolTipLine() {
		this(LiteralText.EMPTY);
	}

	public Text getLine() {
		return line;
	}

	public void setLine(Text line) {
		this.line = line;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public boolean isShadowed() {
		return shadowed;
	}

	public void setShadowed(boolean shadowed) {
		this.shadowed = shadowed;
	}

	public int getWidth(TextRenderer fontRenderer) {
		return fontRenderer.getWidth(getLine());
	}

	public void draw(MatrixStack matrixStack, TextRenderer fontRenderer, int x, int y) {
		if (!isShadowed()) {
			fontRenderer.draw(matrixStack, getLine(), x, y, color);
		} else {
			fontRenderer.draw(matrixStack, getLine(), x, y, color);
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ToolTipLine that = (ToolTipLine) o;
		return color == that.color &&
				shadowed == that.shadowed &&
				line.equals(that.line);

	}

	@Override
	public int hashCode() {
		int result = line.hashCode();
		result = 31 * result + color;
		result = 31 * result + (shadowed ? 1 : 0);
		return result;
	}

}
