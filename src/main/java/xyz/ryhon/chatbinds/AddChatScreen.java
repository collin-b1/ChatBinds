package xyz.ryhon.chatbinds;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class AddChatScreen extends Screen {
	private final Screen parent;

	TextFieldWidget titleBox;
	TextFieldWidget commandBox;

	ButtonWidget keyButton;
	ButtonWidget addButton;
	ButtonWidget cancelButton;

	String cmd;
	InputUtil.Key key = InputUtil.UNKNOWN_KEY;
	boolean listening;

	public AddChatScreen(String cmd_, Screen parent) {
		super(Text.translatable("chatbinds.key.add_chat"));
		cmd = cmd_;
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (client == null) return;
        TextWidget titleText = new TextWidget(Text.translatable("chatbinds.addScreen.title"), client.textRenderer);
		titleText.setX(width / 4);
		titleText.setY((height / 2) - 64);
		addDrawable(titleText);

		titleBox = new TextFieldWidget(client.textRenderer, width / 2, 24, Text.empty());
		titleBox.setPosition(width / 2 - (titleBox.getWidth() / 2), titleText.getY() + titleText.getHeight());
		addDrawable(titleBox);
		addSelectableChild(titleBox);

		TextWidget commandText = new TextWidget(Text.translatable("chatbinds.addScreen.command"), client.textRenderer);
		commandText.setX(width / 4);
		commandText.setY(titleBox.getY() + titleBox.getHeight());
		addDrawable(commandText);

		commandBox = new TextFieldWidget(client.textRenderer, width / 2, 24, Text.empty());
		titleBox.setChangedListener(this::onCommandChanged);
		commandBox.setPosition(width / 2 - (commandBox.getWidth() / 2), commandText.getY() + commandText.getHeight());
		commandBox.setMaxLength(256);
		addDrawable(commandBox);
		addSelectableChild(commandBox);
		this.setInitialFocus(commandBox);

		int buttonWidth = 64;
		int buttonHeight = 16;

		keyButton = ButtonWidget.builder(Text.empty(), this::onListenButton)
				.position(commandBox.getX() + commandBox.getWidth() - buttonWidth,
						commandBox.getY() + commandBox.getHeight() + 6)
				.size(buttonWidth, buttonHeight)
				.build();
		addDrawable(keyButton);
		addDrawableChild(keyButton);
		updateKeyText();

		TextWidget keyText = new TextWidget(Text.translatable("chatbinds.addScreen.keybind"), client.textRenderer);
		keyText.setX(keyButton.getX() - keyText.getWidth());
		keyText.setY(keyButton.getY() + (keyButton.getHeight() / 2) - (keyText.getHeight() / 2));
		addDrawable(keyText);

		addButton = ButtonWidget.builder(Text.translatable("chatbinds.addScreen.add"), this::onAdd)
				.position(keyButton.getX(),
						keyButton.getY() + keyButton.getHeight() + 6)
				.size(buttonWidth, buttonHeight)
				.build();
		addDrawable(addButton);
		addSelectableChild(addButton);

		cancelButton = ButtonWidget.builder(Text.translatable("chatbinds.addScreen.cancel"), this::onCancel)
				.position(addButton.getX() - buttonWidth,
						addButton.getY())
				.size(buttonWidth, buttonHeight)
				.build();
		addDrawable(cancelButton);
		addSelectableChild(cancelButton);
	}

	void onListenButton(ButtonWidget b) {
		listening = true;
		updateKeyText();
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (listening) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE)
				key = InputUtil.UNKNOWN_KEY;
			else
				key = InputUtil.fromKeyCode(keyCode, scanCode);

			listening = false;
			updateKeyText();
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			add();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (listening) {
			key = InputUtil.Type.MOUSE.createFromCode(button);
			listening = false;
			updateKeyText();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	void updateKeyText() {
		if (listening)
			keyButton.setMessage(Text.literal("..."));
		else
			keyButton.setMessage(key.getLocalizedText());
	}

	void onCommandChanged(String c) {
		cmd = c;
	}

	void onAdd(ButtonWidget b) {
		add();
	}

	void onCancel(ButtonWidget b) {
		close();
	}

	void add() {
		String cmd = commandBox.getText().strip();
		String title = titleBox.getText().strip();

		if (cmd.isEmpty())
			return;
		if (title.isEmpty())
			title = cmd;

		ChatBinds.registerUserBind(title, cmd, key);
		ChatBinds.saveConfig();
		close();
	}

	@Override
	public void close() {
		if (client == null) return;
		client.setScreen(parent);
    }
}
