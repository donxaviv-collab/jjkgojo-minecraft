package com.jjkshenanigans.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

/** Mini planificador de ticks del servidor (para animar habilidades sin crear entidades). */
public final class Tasks {
    private record Task(long runAt, Runnable r) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now = 0;

    private Tasks() {}

    /** Ejecuta r dentro de `ticks` ticks. */
    public static void later(int ticks, Runnable r) {
        TASKS.add(new Task(now + ticks, r));
    }

    /** Ejecuta r una vez por tick durante `duration` ticks; recibe el índice del tick (0..duration-1). */
    public static void repeat(int duration, IntConsumer r) {
        for (int i = 0; i < duration; i++) {
            final int t = i;
            later(i, () -> r.accept(t));
        }
    }

    public static void tick() {
        now++;
        List<Task> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.runAt() <= now) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            try {
                t.r().run();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }
}
