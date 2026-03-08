package ch.njol.skript.core;

import ch.njol.skript.platform.SkriptLocation;
import ch.njol.skript.platform.SkriptPlayerInfo;

/**
 * Event context passed from platform adapters into the core runtime when
 * firing events via {@link ch.njol.skript.core.SkriptBootstrap#fireEvent(String, RuntimeEventContext)}.
 * Platforms can attach an optional player and/or location for use by script logic.
 */
public final class RuntimeEventContext {

    private final String description;
    private final String subject;
    private final SkriptPlayerInfo player;
    private final SkriptLocation location;
    private final Object entity;

    public RuntimeEventContext(String description, String subject) {
        this(description, subject, null, null, null);
    }

    public RuntimeEventContext(String description, String subject,
                               SkriptPlayerInfo player, SkriptLocation location) {
        this(description, subject, player, location, null);
    }

    public RuntimeEventContext(String description, String subject,
                               SkriptPlayerInfo player, SkriptLocation location, Object entity) {
        this.description = description;
        this.subject = subject;
        this.player = player;
        this.location = location;
        this.entity = entity;
    }

    public String getDescription() {
        return description;
    }

    public String getSubject() {
        return subject;
    }

    /** Optional player involved in the event (e.g. join, quit). */
    public SkriptPlayerInfo getPlayer() {
        return player;
    }

    /** Optional location involved in the event (e.g. block break). */
    public SkriptLocation getLocation() {
        return location;
    }

    /** Optional entity involved in the event. Platform-specific (e.g. net.minecraft.world.entity.Entity). */
    public Object getEntity() {
        return entity;
    }
}

