package me.vitaxses.focused.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.util.Window;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class FocusedClient implements ClientModInitializer {
    private boolean wasHurtLastTick = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            boolean isHurtNow = client.player.hurtTime > 0;

            if (isHurtNow && !wasHurtLastTick) {
                System.out.println("[Focused] Player was hurt! Attempting to focus window...");
                bringWindowToFront(client);
            }

            wasHurtLastTick = isHurtNow;
        });
    }

    private void bringWindowToFront(MinecraftClient client) {
        try {
            Window window = client.getWindow();
            long handle = window.getHandle();
            boolean showMsg = false;

            int[] xpos = new int[1];
            int[] ypos = new int[1];
            int[] width = new int[1];
            int[] height = new int[1];

            GLFW.glfwGetWindowPos(handle, xpos, ypos);
            GLFW.glfwGetWindowSize(handle, width, height);

            if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE) {
                GLFW.glfwRestoreWindow(handle);
                GLFW.glfwSetWindowPos(handle, xpos[0], ypos[0]);
                GLFW.glfwSetWindowSize(handle, width[0], height[0]);
                showMsg = true;
            }

            if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) {
                GLFW.glfwShowWindow(handle);
                GLFW.glfwFocusWindow(handle);
                showMsg = true;
            }

            System.out.println("[Focused] LWJGL window focus/unminimize attempted.");
            if (showMsg) {
                client.player.sendMessage(Text.literal("[Focused] getting attacked..").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                if (client.isInSingleplayer()) client.setScreen(new GameMenuScreen(false));
            }

        } catch (Exception e) {
            System.err.println("[Focused] LWJGL failed, trying AWT fallback.");
            e.printStackTrace();
            try {
                Frame[] frames = Frame.getFrames();
                for (Frame frame : frames) {
                    if (frame.isVisible()) {
                        if ((frame.getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
                            frame.setExtendedState(Frame.NORMAL);
                        }

                        if (!frame.isFocused()) {
                            frame.toFront();
                            frame.requestFocus();
                        }

                        System.out.println("[Focused] AWT window focus/unminimize attempted.");
                        break;
                    }
                }
            } catch (Exception awtEx) {
                awtEx.printStackTrace();
            }
        }
    }

}
