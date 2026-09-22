package com.sophisticatedmatcher.menu;

import com.sophisticatedmatcher.item.MultiMatcherItem;
import com.sophisticatedmatcher.item.NbtMatcherItem;
import com.sophisticatedmatcher.registry.ModMenus;
import com.sophisticatedmatcher.util.MatcherData;
import com.sophisticatedmatcher.util.MultiMatcherData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot-based editor for the multi matcher. Storage slots are ghost slots, exactly like the
 * single matcher's preview slot: placing a configured matcher only reads its rule (the
 * matcher item is never consumed), clicking an occupied slot with an empty cursor clears
 * the rule, and the slots cannot be extracted from. The held multi matcher item mirrors
 * the storage on every change; only rules are persisted, never matcher items.
 */
public final class MultiMatcherMenu extends AbstractContainerMenu {
    public static final int SLOT_COUNT = MultiMatcherData.SLOT_COUNT;
    private static final int SWITCH_BUTTON_BASE = 300;
    private static final int PLAYER_SLOT_START = SLOT_COUNT;

    private final SimpleContainer storage = new SimpleContainer(SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (player != null && !player.level().isClientSide) {
                persistToHeldMatcher();
            }
        }
    };
    private final Player player;

    public MultiMatcherMenu(int containerId, Inventory inventory) {
        super(ModMenus.MULTI.get(), containerId);
        this.player = inventory.player;
        for (int i = 0; i < SLOT_COUNT; i++) {
            addSlot(new StoredSlot(i, 7 + i * 18, 20));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        loadFromHeldMatcher();
    }

    public ItemStack storedStack(int slot) {
        return storage.getItem(slot);
    }

    public MultiMatcherData.Join joinAt(int slot) {
        return MultiMatcherData.stackJoin(storage.getItem(slot));
    }

    /** Sets the join state on both sides; the server pass persists through setChanged. */
    public void setJoin(int slot, MultiMatcherData.Join join) {
        if (slot < 0 || slot >= SLOT_COUNT || join == null) {
            return;
        }
        ItemStack stack = storage.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        MultiMatcherData.setStackJoin(stack, join);
        storage.setChanged();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= SWITCH_BUTTON_BASE && id < SWITCH_BUTTON_BASE + SLOT_COUNT * 3) {
            int code = id - SWITCH_BUTTON_BASE;
            setJoin(code / 3, MultiMatcherData.Join.values()[code % 3]);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < SLOT_COUNT) {
            // Ghost storage: never route through vanilla item moving, the cursor is untouched.
            handleStorageClick(slotId, button, clickType);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void handleStorageClick(int slotId, int button, ClickType clickType) {
        if (clickType != ClickType.PICKUP) {
            return;
        }
        ItemStack carried = getCarried();
        if (isConfiguredMatcher(carried)) {
            // Read the rule only; the carried matcher stays with the player.
            installDisplay(slotId, carried);
        } else if (button == 0 && carried.isEmpty() && !storage.getItem(slotId).isEmpty()) {
            storage.setItem(slotId, ItemStack.EMPTY);
        }
    }

    private void installDisplay(int slot, ItemStack configuredMatcher) {
        ItemStack display = MatcherData.matcherStack(MatcherData.selectedRule(configuredMatcher));
        MultiMatcherData.setStackJoin(display, MultiMatcherData.stackJoin(storage.getItem(slot)));
        storage.setItem(slot, display);
    }

    public static boolean isConfiguredMatcher(ItemStack stack) {
        return NbtMatcherItem.isMatcher(stack) && MatcherData.selectedRule(stack) != null;
    }

    private void loadFromHeldMatcher() {
        ItemStack held = findMultiMatcher(player);
        if (held == null) {
            return;
        }
        List<MultiMatcherData.Slot> slots = MultiMatcherData.slots(held);
        for (int i = 0; i < SLOT_COUNT && i < slots.size(); i++) {
            MultiMatcherData.Slot slot = slots.get(i);
            if (!slot.occupied()) {
                continue;
            }
            ItemStack display = MatcherData.matcherStack(slot.rule());
            MultiMatcherData.setStackJoin(display, slot.join());
            storage.setItem(i, display);
        }
    }

    private void persistToHeldMatcher() {
        ItemStack held = findMultiMatcher(player);
        if (held == null) {
            return;
        }
        List<MultiMatcherData.Join> joins = new ArrayList<>(SLOT_COUNT);
        List<MatcherData.Rule> rules = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = storage.getItem(i);
            joins.add(MultiMatcherData.stackJoin(stack));
            rules.add(MatcherData.selectedRule(stack));
        }
        MultiMatcherData.write(held, joins, rules);
        player.getInventory().setChanged();
    }

    public static ItemStack findMultiMatcher(Player player) {
        if (MultiMatcherItem.isMatcher(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        if (MultiMatcherItem.isMatcher(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < PLAYER_SLOT_START || index >= slots.size()) {
            // Storage slots are ghost: never shift-clicked out of the storage.
            return ItemStack.EMPTY;
        }
        ItemStack source = slots.get(index).getItem();
        if (!isConfiguredMatcher(source)) {
            return ItemStack.EMPTY;
        }
        int target = firstEmptyStorageSlot();
        if (target < 0) {
            return ItemStack.EMPTY;
        }
        // Ghost install: read the rule, keep the matcher in the inventory.
        installDisplay(target, source);
        return ItemStack.EMPTY;
    }

    private int firstEmptyStorageSlot() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (storage.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    private final class StoredSlot extends Slot {
        private StoredSlot(int index, int x, int y) {
            super(storage, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isConfiguredMatcher(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            // Ghost slot: contents are rules, nothing to extract.
            return false;
        }
    }
}
