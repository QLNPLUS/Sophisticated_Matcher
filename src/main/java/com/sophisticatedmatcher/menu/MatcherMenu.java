package com.sophisticatedmatcher.menu;

import com.sophisticatedmatcher.item.NbtMatcherItem;
import com.sophisticatedmatcher.registry.ModMenus;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MatcherMenu extends AbstractContainerMenu {
    private static final int PREVIEW_SLOT = 0;
    private static final int PLAYER_SLOT_START = 1;
    private static final int SAVE_BUTTON = 0;
    private static final int ENTRY_BUTTON_BASE = 100;

    private final SimpleContainer preview = new SimpleContainer(1);
    private int selectedIndex = -1;

    public MatcherMenu(int containerId, Inventory inventory) {
        super(ModMenus.MATCHER.get(), containerId);
        Player player = inventory.player;
        ItemStack matcher = findMatcher(player);
        if (matcher != null) {
            ItemStack savedPreview = MatcherData.preview(matcher);
            if (!savedPreview.isEmpty()) {
                preview.setItem(0, savedPreview.copyWithCount(1));
                selectedIndex = MatcherData.selectedIndex(matcher, savedPreview);
            }
        }

        addSlot(new PreviewSlot(preview, 0, 21, 20));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 21 + column * 18, 91 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 21 + column * 18, 149));
        }
    }

    public ItemStack previewStack() {
        return preview.getItem(0);
    }

    public java.util.List<MatcherData.ComponentEntry> entries() {
        return MatcherData.entries(previewStack());
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void selectIndexClient(int index) {
        selectedIndex = index;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == SAVE_BUTTON) {
            if (!(player instanceof ServerPlayer serverPlayer) || selectedIndex < 0 || previewStack().isEmpty()) {
                return false;
            }
            ItemStack matcher = findMatcher(serverPlayer);
            if (matcher == null) {
                return false;
            }
            MatcherData.save(matcher, previewStack(), selectedIndex);
            serverPlayer.getInventory().setChanged();
            return true;
        }
        if (id >= ENTRY_BUTTON_BASE) {
            int index = id - ENTRY_BUTTON_BASE;
            if (index >= 0 && index < entries().size()) {
                selectedIndex = index;
                return true;
            }
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == PREVIEW_SLOT && clickType == ClickType.PICKUP) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty() && !NbtMatcherItem.isMatcher(carried)) {
                preview.setItem(0, carried.copyWithCount(1));
                selectedIndex = -1;
                broadcastChanges();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == PREVIEW_SLOT) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < PLAYER_SLOT_START + 36) {
            if (!moveItemStackTo(source, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, PLAYER_SLOT_START, PLAYER_SLOT_START + 36, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    private static ItemStack findMatcher(Player player) {
        if (NbtMatcherItem.isMatcher(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        if (NbtMatcherItem.isMatcher(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        return null;
    }

    private static final class PreviewSlot extends Slot {
        public PreviewSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.isEmpty() && !NbtMatcherItem.isMatcher(stack);
        }
    }
}
