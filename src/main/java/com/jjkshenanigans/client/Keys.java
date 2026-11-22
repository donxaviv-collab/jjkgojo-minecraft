package com.jjkshenanigans.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private static final String CATEGORY = "key.categories.jjkshenanigans";

    /** Índices 0-3 = habilidades (Z X C V), 4 = despertar Six Eyes (G). */
    public static final KeyMapping[] ALL = {
            k("key.jjkshenanigans.ability1", GLFW.GLFW_KEY_Z),
            k("key.jjkshenanigans.ability2", GLFW.GLFW_KEY_X),
            k("key.jjkshenanigans.ability3", GLFW.GLFW_KEY_C),
            k("key.jjkshenanigans.ability4", GLFW.GLFW_KEY_V),
            k("key.jjkshenanigans.awaken", GLFW.GLFW_KEY_G)
    };

    private Keys() {}

    private static KeyMapping k(String name, int code) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, code, CATEGORY);
    }
}
