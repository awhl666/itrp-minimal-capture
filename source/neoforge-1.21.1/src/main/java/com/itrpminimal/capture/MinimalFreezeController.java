package com.itrpminimal.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class MinimalFreezeController {
    private static boolean frozen = false;
    private static boolean celestialPause = true;
    private static long frozenDayTime = 0L;
    private static long frozenGameTime = 0L;
    private static float frozenPartialTick = 0.0F;
    private static String reason = "none";

    public static void freeze(String why) {
        if (!frozen) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                frozenDayTime = level.getDayTime();
                frozenGameTime = level.getGameTime();
            }
        }
        frozen = true;
        reason = why == null ? "unspecified" : why;
        MinimalLog.line("FREEZE enabled " + summary());
    }

    public static void unfreeze(String why) {
        if (!frozen) {
            return;
        }
        frozen = false;
        reason = why == null ? "unspecified" : why;
        MinimalLog.line("FREEZE disabled reason=" + reason);
    }

    public static boolean isFrozen() {
        return frozen;
    }

    public static boolean celestialPauseEnabled() {
        return celestialPause;
    }

    public static boolean shouldFreezeCelestial() {
        return frozen && celestialPause;
    }

    public static void setCelestialPauseEnabled(boolean enabled) {
        celestialPause = enabled;
        MinimalLog.line("FREEZE celestialPause=" + celestialPause);
    }

    public static long frozenDayTime() {
        return frozenDayTime;
    }

    public static long frozenGameTime() {
        return frozenGameTime;
    }

    public static float frozenPartialTick() {
        return frozenPartialTick;
    }

    public static float frozenTimeOfDay(float partialTick) {
        long time = frozenDayTime();
        double value = Math.floorMod(time, 24000L) / 24000.0D - 0.25D;
        value = value - Math.floor(value);
        return (float) value;
    }

    public static float frozenSunAngle(float partialTick) {
        return frozenTimeOfDay(partialTick) * 6.2831855F;
    }

    public static void rememberPartialTick(float partialTick) {
        if (!frozen) {
            frozenPartialTick = partialTick;
        }
    }

    public static String summary() {
        return "frozen=" + frozen + " celestialPause=" + celestialPause + " dayTime=" + frozenDayTime() + " gameTime=" + frozenGameTime() + " partialTick=" + frozenPartialTick + " reason=" + reason;
    }

    private MinimalFreezeController() {
    }
}
