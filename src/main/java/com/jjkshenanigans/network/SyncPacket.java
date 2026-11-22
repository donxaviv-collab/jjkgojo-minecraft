package com.jjkshenanigans.network;

import com.jjkshenanigans.common.ClientState;
import com.jjkshenanigans.common.GojoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: estado para el HUD. */
public class SyncPacket {
    private final boolean active;
    private final float energy, sixEyes;
    private final int awaken;
    private final int[] cd;

    public SyncPacket(GojoState s) {
        this(s.active, s.energy, s.sixEyes, s.awakenTicks, s.cooldown.clone());
    }

    private SyncPacket(boolean active, float energy, float sixEyes, int awaken, int[] cd) {
        this.active = active;
        this.energy = energy;
        this.sixEyes = sixEyes;
        this.awaken = awaken;
        this.cd = cd;
    }

    public static void encode(SyncPacket p, FriendlyByteBuf b) {
        b.writeBoolean(p.active);
        b.writeFloat(p.energy);
        b.writeFloat(p.sixEyes);
        b.writeVarInt(p.awaken);
        for (int i = 0; i < 8; i++) b.writeVarInt(p.cd[i]);
    }

    public static SyncPacket decode(FriendlyByteBuf b) {
        boolean active = b.readBoolean();
        float energy = b.readFloat();
        float six = b.readFloat();
        int awaken = b.readVarInt();
        int[] cd = new int[8];
        for (int i = 0; i < 8; i++) cd[i] = b.readVarInt();
        return new SyncPacket(active, energy, six, awaken, cd);
    }

    public static void handle(SyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            GojoState s = ClientState.STATE;
            s.active = p.active;
            s.energy = p.energy;
            s.sixEyes = p.sixEyes;
            s.awakenTicks = p.awaken;
            System.arraycopy(p.cd, 0, s.cooldown, 0, 8);
        });
        c.setPacketHandled(true);
    }
}
