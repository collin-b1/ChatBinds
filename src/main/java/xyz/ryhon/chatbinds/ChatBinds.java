package xyz.ryhon.chatbinds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

@Environment(EnvType.CLIENT)
public class ChatBinds implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("chat-binds");

	private static ArrayList<ChatBind> userBinds = new ArrayList<>();
	private static final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chatbinds");
	private static final Path configFileBinds = configDir.resolve("binds.json");

	@Override
	public void onInitialize() {
		KeyBinding menuBind = new KeyBinding("chatbinds.key.menu",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
				"category.chatbinds");
		KeyBindingHelper.registerKeyBinding(menuBind);
		KeyBinding addChatBind = new KeyBinding("chatbinds.key.add_chat",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
				"category.chatbinds");
		KeyBindingHelper.registerKeyBinding(addChatBind);

		loadConfig();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.currentScreen == null && client.player != null) {
				if (menuBind.wasPressed())
					client.setScreen(new BindMenuScreen(null));
				if (addChatBind.wasPressed())
					client.setScreen(new AddChatScreen("", null));

				for (ChatBind b : userBinds) {
					if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), b.key.getCode())) {
						if (!b.wasKeyPressed) {
							sendMessage(b.cmd);
							b.wasKeyPressed = true;
						}
					} else {
						b.wasKeyPressed = false;
					}
				}
			}
		});
	}

	public void sendMessage(String msg) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		if (msg.startsWith("/")) {
			player.networkHandler.sendChatCommand(msg.substring(1));
		} else {
			player.networkHandler.sendChatMessage(msg);
		}
	}

	public static class ChatBind {
		public InputUtil.Key key;
		public String cmd;
		public String title;

		// Tracks if the key bind was pressed last tick.
		// Used to prevent multiple messages getting send when key is held.
		public boolean wasKeyPressed = false;
	}

	public static void loadConfig() {
		userBinds = new ArrayList<>();

		try {
			Files.createDirectories(configDir);
			if (!Files.exists(configFileBinds))
				return;

			String str = Files.readString(configFileBinds);
			JsonArray ja = (JsonArray) JsonParser.parseString(str);

			for (JsonElement je : ja) {
				if (je instanceof JsonObject jo) {
					String title = jo.get("title").getAsString();
					String cmd = jo.get("cmd").getAsString();
					InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(jo.get("key").getAsInt());
					registerUserBind(title, cmd, key);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load config", e);
		}
	}

	public static List<ChatBind> getUserBinds() {
		return userBinds;
	}

	public static boolean registerUserBind(String title, String cmd, InputUtil.Key key) {
		ChatBind b = new ChatBind();
		b.title = title;
		b.cmd = cmd;
		b.key = key;
		return userBinds.add(b);
	}

	public static boolean unregisterUserBind(ChatBind bind) {
		return userBinds.remove(bind);
	}

	public static void saveConfig() {
		JsonArray ja = new JsonArray();

		for (ChatBind b : userBinds) {
			JsonObject jo = new JsonObject();
			jo.add("cmd", new JsonPrimitive(b.cmd));
			jo.add("title", new JsonPrimitive(b.title));
			jo.add("key", new JsonPrimitive(b.key.getCode()));

			ja.add(jo);
		}

		String json = new Gson().toJson(ja);

		try {
			Files.createDirectories(configDir);
			Files.writeString(configFileBinds, json);
		} catch (Exception e) {
			LOGGER.error("Failed to save config", e);
		}
	}
}