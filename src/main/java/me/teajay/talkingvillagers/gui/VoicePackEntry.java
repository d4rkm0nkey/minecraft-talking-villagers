package me.teajay.talkingvillagers.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.navigation.NavigationDirection;
import dev.lambdaurora.spruceui.navigation.NavigationUtils;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceEntryListWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceParentWidget;
import me.teajay.talkingvillagers.voicepacks.VoiceFile;
import me.teajay.talkingvillagers.voicepacks.Voicepack;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class VoicePackEntry extends SpruceEntryListWidget.Entry implements SpruceParentWidget<SpruceWidget> {
    private Voicepack pack;
    private final List<SpruceWidget> children = new ArrayList<>();
    private final VoicePackEntryList parent;
    private @Nullable SpruceWidget focused;
    private boolean dragging;

    private VoicePackEntry(Voicepack pack, VoicePackEntryList parent) {
        this.pack = pack;
        this.parent = parent;
    }

    public static VoicePackEntry create(Voicepack voicepack, VoicePackEntryList parent, Lambda callBack) {
        VoicePackEntry entry = new VoicePackEntry(voicepack, parent);
        var container = new SpruceContainerWidget((Position.of(entry, 0, 0)), parent.getWidth(), 28);
        int containerWidth = container.getWidth();
        int labelWidth = containerWidth - containerWidth / 3 - 20;
        var label = new SpruceLabelWidget(Position.of(container.getPosition(), 20, 8), Text.literal(voicepack.getName()), labelWidth);
        var buttonText = Text.literal("Install all");
        SpruceButtonWidget.PressAction buttonClickListener = new VoicePackInstallAction(voicepack, callBack);
        if(voicepack.isFullyInstalled()) {
            buttonText = Text.literal("Uninstall all");
        }
        SpruceButtonWidget button = new SpruceButtonWidget(
                Position.of(container.getPosition(), labelWidth, 4),
                containerWidth / 3,
                container.getHeight() - 8,
                buttonText,
                buttonClickListener
        );
        container.addChild(label);
        container.addChild(button);
        container.setBackground(new SimpleColorBackground(0, 0, 0, 100));
        var voiceContainer = new SpruceContainerWidget((Position.of(container,0, 30)), parent.getWidth(), 28 * voicepack.getVoices().size());
        List<VoiceFile> voices = voicepack.getVoices();
        for (int i = 0; i < voices.size(); i++) {
            VoiceFile voice = voices.get(i);
            voiceContainer.addChild(new VoiceEntry(voicepack, voice, Position.of(0, 4+i*28), parent.getWidth(), 28, callBack));
        }
        voiceContainer.setBackground(new SimpleColorBackground(0, 0, 0, 0));
        entry.children.add(container);
        //voiceContainer.setBackground(new SimpleColorBackground(255,0,0, 50));
        entry.children.add(voiceContainer);
        //this.getOptionTooltip().ifPresent(button::setTooltip);
        return entry;
    }

    @Override
    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.children.forEach(widget -> widget.render(matrices, mouseX, mouseY, delta));
    }

    @Override
    public List<SpruceWidget> children() {
        return children;
    }

    @Override
    public @Nullable SpruceWidget getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable SpruceWidget focused) {
        if (this.focused == focused)
            return;
        if (this.focused != null)
            this.focused.setFocused(false);
        this.focused = focused;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.setFocused(null);
        }
    }

    @Override
    public int getWidth() {
        return this.parent.getWidth() - (this.parent.getBorder().getThickness() * 2);
    }

    @Override
    public int getHeight() {
        return this.children.stream().mapToInt(SpruceWidget::getHeight).sum() + 4;
    }

    /* Input */

    @Override
    protected boolean onMouseClick(double mouseX, double mouseY, int button) {
        Iterator<SpruceWidget> it = this.children().iterator();

        SpruceWidget element;
        do {
            if (!it.hasNext()) {
                return false;
            }

            element = it.next();
        } while (!element.mouseClicked(mouseX, mouseY, button));

        this.setFocused(element);
        if (button == GLFW.GLFW_MOUSE_BUTTON_1)
            this.dragging = true;

        return true;
    }

    @Override
    protected boolean onMouseRelease(double mouseX, double mouseY, int button) {
        this.dragging = false;
        return this.hoveredElement(mouseX, mouseY).filter(element -> element.mouseReleased(mouseX, mouseY, button)).isPresent();
    }

    @Override
    protected boolean onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.getFocused() != null && this.dragging && button == GLFW.GLFW_MOUSE_BUTTON_1
                && this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        return this.focused != null && this.focused.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean onKeyRelease(int keyCode, int scanCode, int modifiers) {
        return this.focused != null && this.focused.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean onCharTyped(char chr, int keyCode) {
        return this.focused != null && this.focused.charTyped(chr, keyCode);
    }

    /* Navigation */

    @Override
    public boolean onNavigation(@NotNull NavigationDirection direction, boolean tab) {
        if (this.requiresCursor()) return false;
        if (!tab && direction.isVertical()) {
            if (this.isFocused()) {
                this.setFocused(null);
                return false;
            }
            int lastIndex = this.parent.getLastIndex();
            if (lastIndex >= this.children.size())
                lastIndex = this.children.size() - 1;
            if (!this.children.get(lastIndex).onNavigation(direction, tab))
                return false;
            this.setFocused(this.children.get(lastIndex));
            return true;
        }

        boolean result = NavigationUtils.tryNavigate(direction, tab, this.children, this.focused, this::setFocused, true);
        if (result) {
            this.setFocused(true);
            if (direction.isHorizontal() && this.getFocused() != null) {
                this.parent.setLastIndex(this.children.indexOf(this.getFocused()));
            }
        }
        return result;
    }
}
