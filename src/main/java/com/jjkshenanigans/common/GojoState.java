package com.jjkshenanigans.common;

/** Estado de Gojo de un jugador (servidor = real, cliente = copia para el HUD). */
public class GojoState {
    public static final float MAX_ENERGY = 100f;
    public static final float MAX_SIX_EYES = 100f;
    public static final int AWAKEN_TICKS = 1200; // 60 s de Six Eyes

    public static final String[] KEYS = {"Z", "X", "C", "V"};
    public static final String[] BASE_NAMES = {"Lapse Blue", "Reversal Red", "Rapid Punches", "Twofold Kick"};
    public static final String[] AWAKE_NAMES = {"Lapse Blue MAX", "Reversal Red MAX", "Hollow Purple", "Infinite Void"};

    // Valores propios (NO son los del juego original, ajústalos a gusto)
    public static final int[] BASE_COST = {15, 20, 10, 10};
    public static final int[] AWAKE_COST = {30, 35, 60, 50};
    public static final int[] BASE_CD = {160, 200, 140, 160};   // ticks (20 = 1 s)
    public static final int[] AWAKE_CD = {240, 280, 500, 900};

    public boolean active;
    public float energy = MAX_ENERGY;
    public float sixEyes;
    public int awakenTicks;
    /** 0-3 = set base, 4-7 = set Six Eyes */
    public final int[] cooldown = new int[8];

    public boolean awakened() {
        return awakenTicks > 0;
    }
}
