package io.github.poorgrammerdev.ominouswither.internal.config;

public enum BossStat {
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
    APOCALYPSE_HORSEMAN_ARMOR_PROTECTION("boss_stats.apocalypse_horseman_armor_protection"),
    APOCALYPSE_HORSEMAN_BOW_POWER("boss_stats.apocalypse_horseman_bow_power"),
    APOCALYPSE_HORSE_SPEED("boss_stats.apocalypse_horse_speed"),
    APOCALYPSE_HORSE_MOVEMENT_EFFICIENCY("boss_stats.apocalypse_horse_movement_efficiency"),
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
    ENHANCED_BREAK_RANGE("boss_stats.enhanced_break_range"),
    ENHANCED_BREAK_HEIGHT("boss_stats.enhanced_break_height"),
    ENHANCED_BREAK_INTERVAL("boss_stats.enhanced_break_interval"),
    LIFE_DRAIN_COOLDOWN("boss_stats.life_drain_cooldown"),
    LIFE_DRAIN_RANGE("boss_stats.life_drain_range"),
    LIFE_DRAIN_SPEED_THRESHOLD("boss_stats.life_drain_speed_threshold"),
    LIFE_DRAIN_STARTUP_TIME("boss_stats.life_drain_startup_time"),
    LIFE_DRAIN_LIFESPAN("boss_stats.life_drain_lifespan"),
    LIFE_DRAIN_ATTACK_INTERVAL("boss_stats.life_drain_attack_interval"),
    LIFE_DRAIN_HORIZONTAL_RADIUS("boss_stats.life_drain_horizontal_radius"),
    LIFE_DRAIN_VERTICAL_RADIUS("boss_stats.life_drain_vertical_radius"),




    
    ;

    private final String configPath;

    private BossStat(final String configPath) {
        this.configPath = configPath;
    }

    public String getConfigPath() {return this.configPath;}
}
