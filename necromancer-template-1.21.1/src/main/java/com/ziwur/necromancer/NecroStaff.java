package necromancer.modid;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SoulTomeItem extends Item {

    private int spawnMode = 0;
    private boolean targetPlayers = false;

    public SoulTomeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (!world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) world;

            if (user.isSneaking()) {
                if (user.getPitch() < -30.0f) {
                    this.targetPlayers = !this.targetPlayers;
                    String targetText = this.targetPlayers ? "Atacar Jugadores Enemigos" : "Atacar Solo Monstruos";
                    user.sendMessage(Text.literal("Modo de objetivo: " + targetText).formatted(Formatting.RED), true);
                } else {
                    this.spawnMode = (this.spawnMode == 0) ? 1 : 0;
                    String mobText = (this.spawnMode == 0) ? "Esqueleto" : "Zombi";
                    user.sendMessage(Text.literal("Invocación seleccionada: " + mobText).formatted(Formatting.DARK_PURPLE), true);
                }

                world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                return TypedActionResult.success(itemStack);
            }

            EntityType<?> entityToSpawn = (this.spawnMode == 0) ? EntityType.SKELETON : EntityType.ZOMBIE;
            MobEntity entityCreated = (MobEntity) entityToSpawn.spawn(serverWorld, user.getBlockPos().offset(user.getHorizontalFacing(), 2), SpawnReason.MOB_SUMMONED);

            if (entityCreated != null) {
                entityCreated.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));

                if (this.targetPlayers) {
                    entityCreated.targetSelector.add(1, new ActiveTargetGoal<>(
                        entityCreated, 
                        PlayerEntity.class, 
                        true, 
                        p -> p != user && !p.isCreative() && !p.isSpectator()
                    ));
                } else {
                    entityCreated.targetSelector.add(1, new ActiveTargetGoal<>(
                        entityCreated, 
                        HostileEntity.class, 
                        true
                    ));
                }

                entityCreated.setPersistent();
            }

            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.4f, 0.8f);
            user.getItemCooldownManager().set(this, 40);
        }

        return TypedActionResult.success(itemStack);
    }
}