package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.network.MatcherNetwork;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.NumericTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MatcherEditorScreen extends AbstractContainerScreen<MatcherMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_background.png");
    private static final ResourceLocation VANILLA_INVENTORY = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/inventory.png");
    private static final ResourceLocation BUTTON = ResourceLocation.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_button.png");
    private static final ResourceLocation BUTTON_HOVER = ResourceLocation.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_button_hover.png");
    private static final ResourceLocation BUTTON_PRESSED = ResourceLocation.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_button_pressed.png");
    private static final ResourceLocation BUTTON_DISABLED = ResourceLocation.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_button_disabled.png");
    private static final ResourceLocation VERTICAL_SCROLL_KNOB = ResourceLocation.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/scroll_vertical_knob.png");
    private static final ResourceLocation HORIZONTAL_SCROLL_KNOB = ResourceLocation.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/scroll_horizontal_knob.png");
    private static final int GUI_WIDTH = 360;
    private static final int GUI_HEIGHT = 266;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 340;
    private static final int LEFT_PANEL_X = 18;
    private static final int TREE_WIDTH = 220;
    private static final int SCROLLBAR_SIZE = 8;
    private static final int SCROLLBAR_TEXTURE_SIZE = 8;
    private static final int RIGHT_PANEL_X = 250;
    private static final int RIGHT_PANEL_WIDTH = 90;
    private static final int TREE_X = LEFT_PANEL_X;
    private static final int TREE_LIST_Y = 37;
    private static final int ROW_HEIGHT = 12;
    private static final int TREE_VIEW_X = TREE_X + 3;
    private static final int TREE_VIEW_WIDTH = TREE_WIDTH - SCROLLBAR_SIZE - 5;
    private static final int TREE_VIEW_HEIGHT = 144;
    private static final int MAX_VISIBLE_ROWS = TREE_VIEW_HEIGHT / ROW_HEIGHT;
    private static final int RULE_X = RIGHT_PANEL_X;
    private static final int CONTROL_WIDTH = RIGHT_PANEL_WIDTH;
    private static final int RANGE_FIELD_GAP = 4;
    private static final int RANGE_FIELD_WIDTH = (CONTROL_WIDTH - RANGE_FIELD_GAP) / 2;
    private static final int BOUNDS_BUTTON_WIDTH = 28;
    private static final int MODE_BUTTON_WIDTH = CONTROL_WIDTH - BOUNDS_BUTTON_WIDTH - 4;
    private static final int INVENTORY_X = (GUI_WIDTH - 162) / 2;
    private static final int PREVIEW_SLOT_X = 357;
    private static final int PREVIEW_SLOT_Y = 92;
    private static final int MAIN_INVENTORY_Y = 210;
    private static final int HOTBAR_Y = 268;
    private static final int MODE_BUTTON_Y = 62;
    private static final int VALUE_BOX_Y = 89;
    private static final int BOUNDS_BUTTON_Y = MODE_BUTTON_Y;
    private static final int NORMAL_STATUS_Y = VALUE_BOX_Y + 18;
    private static final int BETWEEN_STATUS_Y = NORMAL_STATUS_Y;
    private static final int ACTION_BUTTON_Y = 155;
    private static final int INVENTORY_TEXTURE_SIZE = 256;
    private static final int INVENTORY_SLOT_OFFSET = 1;

    private final Inventory playerInventory;
    private final Set<String> expanded = new HashSet<>();
    private List<MatcherData.TreeNode> roots = List.of();
    private List<MatcherData.TreeNode> visibleNodes = List.of();
    private MatcherData.TreeNode selectedNode;
    private MatcherData.Operator operator = MatcherData.Operator.EQUALS;
    private boolean minInclusive = true;
    private boolean maxInclusive = true;
    private int scrollOffset;
    private int horizontalScrollOffset;
    private boolean draggingVerticalScrollbar;
    private boolean draggingHorizontalScrollbar;
    private int scrollbarDragOffset;
    private EditBox valueBox;
    private EditBox minBox;
    private EditBox maxBox;
    private MatcherButton modeButton;
    private MatcherButton boundsButton;
    private MatcherButton saveButton;
    private String errorMessage = "";

    public MatcherEditorScreen(MatcherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        playerInventory = inventory;
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        MatcherLayoutDebug.beginEditorScreen();
        applySlotLayout();
        refreshTree();

        valueBox = createEditBox("", CONTROL_WIDTH);
        minBox = createEditBox("", RANGE_FIELD_WIDTH);
        maxBox = createEditBox("", RANGE_FIELD_WIDTH);
        addRenderableWidget(valueBox);
        addRenderableWidget(minBox);
        addRenderableWidget(maxBox);
        selectInitialNode();

        modeButton = editorButton(RULE_X, MODE_BUTTON_Y, CONTROL_WIDTH,
                Component.literal(""), button -> cycleOperator());
        boundsButton = editorButton(RULE_X + CONTROL_WIDTH - BOUNDS_BUTTON_WIDTH,
                BOUNDS_BUTTON_Y, BOUNDS_BUTTON_WIDTH,
                Component.literal(""), button -> cycleBounds());
        saveButton = editorButton(RULE_X, actionButtonY(), CONTROL_WIDTH,
                Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".save_rule"),
                button -> saveRule());
        addRenderableWidget(modeButton);
        addRenderableWidget(boundsButton);
        addRenderableWidget(saveButton);
        updateEditorControls();
    }

    private EditBox createEditBox(String value, int width) {
        EditBox box = new EditBox(font, leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_BOX_Y), width, 16, Component.empty());
        box.setMaxLength(64);
        box.setValue(value);
        box.setTextColor(0xFFFFFF);
        box.setTextColorUneditable(0xFFFFFF);
        return box;
    }

    private MatcherButton editorButton(int x, int y, int width, Component message, ButtonPress press) {
        return new MatcherButton(leftPos + x, topPos + y, width, message, press::press,
                BUTTON, BUTTON_HOVER, BUTTON_PRESSED, BUTTON_DISABLED, 96, 16);
    }

    private void applySlotLayout() {
        if (menu.slots.size() < 37) return;
        setSlotPosition(menu.slots.get(0),
                layoutX(MatcherLayoutDebug.Widget.PREVIEW_SLOT, PREVIEW_SLOT_X),
                layoutY(MatcherLayoutDebug.Widget.PREVIEW_SLOT, PREVIEW_SLOT_Y));
        for (int index = 1; index <= 27; index++) {
            int column = (index - 1) % 9;
            int row = (index - 1) / 9;
            setSlotPosition(menu.slots.get(index),
                    layoutX(MatcherLayoutDebug.Widget.INVENTORY, INVENTORY_X) + column * 18,
                    layoutY(MatcherLayoutDebug.Widget.INVENTORY, MAIN_INVENTORY_Y) + row * 18);
        }
        for (int index = 28; index < 37; index++) {
            setSlotPosition(menu.slots.get(index),
                    layoutX(MatcherLayoutDebug.Widget.HOTBAR, INVENTORY_X) + (index - 28) * 18,
                    layoutY(MatcherLayoutDebug.Widget.HOTBAR, HOTBAR_Y));
        }
    }

    private void setSlotPosition(net.minecraft.world.inventory.Slot slot, int x, int y) {
        if (slot instanceof MatcherSlotPositionAccess access) {
            access.sophisticatedMatcher$setPosition(x, y);
        }
    }

    private void refreshTree() {
        roots = MatcherData.tree(menu.previewStack());
        visibleNodes = MatcherData.visibleNodes(roots, expanded);
        scrollOffset = Math.min(scrollOffset, maxScrollOffset());
        horizontalScrollOffset = Math.min(horizontalScrollOffset, maxHorizontalScrollOffset());
        if (selectedNode != null) {
            selectedNode = findNode(selectedNode.pathKey());
        }
    }

    private void selectInitialNode() {
        if (selectedNode == null) {
            selectedNode = firstLeaf(roots);
        }
        if (selectedNode != null) {
            expandAncestors(selectedNode.path());
            refreshTree();
            loadNodeValues();
        }
    }

    private void expandAncestors(List<MatcherData.PathSegment> path) {
        for (int length = 1; length < path.size(); length++) {
            expanded.add(MatcherData.pathKey(path.subList(0, length)));
        }
    }

    private MatcherData.TreeNode firstLeaf(List<MatcherData.TreeNode> nodes) {
        for (MatcherData.TreeNode node : nodes) {
            MatcherData.TreeNode leaf = node.branch() ? firstLeaf(node.children()) : node;
            if (leaf != null) return leaf;
        }
        return null;
    }

    private MatcherData.TreeNode findNode(String pathKey) {
        for (MatcherData.TreeNode node : flatten(roots)) {
            if (node.pathKey().equals(pathKey)) return node;
        }
        return null;
    }

    private List<MatcherData.TreeNode> flatten(List<MatcherData.TreeNode> nodes) {
        java.util.ArrayList<MatcherData.TreeNode> result = new java.util.ArrayList<>();
        for (MatcherData.TreeNode node : nodes) {
            result.add(node);
            if (node.branch()) result.addAll(flatten(node.children()));
        }
        return result;
    }

    private void loadNodeValues() {
        if (selectedNode == null) return;
        operator = MatcherData.Operator.EQUALS;
        minInclusive = true;
        maxInclusive = true;
        String sample = MatcherData.valueText(selectedNode.value());
        valueBox.setValue(sample);
        minBox.setValue(sample);
        maxBox.setValue(sample);
        valueBox.setCursorPosition(0);
        minBox.setCursorPosition(0);
        maxBox.setCursorPosition(0);
        errorMessage = "";
    }

    private void cycleOperator() {
        MatcherData.Operator[] options = operatorOptions();
        int next = (indexOf(options, operator) + 1) % options.length;
        operator = options[next];
        errorMessage = "";
        updateEditorControls();
    }

    private void cycleBounds() {
        if (minInclusive && maxInclusive) {
            minInclusive = false;
        } else if (!minInclusive && maxInclusive) {
            maxInclusive = false;
        } else if (!minInclusive) {
            minInclusive = true;
        } else {
            maxInclusive = true;
        }
        updateEditorControls();
    }

    private MatcherData.Operator[] operatorOptions() {
        if (selectedNode == null) return new MatcherData.Operator[]{MatcherData.Operator.EQUALS};
        if (selectedNode.value() instanceof NumericTag) {
            return new MatcherData.Operator[]{MatcherData.Operator.EQUALS, MatcherData.Operator.NOT_EQUALS,
                    MatcherData.Operator.GREATER_THAN, MatcherData.Operator.GREATER_THAN_OR_EQUAL,
                    MatcherData.Operator.LESS_THAN, MatcherData.Operator.LESS_THAN_OR_EQUAL,
                    MatcherData.Operator.BETWEEN, MatcherData.Operator.EXISTS, MatcherData.Operator.NOT_EXISTS};
        }
        return new MatcherData.Operator[]{MatcherData.Operator.EQUALS, MatcherData.Operator.NOT_EQUALS,
                MatcherData.Operator.EXISTS, MatcherData.Operator.NOT_EXISTS};
    }

    private int indexOf(MatcherData.Operator[] options, MatcherData.Operator value) {
        for (int i = 0; i < options.length; i++) if (options[i] == value) return i;
        return 0;
    }

    private void updateEditorControls() {
        if (modeButton == null || valueBox == null) return;
        modeButton.setMessage(Component.literal(MatcherData.Operator.BETWEEN == operator
                ? "范围" : operator.symbol()));
        boolean existsMode = operator == MatcherData.Operator.EXISTS || operator == MatcherData.Operator.NOT_EXISTS;
        boolean between = operator == MatcherData.Operator.BETWEEN;
        valueBox.visible = !existsMode && !between;
        minBox.visible = between;
        maxBox.visible = between;
        boundsButton.visible = between;
        modeButton.setWidth(between ? MODE_BUTTON_WIDTH : CONTROL_WIDTH);
        boundsButton.setWidth(BOUNDS_BUTTON_WIDTH);
        valueBox.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_BOX_Y));
        minBox.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_BOX_Y));
        maxBox.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN,
                        RULE_X + RANGE_FIELD_WIDTH + RANGE_FIELD_GAP),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_BOX_Y));
        modeButton.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, MODE_BUTTON_Y));
        boundsButton.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN,
                        RULE_X + CONTROL_WIDTH - BOUNDS_BUTTON_WIDTH),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, BOUNDS_BUTTON_Y));
        saveButton.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.SAVE_BUTTON, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.SAVE_BUTTON, actionButtonY()));
        valueBox.setEditable(selectedNode != null && selectedNode.value() instanceof NumericTag);
        saveButton.active = selectedNode != null && !menu.previewStack().isEmpty();
        if (operator == MatcherData.Operator.EXISTS || operator == MatcherData.Operator.NOT_EXISTS) {
            boundsButton.setMessage(Component.literal(""));
        } else {
            String left = minInclusive ? "[" : "(";
            String right = maxInclusive ? "]" : ")";
            boundsButton.setMessage(Component.literal(left + right));
        }
    }

    private void saveRule() {
        if (selectedNode == null) return;
        MatcherData.Rule rule;
        boolean numeric = selectedNode.value() instanceof NumericTag;
        if (operator == MatcherData.Operator.EXISTS || operator == MatcherData.Operator.NOT_EXISTS) {
            rule = MatcherData.Rule.exists(selectedNode.path(), operator == MatcherData.Operator.EXISTS);
        } else if (operator == MatcherData.Operator.BETWEEN) {
            if (!numeric) {
                errorMessage = "需要数值";
                return;
            }
            net.minecraft.nbt.Tag min = MatcherData.parseNumeric(minBox.getValue(), selectedNode.value());
            net.minecraft.nbt.Tag max = MatcherData.parseNumeric(maxBox.getValue(), selectedNode.value());
            if (min == null || max == null) {
                errorMessage = "数值无效";
                return;
            }
            rule = new MatcherData.Rule(selectedNode.path(), operator, null, min, max, minInclusive, maxInclusive);
            if (!MatcherData.validRange(rule)) {
                errorMessage = "范围无效";
                return;
            }
        } else if (numeric) {
            net.minecraft.nbt.Tag value = MatcherData.parseNumeric(valueBox.getValue(), selectedNode.value());
            if (value == null) {
                errorMessage = "数值无效";
                return;
            }
            rule = new MatcherData.Rule(selectedNode.path(), operator, value, null, null, true, true);
        } else {
            rule = new MatcherData.Rule(selectedNode.path(), operator, selectedNode.value(), null, null, true, true);
        }
        MatcherNetwork.sendSaveRule(menu.containerId, rule);
        errorMessage = "";
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT,
                GUI_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
        graphics.blit(VANILLA_INVENTORY,
                leftPos + layoutX(MatcherLayoutDebug.Widget.INVENTORY, INVENTORY_X) - INVENTORY_SLOT_OFFSET,
                topPos + layoutY(MatcherLayoutDebug.Widget.INVENTORY, MAIN_INVENTORY_Y) - INVENTORY_SLOT_OFFSET,
                7, 83, 162, 54, INVENTORY_TEXTURE_SIZE, INVENTORY_TEXTURE_SIZE);
        graphics.blit(VANILLA_INVENTORY,
                leftPos + layoutX(MatcherLayoutDebug.Widget.HOTBAR, INVENTORY_X) - INVENTORY_SLOT_OFFSET,
                topPos + layoutY(MatcherLayoutDebug.Widget.HOTBAR, HOTBAR_Y) - INVENTORY_SLOT_OFFSET,
                7, 141, 162, 18, INVENTORY_TEXTURE_SIZE, INVENTORY_TEXTURE_SIZE);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".editor"),
                layoutX(MatcherLayoutDebug.Widget.TITLE, 8),
                layoutY(MatcherLayoutDebug.Widget.TITLE, 6), 0xFF404040, false);
        if (!errorMessage.isEmpty()) {
            graphics.drawString(font, errorMessage, layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                    layoutY(MatcherLayoutDebug.Widget.DROPDOWN, errorMessageY()), 0xFFFF5555, false);
        }
    }

    private void renderTree(GuiGraphics graphics) {
        int treeX = treePanelX();
        int treeY = treeListY();
        int viewX = treeViewX();
        int viewY = treeY;
        int viewRight = viewX + TREE_VIEW_WIDTH;
        int viewBottom = viewY + TREE_VIEW_HEIGHT;
        if (visibleNodes.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".no_nbt"),
                    viewX, viewY, 0xFFAAAAAA, false);
            return;
        }
        int rows = visibleRows();
        graphics.enableScissor(viewX, viewY, viewRight, viewBottom);
        for (int row = 0; row < rows; row++) {
            MatcherData.TreeNode node = visibleNodes.get(scrollOffset + row);
            int y = treeY + row * ROW_HEIGHT;
            int x = treeX + 8 + Math.min(7, node.path().size() - 1) * 7 - horizontalScrollOffset;
            if (node.branch()) {
                graphics.drawString(font, expanded.contains(node.pathKey()) ? "-" : "+", x, y, 0xFFDDDDDD, false);
            }
            String marker = selectedNode != null && selectedNode.pathKey().equals(node.pathKey()) ? "> " : "  ";
            String text = marker + (node.branch() ? node.label() : node.label() + " = " + node.valueText());
            graphics.drawString(font, text, x + 8, y, 0xFFFFFFFF, false);
        }
        graphics.disableScissor();
        renderTreeScrollbars(graphics);
    }

    private void renderTreeScrollbars(GuiGraphics graphics) {
        if (hasVerticalOverflow()) {
            int knobHeight = verticalKnobHeight();
            blitVerticalKnob(graphics, VERTICAL_SCROLL_KNOB, verticalScrollbarX(), verticalKnobY(), knobHeight);
        }
        if (hasHorizontalOverflow()) {
            int knobWidth = horizontalKnobWidth();
            blitHorizontalKnob(graphics, HORIZONTAL_SCROLL_KNOB, horizontalKnobX(),
                    horizontalScrollbarY(), knobWidth);
        }
    }

    private void blitVerticalKnob(GuiGraphics graphics, ResourceLocation texture, int x, int y, int height) {
        graphics.blit(texture, x, y, SCROLLBAR_SIZE, 2,
                0.0F, 0.0F, SCROLLBAR_TEXTURE_SIZE, 2, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
        graphics.blit(texture, x, y + 2, SCROLLBAR_SIZE, height - 4,
                0.0F, 4.0F, SCROLLBAR_TEXTURE_SIZE, 1, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
        graphics.blit(texture, x, y + height - 2, SCROLLBAR_SIZE, 2,
                0.0F, 6.0F, SCROLLBAR_TEXTURE_SIZE, 2, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
    }

    private void blitHorizontalKnob(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width) {
        graphics.blit(texture, x, y, 2, SCROLLBAR_SIZE,
                0.0F, 0.0F, 2, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
        graphics.blit(texture, x + 2, y, width - 4, SCROLLBAR_SIZE,
                4.0F, 0.0F, 1, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
        graphics.blit(texture, x + width - 2, y, 2, SCROLLBAR_SIZE,
                6.0F, 0.0F, 2, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applySlotLayout();
        refreshTree();
        updateEditorControls();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTree(graphics);
        renderTooltip(graphics, mouseX, mouseY);
        renderDebugOverlay(graphics);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MatcherLayoutDebug.keyPressed(keyCode, hasShiftDown(), hasAltDown())) {
            applySlotLayout();
            updateEditorControls();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderDebugOverlay(GuiGraphics graphics) {
        if (!MatcherLayoutDebug.isEnabled()) {
            return;
        }
        DebugBounds bounds = debugBounds(MatcherLayoutDebug.selected());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0, 0.0, 900.0);
        MatcherLayoutDebug.renderOverlay(graphics, font, bounds.x(), bounds.y(), bounds.width(), bounds.height());
        graphics.flush();
        graphics.pose().popPose();
    }

    private DebugBounds debugBounds(MatcherLayoutDebug.Widget widget) {
        return switch (widget) {
            case TITLE -> new DebugBounds(leftPos + layoutX(widget, 8), topPos + layoutY(widget, 6),
                    font.width(title), font.lineHeight);
            case PREVIEW_SLOT -> new DebugBounds(
                    leftPos + layoutX(widget, PREVIEW_SLOT_X),
                    topPos + layoutY(widget, PREVIEW_SLOT_Y), 18, 18);
            case SELECTOR -> new DebugBounds(leftPos + layoutX(widget, TREE_VIEW_X),
                    topPos + layoutY(widget, TREE_LIST_Y), TREE_VIEW_WIDTH, TREE_VIEW_HEIGHT);
            case VERTICAL_SCROLLBAR -> new DebugBounds(verticalScrollbarX(), verticalScrollbarY(),
                    SCROLLBAR_SIZE, TREE_VIEW_HEIGHT);
            case HORIZONTAL_SCROLLBAR -> new DebugBounds(horizontalScrollbarX(), horizontalScrollbarY(),
                    TREE_VIEW_WIDTH, SCROLLBAR_SIZE);
            case DROPDOWN -> new DebugBounds(leftPos + layoutX(widget, RULE_X),
                    topPos + layoutY(widget, MODE_BUTTON_Y), CONTROL_WIDTH,
                    VALUE_BOX_Y + 16 - MODE_BUTTON_Y);
            case SAVE_BUTTON -> new DebugBounds(leftPos + layoutX(widget, RULE_X),
                    topPos + layoutY(widget, actionButtonY()), CONTROL_WIDTH, 16);
            case INVENTORY -> new DebugBounds(leftPos + layoutX(widget, INVENTORY_X),
                    topPos + layoutY(widget, MAIN_INVENTORY_Y), 162, 54);
            case HOTBAR -> new DebugBounds(leftPos + layoutX(widget, INVENTORY_X),
                    topPos + layoutY(widget, HOTBAR_Y), 162, 18);
        };
    }

    private int layoutX(MatcherLayoutDebug.Widget widget, int normalX) {
        return MatcherLayoutDebug.x(widget, normalX);
    }

    private int layoutY(MatcherLayoutDebug.Widget widget, int normalY) {
        return MatcherLayoutDebug.y(widget, normalY);
    }

    private int actionButtonY() {
        return ACTION_BUTTON_Y;
    }

    private int errorMessageY() {
        return operator == MatcherData.Operator.BETWEEN
                ? BETWEEN_STATUS_Y : NORMAL_STATUS_Y;
    }

    private int treePanelX() {
        return leftPos + layoutX(MatcherLayoutDebug.Widget.SELECTOR, TREE_X);
    }

    private int treeListY() {
        return topPos + layoutY(MatcherLayoutDebug.Widget.SELECTOR, TREE_LIST_Y);
    }

    private int treeViewX() {
        return leftPos + layoutX(MatcherLayoutDebug.Widget.SELECTOR, TREE_VIEW_X);
    }

    private int verticalScrollbarX() {
        return leftPos + layoutX(MatcherLayoutDebug.Widget.VERTICAL_SCROLLBAR,
                TREE_X + TREE_WIDTH - SCROLLBAR_SIZE);
    }

    private int verticalScrollbarY() {
        return topPos + layoutY(MatcherLayoutDebug.Widget.VERTICAL_SCROLLBAR, TREE_LIST_Y + 2);
    }

    private int horizontalScrollbarX() {
        return leftPos + layoutX(MatcherLayoutDebug.Widget.HORIZONTAL_SCROLLBAR, TREE_VIEW_X);
    }

    private int horizontalScrollbarY() {
        return topPos + layoutY(MatcherLayoutDebug.Widget.HORIZONTAL_SCROLLBAR,
                TREE_LIST_Y + TREE_VIEW_HEIGHT + 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasVerticalOverflow() && isInsideVerticalScrollbar(mouseX, mouseY)) {
            int knobY = verticalKnobY();
            int knobHeight = verticalKnobHeight();
            if (mouseY >= knobY && mouseY < knobY + knobHeight) {
                draggingVerticalScrollbar = true;
                scrollbarDragOffset = (int) mouseY - knobY;
            } else {
                setVerticalScrollFromMouse(mouseY - knobHeight / 2.0);
            }
            return true;
        }
        if (button == 0 && hasHorizontalOverflow() && isInsideHorizontalScrollbar(mouseX, mouseY)) {
            int knobX = horizontalKnobX();
            int knobWidth = horizontalKnobWidth();
            if (mouseX >= knobX && mouseX < knobX + knobWidth) {
                draggingHorizontalScrollbar = true;
                scrollbarDragOffset = (int) mouseX - knobX;
            } else {
                setHorizontalScrollFromMouse(mouseX - knobWidth / 2.0);
            }
            return true;
        }
        if (button == 0 && isInsideTree(mouseX, mouseY)) {
            int row = (int) ((mouseY - treeListY()) / ROW_HEIGHT);
            int index = scrollOffset + row;
            if (row >= 0 && row < visibleRows() && index < visibleNodes.size()) {
                MatcherData.TreeNode node = visibleNodes.get(index);
                if (node.branch()) {
                    if (expanded.contains(node.pathKey())) expanded.remove(node.pathKey());
                    else expanded.add(node.pathKey());
                    refreshTree();
                } else {
                    selectedNode = node;
                    loadNodeValues();
                    updateEditorControls();
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingVerticalScrollbar) {
            setVerticalScrollFromMouse(mouseY - scrollbarDragOffset);
            return true;
        }
        if (button == 0 && draggingHorizontalScrollbar) {
            setHorizontalScrollFromMouse(mouseX - scrollbarDragOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingVerticalScrollbar = false;
            draggingHorizontalScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasVerticalOverflow() && isInsideVerticalScrollbar(mouseX, mouseY)) {
            scrollOffset = Math.max(0, Math.min(maxScrollOffset(),
                    scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        if (hasHorizontalOverflow() && isInsideHorizontalScrollbar(mouseX, mouseY)) {
            horizontalScrollOffset = Math.max(0, Math.min(maxHorizontalScrollOffset(),
                    horizontalScrollOffset - (int) Math.signum(scrollY) * ROW_HEIGHT));
            return true;
        }
        if (isInsideTree(mouseX, mouseY) && (hasVerticalOverflow() || hasHorizontalOverflow())) {
            if (hasShiftDown() && hasHorizontalOverflow()) {
                horizontalScrollOffset = Math.max(0, Math.min(maxHorizontalScrollOffset(),
                        horizontalScrollOffset - (int) Math.signum(scrollY) * ROW_HEIGHT));
            } else if (hasVerticalOverflow()) {
                scrollOffset = Math.max(0, Math.min(maxScrollOffset(),
                        scrollOffset - (int) Math.signum(scrollY)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isInsideTree(double mouseX, double mouseY) {
        return mouseX >= treeViewX() && mouseX < treeViewX() + TREE_VIEW_WIDTH
                && mouseY >= treeListY() && mouseY < treeListY() + TREE_VIEW_HEIGHT;
    }

    private boolean isInsideVerticalScrollbar(double mouseX, double mouseY) {
        return mouseX >= verticalScrollbarX()
                && mouseX < verticalScrollbarX() + SCROLLBAR_SIZE
                && mouseY >= verticalScrollbarY()
                && mouseY < verticalScrollbarY() + TREE_VIEW_HEIGHT;
    }

    private boolean isInsideHorizontalScrollbar(double mouseX, double mouseY) {
        return mouseX >= horizontalScrollbarX()
                && mouseX < horizontalScrollbarX() + TREE_VIEW_WIDTH
                && mouseY >= horizontalScrollbarY()
                && mouseY < horizontalScrollbarY() + SCROLLBAR_SIZE;
    }

    private int visibleRows() {
        return Math.min(MAX_VISIBLE_ROWS, visibleNodes.size());
    }

    private int maxScrollOffset() {
        return Math.max(0, visibleNodes.size() - MAX_VISIBLE_ROWS);
    }

    private boolean hasVerticalOverflow() {
        return maxScrollOffset() > 0;
    }

    private boolean hasHorizontalOverflow() {
        return maxHorizontalScrollOffset() > 0;
    }

    private int treeContentWidth() {
        int width = TREE_VIEW_WIDTH;
        for (MatcherData.TreeNode node : visibleNodes) {
            int depthOffset = 8 + Math.min(7, node.path().size() - 1) * 7;
            String marker = selectedNode != null && selectedNode.pathKey().equals(node.pathKey()) ? "> " : "  ";
            String text = marker + (node.branch() ? node.label() : node.label() + " = " + node.valueText());
            width = Math.max(width, depthOffset + 8 + font.width(text));
        }
        return width;
    }

    private int maxHorizontalScrollOffset() {
        return Math.max(0, treeContentWidth() - TREE_VIEW_WIDTH);
    }

    private int verticalKnobHeight() {
        return Math.max(SCROLLBAR_SIZE, TREE_VIEW_HEIGHT * MAX_VISIBLE_ROWS / visibleNodes.size());
    }

    private int verticalKnobY() {
        int knobRange = TREE_VIEW_HEIGHT - verticalKnobHeight();
        return verticalScrollbarY() + knobRange * scrollOffset / maxScrollOffset();
    }

    private int horizontalKnobWidth() {
        return Math.max(SCROLLBAR_SIZE, TREE_VIEW_WIDTH * TREE_VIEW_WIDTH / treeContentWidth());
    }

    private int horizontalKnobX() {
        int knobRange = TREE_VIEW_WIDTH - horizontalKnobWidth();
        return horizontalScrollbarX() + knobRange * horizontalScrollOffset / maxHorizontalScrollOffset();
    }

    private void setVerticalScrollFromMouse(double mouseY) {
        int knobHeight = verticalKnobHeight();
        int knobRange = TREE_VIEW_HEIGHT - knobHeight;
        int relative = (int) mouseY - verticalScrollbarY();
        scrollOffset = knobRange == 0 ? 0 : Math.max(0, Math.min(maxScrollOffset(),
                relative * maxScrollOffset() / knobRange));
    }

    private void setHorizontalScrollFromMouse(double mouseX) {
        int knobWidth = horizontalKnobWidth();
        int knobRange = TREE_VIEW_WIDTH - knobWidth;
        int relative = (int) mouseX - horizontalScrollbarX();
        horizontalScrollOffset = knobRange == 0 ? 0 : Math.max(0, Math.min(maxHorizontalScrollOffset(),
                relative * maxHorizontalScrollOffset() / knobRange));
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @FunctionalInterface
    private interface ButtonPress {
        void press(net.minecraft.client.gui.components.Button button);
    }

    private record DebugBounds(int x, int y, int width, int height) {
    }
}
