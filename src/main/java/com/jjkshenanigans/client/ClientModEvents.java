package com.jjkshenanigans.client;

import com.jjkshenanigans.JJKShenanigans;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JJKShenanigans.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent e) {
        for (KeyMapping k : Keys.ALL) e.register(k);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("jjk_hud", Hud.OVERLAY);
    }
}
