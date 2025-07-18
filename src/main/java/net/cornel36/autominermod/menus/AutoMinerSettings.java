package net.cornel36.autominermod.menus;

public class AutoMinerSettings {

    public enum Mode {
        AREA,
        STRAIGHT,
        MOB_GRINDER
    }

    private static Mode currentMode = Mode.AREA;

    public static Mode getMode() {
        return currentMode;
    }

    public static void setMode(Mode mode) {
        currentMode = mode;
        System.out.println("AutoMiner mode set to " + mode);
    }

    private static boolean straightMiningWalkForward = true;

    public static boolean isStraightMiningWalkForward() {
        return straightMiningWalkForward;
    }

    public static void setStraightMiningWalkForward(boolean value) {
        straightMiningWalkForward = value;
        System.out.println("Walk Forward set to " + value);
    }

    public enum MobGrinderType {
        HOSTILE_ONLY,
        ALL
    }

    private static MobGrinderType mobGrinderType = MobGrinderType.HOSTILE_ONLY;

    public static MobGrinderType getMobGrinderModeType() {
        return mobGrinderType;
    }

    public static void setMobGrinderModeType(MobGrinderType type) {
        mobGrinderType = type;
    }
}
