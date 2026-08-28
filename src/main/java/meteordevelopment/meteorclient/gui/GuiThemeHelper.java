package meteordevelopment.meteorclient.gui;

import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;

public class GuiThemeHelper {
    public static WButton button(GuiTheme theme, String text) {
        return theme.button(text);
    }

    public static WConfirmedButton confirmedButton(GuiTheme theme, String text, String confirmText) {
        return theme.confirmedButton(text, confirmText);
    }
}
