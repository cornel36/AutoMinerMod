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
}
