package net.ua.util;

import net.minecraft.world.Difficulty;

import java.util.function.Predicate;

public class Variables {
    public static final Predicate<Difficulty> DIFFICULTY_ALLOWS_DOOR_BREAKING_PREDICATE = difficulty -> difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD;
}