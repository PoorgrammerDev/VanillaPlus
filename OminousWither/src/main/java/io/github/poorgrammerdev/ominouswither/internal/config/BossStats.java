package io.github.poorgrammerdev.ominouswither.internal.config;

public enum BossStats {
    NORMAL_SKULL_SPEED("boss_stats.normal_skull_speed"),
    EXPLOSIVE_SKULL_SPEED("boss_stats.explosive_skull_speed"),
    APOCALYPSE_SKULL_SPEED("boss_stats.apocalypse_skull_speed"),
    GRAVITY_SKULL_SPEED("boss_stats.gravity_skull_speed"),
    DANGEROUS_SKULL_LIFESPAN("boss_stats.dangerous_skull_lifespan"),
    APOCALYPSE_HOMING_LIFESPAN("boss_stats.apocalypse_homing_lifespan"),
    EXPLOSIVE_SKULL_POWER("boss_stats.explosive_skull_power"),
    GRAVITY_RADIUS("boss_stats.gravity_radius"),
    GRAVITY_FORCE_INTENSITY("boss_stats.gravity_force_intensity"),
    APOCALYPSE_SPAWN_AMOUNT("boss_stats.apocalypse_spawn_amount"),
    APOCALYPSE_HORSEMAN_LIFESPAN("boss_stats.apocalypse_horseman_lifespan"),
    DANGEROUS_SKULL_CHANCE_BOOST("boss_stats.dangerous_skull_chance_boost"),
    GENERAL_EXPLOSION_RESISTANCE("boss_stats.general_explosion_resistance"),
    END_CRYSTAL_RESISTANCE("boss_stats.end_crystal_resistance"),
    FLIGHT_ACCELERATION_DISTANCE_THRESHOLD("boss_stats.flight_acceleration_distance_threshold"),
    FLIGHT_SPEED("boss_stats.flight_speed"),
    SUFFOCATE_TELEPORT_RANGE("boss_stats.suffocate_teleport_range"),
    FIRST_PHASE_ARMOR("boss_stats.first_phase_armor"),
    SECOND_PHASE_ARMOR("boss_stats.second_phase_armor"),
    FIRST_PHASE_ARMOR_TOUGHNESS("boss_stats.first_phase_armor_toughness"),
    SECOND_PHASE_ARMOR_TOUGHNESS("boss_stats.second_phase_armor_toughness"),
    SKULL_BARRAGE_AMOUNT("boss_stats.skull_barrage_amount"),
    BOSS_MAX_HEALTH("boss_stats.boss_max_health"),
    MINION_AMOUNT("boss_stats.minion_amount"),
    MINION_SPAWN_RANGE("boss_stats.minion_spawn_range"),
    MINION_ARMOR("boss_stats.minion_armor"),
    MINION_ARMOR_TOUGHNESS("boss_stats.minion_armor_toughness"),
    MINION_MOVEMENT_SPEED("boss_stats.minion_movement_speed"),
    MINION_SWORD_SHARPNESS("boss_stats.minion_sword_sharpness"), 
    ;

    private final String configPath;

    private BossStats(final String configPath) {
        this.configPath = configPath;
    }

    public String getConfigPath() {return this.configPath;}
}
