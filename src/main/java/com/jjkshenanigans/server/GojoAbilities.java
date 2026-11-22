package com.jjkshenanigans.server;

import com.jjkshenanigans.common.GojoState;
import com.jjkshenanigans.network.ModNetwork;
import com.jjkshenanigans.network.SyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.*;

/**
 * Moveset de "The Honored One" (Gojo) estilo Jujutsu Shenanigans.
 * Set base:   Z Lapse Blue | X Reversal Red | C Rapid Punches | V Twofold Kick
 * Six Eyes (G con la barra llena): Z Blue MAX | X Red MAX | C Hollow Purple | V Infinite Void
 * Todo corre en el servidor; el cliente solo manda teclas y dibuja el HUD.
 */
public final class GojoAbilities {
    /** Si es false, las habilidades no rompen bloques (útil para servidores con protección). */
    public static final boolean DESTROY_BLOCKS = true;
    private static final int DOMAIN_TICKS = 160;

    private static final ParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.25f, 0.55f, 1.0f), 1.6f);
    private static final ParticleOptions RED = new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.15f), 1.6f);
    private static final ParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.7f, 0.2f, 1.0f), 2.0f);
    private static final ParticleOptions WHITE = new DustParticleOptions(new Vector3f(1f, 1f, 1f), 1.2f);

    private static final Map<UUID, GojoState> STATES = new HashMap<>();
    private static final Set<UUID> STUNNED = new HashSet<>();
    private static final Map<UUID, Orb> BLUE_MAX = new HashMap<>();

    private static final class Orb {
        Vec3 pos = Vec3.ZERO;
        boolean alive = true;
    }

    private GojoAbilities() {}

    // ---------------------------------------------------------------- estado

    public static GojoState state(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new GojoState());
    }

    public static void remove(ServerPlayer p) {
        STATES.remove(p.getUUID());
        STUNNED.remove(p.getUUID());
        BLUE_MAX.remove(p.getUUID());
    }

    public static void clearAll() {
        STATES.clear();
        STUNNED.clear();
        BLUE_MAX.clear();
    }

    public static void activate(ServerPlayer p, boolean on) {
        GojoState s = state(p);
        s.active = on;
        s.energy = GojoState.MAX_ENERGY;
        s.sixEyes = 0;
        s.awakenTicks = 0;
        Arrays.fill(s.cooldown, 0);
        p.sendSystemMessage(Component.literal(on ? "Personaje: The Honored One (Gojo)" : "Personaje: ninguno"));
        sync(p);
    }

    public static void fillSixEyes(ServerPlayer p) {
        GojoState s = state(p);
        if (!s.active) return;
        s.sixEyes = GojoState.MAX_SIX_EYES;
        sync(p);
    }

    public static void onDeath(ServerPlayer p) {
        GojoState s = STATES.get(p.getUUID());
        if (s == null) return;
        s.awakenTicks = 0;
        s.sixEyes = 0;
        s.energy = GojoState.MAX_ENERGY;
        Arrays.fill(s.cooldown, 0);
        STUNNED.remove(p.getUUID());
        BLUE_MAX.remove(p.getUUID());
    }

    /** La barra de Six Eyes se llena al hacer daño. */
    public static void onDealDamage(ServerPlayer p, float amount) {
        GojoState s = STATES.get(p.getUUID());
        if (s == null || !s.active || s.awakened()) return;
        s.sixEyes = Math.min(GojoState.MAX_SIX_EYES, s.sixEyes + amount * 2.5f);
    }

    public static void sync(ServerPlayer p) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new SyncPacket(state(p)));
    }

    public static void tickPlayer(ServerPlayer p) {
        GojoState s = STATES.get(p.getUUID());
        if (s == null || !s.active) return;
        for (int i = 0; i < 8; i++) if (s.cooldown[i] > 0) s.cooldown[i]--;
        s.energy = Math.min(GojoState.MAX_ENERGY, s.energy + (s.awakened() ? 0.5f : 0.15f));
        if (s.awakenTicks > 0) {
            s.awakenTicks--;
            if (p.tickCount % 20 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            }
            if (s.awakenTicks == 0) {
                s.sixEyes = 0;
                for (int i = 4; i < 8; i++) s.cooldown[i] = 0;
            }
        }
        if (p.tickCount % 4 == 0) sync(p);
    }

    // ---------------------------------------------------------------- entrada

    public static void use(ServerPlayer p, int id) {
        GojoState s = state(p);
        if (!s.active || !p.isAlive() || STUNNED.contains(p.getUUID())) return;
        if (id == 4) {
            awaken(p, s);
            return;
        }
        if (id < 0 || id > 3) return;

        boolean aw = s.awakened();
        int slot = id + (aw ? 4 : 0);
        int cost = (aw ? GojoState.AWAKE_COST : GojoState.BASE_COST)[id];
        int cd = (aw ? GojoState.AWAKE_CD : GojoState.BASE_CD)[id];
        if (s.cooldown[slot] > 0 || s.energy < cost) return;

        s.energy -= cost;
        s.cooldown[slot] = cd;
        ServerLevel l = p.serverLevel();

        boolean ok;
        if (!aw) {
            ok = switch (id) {
                case 0 -> lapseBlue(p, l);
                case 1 -> reversalRed(p, l);
                case 2 -> rapidPunches(p, l);
                case 3 -> twofoldKick(p, l);
                default -> false;
            };
        } else {
            ok = switch (id) {
                case 0 -> blueMax(p, l);
                case 1 -> redMax(p, l);
                case 2 -> hollowPurple(p, l);
                case 3 -> infiniteVoid(p, l);
                default -> false;
            };
        }
        if (!ok) { // no había objetivo: se devuelve el costo
            s.energy = Math.min(GojoState.MAX_ENERGY, s.energy + cost);
            s.cooldown[slot] = 0;
        }
        sync(p);
    }

    private static void awaken(ServerPlayer p, GojoState s) {
        if (s.awakened() || s.sixEyes < GojoState.MAX_SIX_EYES) return;
        s.awakenTicks = GojoState.AWAKEN_TICKS;
        s.energy = GojoState.MAX_ENERGY;
        p.displayClientMessage(Component.literal("Six Eyes"), true);
        ServerLevel l = p.serverLevel();
        Vec3 c = p.position().add(0, 1, 0);
        l.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 60, 0.6, 0.9, 0.6, 0.15);
        l.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        l.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.6f);
        sync(p);
    }

    // ---------------------------------------------------------------- set base

    /** Orbe azul: atrae y al final explota hacia afuera. */
    private static boolean lapseBlue(ServerPlayer p, ServerLevel l) {
        LivingEntity t = findTarget(p, 12);
        final Vec3 c = t != null ? t.getBoundingBox().getCenter()
                : p.getEyePosition().add(p.getLookAngle().scale(4));
        l.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 0.6f);
        Tasks.repeat(20, tick -> {
            sphere(l, BLUE, c, 1.0 + 0.02 * tick, 24);
            l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 3, 0.3, 0.3, 0.3, 0.02);
            pull(p, l, c, 6, 0.3);
            if (tick % 5 == 0) for (LivingEntity e : nearby(l, p, c, 2.0)) hit(p, e, 1.0f);
            if (tick == 19) {
                for (LivingEntity e : nearby(l, p, c, 4.5)) {
                    hit(p, e, 5f);
                    push(e, horiz(c, e.position()), 1.5, 0.5);
                }
                l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                l.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1f, 1.2f);
                carve(l, BlockPos.containing(c), 2);
            }
        });
        return true;
    }

    /** Tentáculos rojos que llegan al objetivo y explotan a su alrededor. */
    private static boolean reversalRed(ServerPlayer p, ServerLevel l) {
        LivingEntity t = findTarget(p, 20);
        final Vec3 start = p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.3, 0);
        final Vec3 end = t != null ? t.getBoundingBox().getCenter()
                : p.getEyePosition().add(p.getLookAngle().scale(14));
        l.playSound(null, start.x, start.y, start.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 0.5f);
        Tasks.repeat(8, tick -> line(l, RED, start, start.lerp(end, (tick + 1) / 8.0), 0.35));
        Tasks.later(8, () -> {
            for (LivingEntity e : nearby(l, p, end, 3.5)) {
                hit(p, e, 7f);
                push(e, horiz(end, e.position()), 1.6, 0.7);
            }
            sphere(l, RED, end, 2.5, 60);
            l.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 4, 1, 1, 1, 0);
            l.playSound(null, end.x, end.y, end.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1f, 0.9f);
            carve(l, BlockPos.containing(end), 2);
        });
        return true;
    }

    /** Ráfaga de golpes (necesita objetivo cerca; hace un dash hacia él). */
    private static boolean rapidPunches(ServerPlayer p, ServerLevel l) {
        LivingEntity t = findTarget(p, 6);
        if (t == null) return false;
        dashTo(p, t);
        Tasks.repeat(16, tick -> {
            if (!t.isAlive() || !p.isAlive()) return;
            if (tick % 2 == 0) {
                hit(p, t, 1.2f);
                p.swing(tick % 4 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
                Vec3 c = t.getBoundingBox().getCenter();
                l.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.2);
                l.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1f);
            }
            if (tick == 15) push(t, horiz(p.position(), t.position()), 1.4, 0.3);
        });
        return true;
    }

    /** Dos patadas; la segunda lanza al enemigo por los aires. */
    private static boolean twofoldKick(ServerPlayer p, ServerLevel l) {
        LivingEntity t = findTarget(p, 6);
        if (t == null) return false;
        dashTo(p, t);
        Tasks.later(2, () -> {
            if (!t.isAlive()) return;
            hit(p, t, 3f);
            push(t, horiz(p.position(), t.position()), 0.4, 0.2);
            p.swing(InteractionHand.MAIN_HAND, true);
            Vec3 c = t.getBoundingBox().getCenter();
            l.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.2);
            l.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 1f);
        });
        Tasks.later(8, () -> {
            if (!t.isAlive()) return;
            hit(p, t, 4f);
            t.setDeltaMovement(0, 1.3, 0);
            t.hurtMarked = true;
            p.swing(InteractionHand.OFF_HAND, true);
            Vec3 c = t.getBoundingBox().getCenter();
            l.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            l.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.8f);
        });
        return true;
    }

    // ---------------------------------------------------------------- Six Eyes

    /** Orbe gigante que arrastras con la mirada y se come todo lo que toca. */
    private static boolean blueMax(ServerPlayer p, ServerLevel l) {
        final Orb orb = new Orb();
        final UUID id = p.getUUID();
        BLUE_MAX.put(id, orb);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.4f);
        Tasks.repeat(60, tick -> {
            if (!orb.alive || !p.isAlive()) return;
            Vec3 c = p.getEyePosition().add(p.getLookAngle().scale(7));
            orb.pos = c;
            sphere(l, BLUE, c, 2.5, 40);
            sphere(l, BLUE, c, 1.5, 20);
            l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 6, 0.8, 0.8, 0.8, 0.05);
            pull(p, l, c, 14, 0.5);
            if (tick % 4 == 0) for (LivingEntity e : nearby(l, p, c, 3.0)) hit(p, e, 1.5f);
            if (tick % 6 == 0) carve(l, BlockPos.containing(c), 3);
        });
        Tasks.later(60, () -> {
            orb.alive = false;
            if (BLUE_MAX.get(id) == orb) BLUE_MAX.remove(id);
        });
        return true;
    }

    /** Rayo rojo. Si cruza un Blue MAX activo, se fusionan en Hollow Purple. */
    private static boolean redMax(ServerPlayer p, ServerLevel l) {
        final Vec3 start = p.getEyePosition();
        final Vec3 dir = p.getLookAngle();
        final UUID id = p.getUUID();
        final Orb blue = BLUE_MAX.get(id);
        final Set<LivingEntity> hit = new HashSet<>();
        l.playSound(null, start.x, start.y, start.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1f, 1.5f);
        for (double d = 1; d <= 30; d += 1.0) {
            Vec3 q = start.add(dir.scale(d));
            if (blue != null && blue.alive && q.distanceTo(blue.pos) < 3.5) {
                blue.alive = false;
                BLUE_MAX.remove(id);
                launchPurple(l, p, blue.pos, dir);
                return true;
            }
            sphere(l, RED, q, 0.8, 8);
            for (LivingEntity e : nearby(l, p, q, 1.8)) {
                if (hit.add(e)) {
                    hit(p, e, 9f);
                    push(e, horiz(start, e.position()), 1.2, 0.5);
                }
            }
            carve(l, BlockPos.containing(q), 1);
        }
        return true;
    }

    /** Carga (azul + rojo se juntan) y dispara el orbe morado. */
    private static boolean hollowPurple(ServerPlayer p, ServerLevel l) {
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2f, 0.5f);
        Tasks.repeat(20, tick -> {
            if (!p.isAlive()) return;
            Vec3 look = p.getLookAngle();
            Vec3 c = p.getEyePosition().add(look.scale(3));
            Vec3 right = look.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : right.normalize();
            double off = 1.8 * (1 - tick / 20.0);
            sphere(l, BLUE, c.add(right.scale(off)), 0.7, 14);
            sphere(l, RED, c.subtract(right.scale(off)), 0.7, 14);
            if (tick == 19) {
                sphere(l, PURPLE, c, 1.2, 40);
                launchPurple(l, p, c, look);
            }
        });
        return true;
    }

    private static void launchPurple(ServerLevel l, ServerPlayer owner, Vec3 start, Vec3 dir) {
        final Set<LivingEntity> hit = new HashSet<>();
        l.playSound(null, start.x, start.y, start.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2f, 0.6f);
        Tasks.repeat(30, tick -> {
            Vec3 c = start.add(dir.scale(2.0 * (tick + 1)));
            sphere(l, PURPLE, c, 1.6, 30);
            sphere(l, PURPLE, c, 0.8, 12);
            l.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 10, 1, 1, 1, 0.5);
            for (LivingEntity e : nearby(l, owner, c, 3.0)) {
                if (hit.add(e)) {
                    hit(owner, e, 24f);
                    push(e, dir.multiply(1, 0, 1).lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.multiply(1, 0, 1).normalize(), 1.8, 0.6);
                }
            }
            carve(l, BlockPos.containing(c), 3);
            if (tick == 29) l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        });
    }

    /** Expansión de dominio: quienes estén dentro quedan inmóviles, sin habilidades y reciben daño constante. */
    private static boolean infiniteVoid(ServerPlayer p, ServerLevel l) {
        final Vec3 c = p.position();
        final double radius = 12;
        final Set<UUID> stunnedHere = new HashSet<>();
        l.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        l.playSound(null, c.x, c.y, c.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2f, 0.7f);
        Tasks.repeat(DOMAIN_TICKS, tick -> {
            sphere(l, WHITE, c.add(0, 1, 0), radius, 80);
            l.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 4, radius / 2, radius / 2, radius / 2, 0.02);
            Set<UUID> inside = new HashSet<>();
            for (LivingEntity e : nearby(l, p, c, radius)) {
                if (tick % 10 == 0) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 10, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 10, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
                }
                e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
                e.hurtMarked = true;
                if (tick % 20 == 0) hit(p, e, 2f);
                if (e instanceof ServerPlayer sp) {
                    inside.add(sp.getUUID());
                    STUNNED.add(sp.getUUID());
                }
            }
            for (UUID u : new ArrayList<>(stunnedHere)) {
                if (!inside.contains(u)) {
                    STUNNED.remove(u);
                    stunnedHere.remove(u);
                }
            }
            stunnedHere.addAll(inside);
        });
        Tasks.later(DOMAIN_TICKS, () -> {
            STUNNED.removeAll(stunnedHere);
            stunnedHere.clear();
        });
        return true;
    }

    // ---------------------------------------------------------------- utilidades

    private static void hit(ServerPlayer owner, LivingEntity t, float dmg) {
        t.invulnerableTime = 0; // permite golpes seguidos
        t.hurt(owner.damageSources().playerAttack(owner), dmg);
    }

    private static Vec3 horiz(Vec3 from, Vec3 to) {
        Vec3 d = new Vec3(to.x - from.x, 0, to.z - from.z);
        return d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
    }

    private static void push(LivingEntity e, Vec3 dir, double h, double v) {
        e.setDeltaMovement(dir.x * h, v, dir.z * h);
        e.hurtMarked = true;
    }

    private static void dashTo(ServerPlayer p, LivingEntity t) {
        Vec3 dir = horiz(p.position(), t.position());
        Vec3 pos = t.position().subtract(dir.scale(1.6));
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * 180.0 / Math.PI) - 90.0f;
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        p.connection.teleport(pos.x, t.getY(), pos.z, yaw, 0f);
    }

    /** Entidad viva más cercana dentro del cono de mira. */
    private static LivingEntity findTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        LivingEntity best = null;
        double bestD = range * range;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive())) {
            Vec3 to = e.getEyePosition().subtract(eye);
            double d = to.lengthSqr();
            if (d > bestD || to.normalize().dot(look) < 0.9) continue;
            best = e;
            bestD = d;
        }
        return best;
    }

    private static List<LivingEntity> nearby(ServerLevel l, LivingEntity owner, Vec3 c, double r) {
        return l.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e != owner && e.isAlive() && e.getBoundingBox().getCenter().distanceToSqr(c) <= r * r);
    }

    private static void pull(ServerPlayer owner, ServerLevel l, Vec3 c, double radius, double strength) {
        for (LivingEntity e : nearby(l, owner, c, radius)) {
            Vec3 d = c.subtract(e.getBoundingBox().getCenter());
            if (d.length() < 0.3) continue;
            e.setDeltaMovement(e.getDeltaMovement().scale(0.4).add(d.normalize().scale(strength)));
            e.hurtMarked = true;
        }
    }

    private static void sphere(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n) {
        RandomSource rnd = l.random;
        for (int i = 0; i < n; i++) {
            double x = rnd.nextGaussian(), y = rnd.nextGaussian(), z = rnd.nextGaussian();
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len < 1e-4) continue;
            l.sendParticles(o, c.x + x / len * r, c.y + y / len * r, c.z + z / len * r, 1, 0, 0, 0, 0);
        }
    }

    private static void line(ServerLevel l, ParticleOptions o, Vec3 a, Vec3 b, double step) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-4) return;
        Vec3 dir = d.scale(1 / len);
        for (double i = 0; i < len; i += step) {
            Vec3 q = a.add(dir.scale(i));
            l.sendParticles(o, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    /** Borra una esfera de bloques (ignora bedrock y bloques con inventario). */
    private static void carve(ServerLevel l, BlockPos c, int r) {
        if (!DESTROY_BLOCKS || !l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    BlockPos bp = c.offset(dx, dy, dz);
                    BlockState st = l.getBlockState(bp);
                    if (st.isAir() || st.getDestroySpeed(l, bp) < 0 || st.getBlock() instanceof EntityBlock) continue;
                    l.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
                }
    }
}
