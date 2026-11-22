package com.jjkshenanigans.network;

import com.jjkshenanigans.JJKShenanigans;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JJKShenanigans.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, AbilityPacket.class, AbilityPacket::encode, AbilityPacket::decode, AbilityPacket::handle);
        CHANNEL.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
    }
}
