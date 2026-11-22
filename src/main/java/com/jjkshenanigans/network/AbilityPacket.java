package com.jjkshenanigans.network;

import com.jjkshenanigans.server.GojoAbilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "apreté la tecla X". 0-3 = habilidades, 4 = despertar (G). */
public record AbilityPacket(int id) {
    public static void encode(AbilityPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.id());
    }

    public static AbilityPacket decode(FriendlyByteBuf buf) {
        return new AbilityPacket(buf.readVarInt());
    }

    public static void handle(AbilityPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) GojoAbilities.use(sender, p.id());
        });
        c.setPacketHandled(true);
    }
}
