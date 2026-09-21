package com.sophisticatedmatcher.client;

import com.sophisticatedmatcher.SophisticatedMatcherMod;
import com.sophisticatedmatcher.menu.MatcherMenu;
import com.sophisticatedmatcher.network.MatcherNetwork;
import com.sophisticatedmatcher.util.MatcherData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.NumericTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MatcherEditorScreen extends AbstractContainerScreen<MatcherMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_background.png");
    private static final Identifier VANILLA_INVENTORY = Identifier.withDefaultNamespace(
            "textures/gui/container/inventory.png");
    private static final Identifier BUTTON = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_button.png");
    private static final Identifier BUTTON_HOVER = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_button_hover.png");
    private static final Identifier BUTTON_PRESSED = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_button_pressed.png");
    private static final Identifier BUTTON_DISABLED = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/editor_button_disabled.png");
    private static final Identifier VERTICAL_SCROLL_KNOB = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/scroll_vertical_knob.png");
    private static final Identifier HORIZONTAL_SCROLL_KNOB = Identifier.fromNamespaceAndPath(
            SophisticatedMatcherMod.MOD_ID, "textures/gui/scroll_horizontal_knob.png");
    private static final int GUI_WIDTH = 360;
    private static final int GUI_HEIGHT = 284;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 340;
    private static final int LEFT_PANEL_X = 12;
    private static final int RIGHT_PANEL_X = 180;
    private static final int PANEL_Y = 34;
    private static final int PANEL_WIDTH = 168;
    private static final int PANEL_HEIGHT = 192;
    private static final int TREE_X = LEFT_PANEL_X;
    private static final int TREE_WIDTH = PANEL_WIDTH;
    private static final int TREE_LIST_Y = 64;
    private static final int ROW_HEIGHT = 12;
    private static final int TREE_VIEW_X = TREE_X + 4;
    private static final int TREE_VIEW_WIDTH = TREE_WIDTH - 16;
    private static final int TREE_VIEW_HEIGHT = 144;
    private static final int MAX_VISIBLE_ROWS = TREE_VIEW_HEIGHT / ROW_HEIGHT;
    private static final int SCROLLBAR_SIZE = 6;
    private static final int SCROLLBAR_TEXTURE_SIZE = 8;
    private static final int RULE_X = RIGHT_PANEL_X + 10;
    private static final int CONTROL_WIDTH = PANEL_WIDTH - 20;
    private static final int INVENTORY_X = (GUI_WIDTH - 162) / 2;
    private static final int PREVIEW_SLOT_X = LEFT_PANEL_X + (PANEL_WIDTH - 18) / 2;
    private static final int PREVIEW_SLOT_Y = PANEL_Y + 7;
    private static final int MAIN_INVENTORY_Y = 226;
    private static final int HOTBAR_Y = 284;
    private static final int PATH_Y = PANEL_Y + 28;
    private static final int MODE_BUTTON_Y = PANEL_Y + 42;
    private static final int VALUE_LABEL_Y = PANEL_Y + 61;
    private static final int VALUE_BOX_Y = PANEL_Y + 72;
    private static final int MAX_LABEL_Y = PANEL_Y + 93;
    private static final int MAX_BOX_Y = PANEL_Y + 104;
    private static final int BOUNDS_BUTTON_Y = PANEL_Y + 136;
    private static final int STATUS_Y = PANEL_Y + 156;
    private static final int ACTION_BUTTON_Y = PANEL_Y + 170;
    private static final int INVENTORY_TEXTURE_SIZE = 256;
    private static final int INVENTORY_SLOT_OFFSET = 1;

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
    private MatcherButton backButton;
    private String errorMessage = "";
    private String statusMessage = "";

    public MatcherEditorScreen(MatcherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        MatcherLayoutDebug.beginEditorScreen();
        applySlotLayout();
        refreshTree();

        valueBox = createEditBox("", CONTROL_WIDTH);
        minBox = createEditBox("", CONTROL_WIDTH - 31);
        maxBox = createEditBox("", CONTROL_WIDTH - 31);
        addRenderableWidget(valueBox);
        addRenderableWidget(minBox);
        addRenderableWidget(maxBox);
        selectInitialNode();

        modeButton = editorButton(RULE_X, MODE_BUTTON_Y, CONTROL_WIDTH,
                Component.literal(""), button -> cycleOperator());
        boundsButton = editorButton(RULE_X, BOUNDS_BUTTON_Y, CONTROL_WIDTH,
                Component.literal(""), button -> cycleBounds());
        backButton = editorButton(RULE_X, ACTION_BUTTON_Y, 70,
                Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".back"),
                button -> returnToMatcher());
        saveButton = editorButton(RULE_X + 78, ACTION_BUTTON_Y, 70,
                Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".save_rule"),
                button -> saveRule());
        addRenderableWidget(modeButton);
        addRenderableWidget(boundsButton);
        addRenderableWidget(backButton);
        addRenderableWidget(saveButton);
        updateEditorControls();
    }

    private EditBox createEditBox(String value, int width) {
        EditBox box = new EditBox(font, leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_BOX_Y), width, 16, Component.empty());
        box.setMaxLength(64);
        box.setValue(value);
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
        statusMessage = "";
    }

    private void cycleOperator() {
        MatcherData.Operator[] options = operatorOptions();
        int next = (indexOf(options, operator) + 1) % options.length;
        operator = options[next];
        errorMessage = "";
        statusMessage = "";
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
        valueBox.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_BOX_Y));
        minBox.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X + 31),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_BOX_Y));
        maxBox.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X + 31),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, MAX_BOX_Y));
        modeButton.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, MODE_BUTTON_Y));
        boundsButton.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.DROPDOWN, BOUNDS_BUTTON_Y));
        backButton.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.SAVE_BUTTON, RULE_X),
                topPos + layoutY(MatcherLayoutDebug.Widget.SAVE_BUTTON, ACTION_BUTTON_Y));
        saveButton.setPosition(leftPos + layoutX(MatcherLayoutDebug.Widget.SAVE_BUTTON, RULE_X + 78),
                topPos + layoutY(MatcherLayoutDebug.Widget.SAVE_BUTTON, ACTION_BUTTON_Y));
        valueBox.setEditable(selectedNode != null && selectedNode.value() instanceof NumericTag);
        saveButton.active = selectedNode != null && !menu.previewStack().isEmpty();
        if (operator == MatcherData.Operator.EXISTS || operator == MatcherData.Operator.NOT_EXISTS) {
            boundsButton.setMessage(Component.literal(""));
        } else {
            String left = minInclusive ? "[" : "(";
            String right = maxInclusive ? "]" : ")";
            boundsButton.setMessage(Component.literal(left + "x, x" + right));
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
        statusMessage = Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".saved").getString();
    }

    private void returnToMatcher() {
        super.onClose();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        applySlotLayout();
        refreshTree();
        updateEditorControls();
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0,
                GUI_WIDTH, GUI_HEIGHT - 3, GUI_WIDTH, GUI_HEIGHT - 3, GUI_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos + GUI_HEIGHT - 3, 0, 337,
                GUI_WIDTH, 3, GUI_WIDTH, 3, GUI_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, VANILLA_INVENTORY,
                leftPos + layoutX(MatcherLayoutDebug.Widget.INVENTORY, INVENTORY_X) - INVENTORY_SLOT_OFFSET,
                topPos + layoutY(MatcherLayoutDebug.Widget.INVENTORY, MAIN_INVENTORY_Y) - INVENTORY_SLOT_OFFSET,
                7, 83, 162, 54, 162, 54, INVENTORY_TEXTURE_SIZE, INVENTORY_TEXTURE_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, VANILLA_INVENTORY,
                leftPos + layoutX(MatcherLayoutDebug.Widget.HOTBAR, INVENTORY_X) - INVENTORY_SLOT_OFFSET,
                topPos + layoutY(MatcherLayoutDebug.Widget.HOTBAR, HOTBAR_Y) - INVENTORY_SLOT_OFFSET,
                7, 141, 162, 18, 162, 18, INVENTORY_TEXTURE_SIZE, INVENTORY_TEXTURE_SIZE);
        graphics.nextStratum();
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        graphics.nextStratum();
        renderTree(graphics);
        renderDebugOverlay(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable("container." + SophisticatedMatcherMod.MOD_ID + ".editor"),
                layoutX(MatcherLayoutDebug.Widget.TITLE, 8),
                layoutY(MatcherLayoutDebug.Widget.TITLE, 6), 0xFF404040, false);
        graphics.text(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".rule"),
                layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RIGHT_PANEL_X + 10),
                layoutY(MatcherLayoutDebug.Widget.DROPDOWN, PANEL_Y + 7), 0xFF404040, false);
        if (selectedNode == null) {
            graphics.text(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".select_leaf"),
                    layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                    layoutY(MatcherLayoutDebug.Widget.DROPDOWN, PATH_Y), 0xFF777777, false);
        } else {
            String path = font.plainSubstrByWidth(selectedNode.pathText(), CONTROL_WIDTH);
            graphics.text(font, path, layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                    layoutY(MatcherLayoutDebug.Widget.DROPDOWN, PATH_Y), 0xFF303030, false);
            if (operator == MatcherData.Operator.BETWEEN) {
                graphics.text(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".min"),
                        layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                        layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_LABEL_Y), 0xFF555555, false);
                graphics.text(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".max"),
                        layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                        layoutY(MatcherLayoutDebug.Widget.DROPDOWN, MAX_LABEL_Y), 0xFF555555, false);
            } else {
                graphics.text(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".value"),
                        layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                        layoutY(MatcherLayoutDebug.Widget.DROPDOWN, VALUE_LABEL_Y), 0xFF555555, false);
            }
        }
        if (!errorMessage.isEmpty()) {
            graphics.text(font, errorMessage, layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                    layoutY(MatcherLayoutDebug.Widget.DROPDOWN, STATUS_Y), 0xFFFF5555, false);
        } else if (!statusMessage.isEmpty()) {
            graphics.text(font, statusMessage, layoutX(MatcherLayoutDebug.Widget.DROPDOWN, RULE_X),
                    layoutY(MatcherLayoutDebug.Widget.DROPDOWN, STATUS_Y), 0xFF3F7F3F, false);
        }
    }

    private void renderTree(GuiGraphicsExtractor graphics) {
        int treeX = treePanelX();
        int treeY = treeListY();
        int viewX = treeViewX();
        int viewY = treeY;
        int viewRight = viewX + TREE_VIEW_WIDTH;
        int viewBottom = viewY + TREE_VIEW_HEIGHT;
        if (visibleNodes.isEmpty()) {
            graphics.text(font, Component.translatable("gui." + SophisticatedMatcherMod.MOD_ID + ".no_nbt"),
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
                graphics.text(font, expanded.contains(node.pathKey()) ? "-" : "+", x, y, 0xFFDDDDDD, false);
            }
            String marker = selectedNode != null && selectedNode.pathKey().equals(node.pathKey()) ? "> " : "  ";
            String text = marker + (node.branch() ? node.label() : node.label() + " = " + node.valueText());
            graphics.text(font, text, x + 8, y, 0xFFFFFFFF, false);
        }
        graphics.disableScissor();
        renderTreeScrollbars(graphics);
    }

    private void renderTreeScrollbars(GuiGraphicsExtractor graphics) {
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

    private void blitVerticalKnob(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0,
                SCROLLBAR_SIZE, 2, SCROLLBAR_TEXTURE_SIZE, 2, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + 2, 0, 4,
                SCROLLBAR_SIZE, height - 4, SCROLLBAR_TEXTURE_SIZE, 1, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + height - 2, 0, 6,
                SCROLLBAR_SIZE, 2, SCROLLBAR_TEXTURE_SIZE, 2, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
    }

    private void blitHorizontalKnob(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0,
                2, SCROLLBAR_SIZE, 2, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + 2, y, 4, 0,
                width - 4, SCROLLBAR_SIZE, 1, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - 2, y, 6, 0,
                2, SCROLLBAR_SIZE, 2, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE, SCROLLBAR_TEXTURE_SIZE);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (MatcherLayoutDebug.keyPressed(event.key(), event.hasShiftDown(), event.hasAltDown())) {
            applySlotLayout();
            updateEditorControls();
            return true;
        }
        return super.keyPressed(event);
    }

    private void renderDebugOverlay(GuiGraphicsExtractor graphics) {
        if (!MatcherLayoutDebug.isEnabled()) return;
        graphics.nextStratum();
        DebugBounds bounds = debugBounds(MatcherLayoutDebug.selected());
        MatcherLayoutDebug.renderOverlay(graphics, font, bounds.x(), bounds.y(), bounds.width(), bounds.height());
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
                    topPos + layoutY(widget, PANEL_Y + 7), CONTROL_WIDTH, ACTION_BUTTON_Y - (PANEL_Y + 7));
            case SAVE_BUTTON -> new DebugBounds(leftPos + layoutX(widget, RULE_X),
                    topPos + layoutY(widget, ACTION_BUTTON_Y), 148, 16);
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
        return topPos + layoutY(MatcherLayoutDebug.Widget.VERTICAL_SCROLLBAR, TREE_LIST_Y);
    }

    private int horizontalScrollbarX() {
        return leftPos + layoutX(MatcherLayoutDebug.Widget.HORIZONTAL_SCROLLBAR, TREE_VIEW_X);
    }

    private int horizontalScrollbarY() {
        return topPos + layoutY(MatcherLayoutDebug.Widget.HORIZONTAL_SCROLLBAR,
                TREE_LIST_Y + TREE_VIEW_HEIGHT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
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
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && draggingVerticalScrollbar) {
            setVerticalScrollFromMouse(mouseY - scrollbarDragOffset);
            return true;
        }
        if (button == 0 && draggingHorizontalScrollbar) {
            setHorizontalScrollFromMouse(mouseX - scrollbarDragOffset);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            draggingVerticalScrollbar = false;
            draggingHorizontalScrollbar = false;
        }
        return super.mouseReleased(event);
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
            if (scrollX != 0.0 && hasHorizontalOverflow()) {
                horizontalScrollOffset = Math.max(0, Math.min(maxHorizontalScrollOffset(),
                        horizontalScrollOffset - (int) Math.signum(scrollX) * ROW_HEIGHT));
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
