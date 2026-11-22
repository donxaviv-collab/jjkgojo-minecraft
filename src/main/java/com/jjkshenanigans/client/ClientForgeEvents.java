package com.jjkshenanigans.client;

import com.jjkshenanigans.JJKShenanigans;
import com.jjkshenanigans.network.AbilityPacket;
import com.jjkshenanigans.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JJKShenanigans.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        for (int i = 0; i < Keys.ALL.length; i++) {
            while (Keys.ALL[i].consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new AbilityPacket(i));
            }
        }
    }
}
