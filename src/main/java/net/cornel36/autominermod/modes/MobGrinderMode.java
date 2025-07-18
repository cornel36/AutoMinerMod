package net.cornel36.autominermod.modes;

import net.cornel36.autominermod.AutoMinerTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.cornel36.autominermod.menus.AutoMinerSettings;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MobGrinderMode implements MiningMode{
    private final MinecraftClient client;
    private AutoMinerTask.State state = AutoMinerTask.State.IDLE;

    private LivingEntity currentTarget = null;
    private int attackCooldown = 0;

    private int foodCheckDelay = 0;
    private static final int FOOD_CHECK_INTERVAL = 10;


    public MobGrinderMode(MinecraftClient client){
        this.client = client;
    }

    @Override
    public void start(BlockPos pos1, BlockPos pos2){
        stop();
        this.state = AutoMinerTask.State.GRINDING;
        this.currentTarget = null;
        this.attackCooldown = 0;
        this.foodCheckDelay = 0;
        System.out.println("Mob Grinder Mode started.");
    }

    @Override
    public void stop() {
        this.state = AutoMinerTask.State.IDLE;
        this.currentTarget = null;
        this.attackCooldown = 0;
        this.foodCheckDelay = 0;
        System.out.println("Mob Grinder Mode stopped.");
    }

    @Override
    public boolean isRunning() {
        return this.state == AutoMinerTask.State.GRINDING;
    }

    @Override
    public void tick() {
        if (client.player == null || client.world == null) return;
        if (state != AutoMinerTask.State.GRINDING) return;

        handleGrinderTick();
    }

    @Override
    public List<String> getDebugText() {
        List<String> lines = new ArrayList<>();
        lines.add("Mode: Mob Grinder");
        lines.add("State: " + state.name());
        lines.add("Target: " + (currentTarget != null ? currentTarget.getName().getString() : "None"));
        lines.add("Attack Cooldown: " + attackCooldown);
        return lines;
    }

    @Override
    public boolean isMoveForwardEnabled(){
        return false; //will not move in this mode
    }

    @Override
    public BlockPos getCurrentTarget() {
        return currentTarget != null ? currentTarget.getBlockPos() : null;
    }

    @Override
    public int getCurrentY() {
        return client.player != null ? client.player.getBlockY() : 0;
    }

    @Override
    public int getMinY() {
        return getCurrentY();
    }

    @Override
    public int getTotalBlockCount(){
        return 0; //doesnt mine
    }

    @Override
    public int getRemainingBlocks(){
        return 0; //doesnt mine
    }

    private void handleGrinderTick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        foodCheckDelay++;
        boolean isEating = false;

        if (foodCheckDelay >= FOOD_CHECK_INTERVAL) {
            foodCheckDelay = 0;
            isEating = tryEatFood();
        }
        if (!isEating && !client.player.isUsingItem()) {
            findAndAttackMob();
        }
    }

    private LivingEntity findNearestTarget() {
        if (client.player == null || client.world == null) return null;

        double range = 8.0;

        List<LivingEntity> potentialTargets = client.world.getEntitiesByClass(
                LivingEntity.class,
                client.player.getBoundingBox().expand(range),
                entity -> {
                    if (entity.equals(client.player)) return false;
                    if (!entity.isAlive()) return false;
                    if (entity.isSpectator() || entity.isInCreativeMode()) return false;

                    if (AutoMinerSettings.getMobGrinderModeType() == AutoMinerSettings.MobGrinderType.HOSTILE_ONLY) {
                        return entity instanceof HostileEntity;
                    } else {
                        return true;
                    }
                }
        );
        return potentialTargets.stream()
                .min(Comparator.comparingDouble(client.player::distanceTo))
                .orElse(null);
    }

    private void findAndAttackMob() {
        currentTarget = findNearestTarget();

        if (currentTarget != null) {
            lookAt(currentTarget.getEyePos());
            selectBestWeapon(currentTarget);

            if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                client.interactionManager.attackEntity(client.player, currentTarget);
                client.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }



    private boolean isValidWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        return stack.isOf(Items.WOODEN_SWORD) || stack.isOf(Items.STONE_SWORD) || stack.isOf(Items.IRON_SWORD) ||
                stack.isOf(Items.DIAMOND_SWORD) || stack.isOf(Items.NETHERITE_SWORD) ||
                stack.isOf(Items.WOODEN_AXE) || stack.isOf(Items.STONE_AXE) || stack.isOf(Items.IRON_AXE) ||
                stack.isOf(Items.DIAMOND_AXE) || stack.isOf(Items.NETHERITE_AXE);
    }
    private float getWeaponDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0f;

        if (stack.isOf(Items.WOODEN_SWORD)) return 4.0f;
        if (stack.isOf(Items.STONE_SWORD)) return 5.0f;
        if (stack.isOf(Items.IRON_SWORD)) return 6.0f;
        if (stack.isOf(Items.DIAMOND_SWORD)) return 7.0f;
        if (stack.isOf(Items.NETHERITE_SWORD)) return 8.0f;
        if (stack.isOf(Items.WOODEN_AXE)) return 7.0f;
        if (stack.isOf(Items.STONE_AXE)) return 7.0f;
        if (stack.isOf(Items.IRON_AXE)) return 9.0f;
        if (stack.isOf(Items.DIAMOND_AXE)) return 9.0f;
        if (stack.isOf(Items.NETHERITE_AXE)) return 10.0f;

        return 0f;
    }

    private void lookAt(Vec3d targetVec) {
        if (client.player == null) return;

        Vec3d eyePos = client.player.getCameraPosVec(1.0F);
        Vec3d delta = targetVec.subtract(eyePos);

        double distXZ = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, distXZ));

        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }

    private void selectBestWeapon(LivingEntity target) {
        if (client.player == null) return;

        int currentSlot = client.player.getInventory().getSelectedSlot();
        ItemStack currentStack = client.player.getInventory().getStack(currentSlot);

        if (isValidWeapon(currentStack)) {
            return;
        }

        int bestHotbarSlot = -1;
        float bestDamage = 0f;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (isValidWeapon(stack)) {
                float dmg = getWeaponDamage(stack);
                if (dmg > bestDamage) {
                    bestDamage = dmg;
                    bestHotbarSlot = i;
                }
            }
        }

        if (bestHotbarSlot != -1) {
            client.player.getInventory().setSelectedSlot(bestHotbarSlot);
            return;
        }

        int bestInventorySlot = -1;
        bestDamage = 0f;
        for (int i = 9; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (isValidWeapon(stack)) {
                float dmg = getWeaponDamage(stack);
                if (dmg > bestDamage) {
                    bestDamage = dmg;
                    bestInventorySlot = i;
                }
            }
        }

        if (bestInventorySlot != -1) {
            int hotbarSlotToUse = findEmptyHotbarSlot();
            if (hotbarSlotToUse == -1) {
                hotbarSlotToUse = 1;
            }

            client.player.getInventory().swapSlotWithHotbar(bestInventorySlot);
            client.player.getInventory().setSelectedSlot(hotbarSlotToUse);
        }
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private boolean tryEatFood() {
        if (client.player == null || client.player.isUsingItem()) return false;

        boolean needsFood = client.player.getHungerManager().getFoodLevel() < 19;
        boolean needsHealth = client.player.getHealth() < client.player.getMaxHealth();

        if (!needsFood && !needsHealth) return false;

        int bestFoodSlot = -1;
        float bestSaturation = 0.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            FoodComponent foodComponent = stack.getOrDefault(DataComponentTypes.FOOD, null);
            if (foodComponent == null) continue;

            if (stack.isOf(Items.ROTTEN_FLESH) || stack.isOf(Items.PUFFERFISH) ||
                    stack.isOf(Items.POISONOUS_POTATO) || stack.isOf(Items.SPIDER_EYE)) {
                continue;
            }

            float currentSaturation = foodComponent.saturation();
            if (currentSaturation > bestSaturation) {
                bestSaturation = currentSaturation;
                bestFoodSlot = i;
            }
        }

        if (bestFoodSlot != -1) {
            int previousSlot = client.player.getInventory().getSlotWithStack(client.player.getMainHandStack());
            client.player.getInventory().setSelectedSlot(bestFoodSlot);

            ItemStack mainHand = client.player.getMainHandStack();
            if (!mainHand.isEmpty() && mainHand.getOrDefault(DataComponentTypes.FOOD, null) != null) {
                client.options.useKey.setPressed(true);
                System.out.println("Started eating " + mainHand.getName().getString());
                return true;
            }
        } else {
            System.out.println("No suitable food found in hotbar.");
        }

        return false;
    }
}
