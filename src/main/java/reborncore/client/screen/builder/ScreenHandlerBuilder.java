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

package reborncore.client.screen.builder;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.screen.slot.Slot;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;
import reborncore.common.blockentity.MachineBaseBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ScreenHandlerBuilder {

	private final String name;

	private Predicate<PlayerEntity> canInteract = player -> true;

	final List<Slot> slots;
	final List<Range<Integer>> playerInventoryRanges, blockEntityInventoryRanges;

	final List<Pair<Supplier, Consumer>> objectValues;

	final List<Consumer<CraftingInventory>> craftEvents;

	public ScreenHandlerBuilder(final String name) {

		this.name = name;

		this.slots = new ArrayList<>();
		this.playerInventoryRanges = new ArrayList<>();
		this.blockEntityInventoryRanges = new ArrayList<>();

		this.objectValues = new ArrayList<>();

		this.craftEvents = new ArrayList<>();
	}

	public ScreenHandlerBuilder interact(final Predicate<PlayerEntity> canInteract) {
		this.canInteract = canInteract;
		return this;
	}

	public PlayerScreenHandlerBuilder player(final PlayerInventory player) {
		return new PlayerScreenHandlerBuilder(this, player);
	}

	public BlockEntityScreenHandlerBuilder blockEntity(final BlockEntity blockEntity) {
		return new BlockEntityScreenHandlerBuilder(this, blockEntity);
	}

	void addPlayerInventoryRange(final Range<Integer> range) {
		this.playerInventoryRanges.add(range);
	}

	void addBlockEnityInventoryRange(final Range<Integer> range) {
		this.blockEntityInventoryRanges.add(range);
	}

	@Deprecated
	/**
	 * The screen handler have to know if the blockEntity is still available (the block was not destroyed)
	 * and if the player is not to far from him to close the GUI if necessary
	 */
	public BuiltScreenHandler create(int syncID) {
		final BuiltScreenHandler built = new BuiltScreenHandler(syncID, this.name, this.canInteract,
				this.playerInventoryRanges,
				this.blockEntityInventoryRanges, null);
		if (!this.objectValues.isEmpty()) {
			built.addObjectSync(objectValues);
		}
		if (!this.craftEvents.isEmpty()) {
			built.addCraftEvents(this.craftEvents);
		}

		this.slots.forEach(built::addSlot);

		this.slots.clear();
		return built;
	}

	public BuiltScreenHandler create(final MachineBaseBlockEntity blockEntity, int syncID) {
		final BuiltScreenHandler built = new BuiltScreenHandler(syncID, this.name, this.canInteract,
				this.playerInventoryRanges,
				this.blockEntityInventoryRanges, blockEntity);
		if (!this.objectValues.isEmpty())
			built.addObjectSync(objectValues);
		if (!this.craftEvents.isEmpty()) {
			built.addCraftEvents(this.craftEvents);
		}

		this.slots.forEach(built::addSlot);

		this.slots.clear();
		return built;
	}
}
