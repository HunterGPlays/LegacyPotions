/**
 * https://github.com/MinelinkNetwork/LegacyStrength
 * @author Byteflux
 */
package net.terrocidepvp.legacypotions.handlers;

import com.google.common.collect.ImmutableMap;
import net.terrocidepvp.legacypotions.LegacyPotions;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class StrengthHandler {

    private static Map<Material, Double> baseDamageValues = ImmutableMap.<Material, Double> builder()
            // Swords
            .put(Material.WOOD_SWORD, 5.0).put(Material.GOLD_SWORD, 5.0).put(Material.STONE_SWORD, 6.0)
            .put(Material.IRON_SWORD, 7.0).put(Material.DIAMOND_SWORD, 8.0)

            // Axes
            .put(Material.WOOD_AXE, 4.0).put(Material.GOLD_AXE, 4.0).put(Material.STONE_AXE, 5.0)
            .put(Material.IRON_AXE, 6.0).put(Material.DIAMOND_AXE, 7.0)

            // Pickaxes
            .put(Material.WOOD_PICKAXE, 3.0).put(Material.GOLD_PICKAXE, 3.0).put(Material.STONE_PICKAXE, 4.0)
            .put(Material.IRON_PICKAXE, 5.0).put(Material.DIAMOND_PICKAXE, 6.0)

            // Shovels
            .put(Material.WOOD_SPADE, 2.0).put(Material.GOLD_SPADE, 2.0).put(Material.STONE_SPADE, 3.0)
            .put(Material.IRON_SPADE, 4.0).put(Material.DIAMOND_SPADE, 5.0).build();

    private static Map<Material, Double> criticalDamageValues = ImmutableMap.<Material, Double> builder()
            // Swords
            .put(Material.WOOD_SWORD, 7.5).put(Material.GOLD_SWORD, 7.5).put(Material.STONE_SWORD, 9.0)
            .put(Material.IRON_SWORD, 10.5).put(Material.DIAMOND_SWORD, 12.0)

            // Axes
            .put(Material.WOOD_AXE, 6.0).put(Material.GOLD_AXE, 6.0).put(Material.STONE_AXE, 7.5)
            .put(Material.IRON_AXE, 9.0).put(Material.DIAMOND_AXE, 10.5)

            // Pickaxes
            .put(Material.WOOD_PICKAXE, 4.5).put(Material.GOLD_PICKAXE, 4.5).put(Material.STONE_PICKAXE, 6.0)
            .put(Material.IRON_PICKAXE, 7.5).put(Material.DIAMOND_PICKAXE, 9.0)

            // Shovels
            .put(Material.WOOD_SPADE, 3.0).put(Material.GOLD_SPADE, 3.0).put(Material.STONE_SPADE, 4.5)
            .put(Material.IRON_SPADE, 6.0).put(Material.DIAMOND_SPADE, 7.5).build();

    // Convert the initial damage into the damage.
    public static double convertDamage(Player player, double initialDamage) {
        StrengthHandler helper = new StrengthHandler();

        helper.initialDamage = initialDamage;
        helper.setPlayer(player);

        return helper.getFinalDamage();
    }

    private int damagePerLevel = LegacyPotions.getInstance().damagePerLevel;

    private double initialDamage = 0.0;

    private double baseDamage = 1.0;

    private double criticalDamage = 0.0;

    private double extraDamage = 0.0;

    private double baseStrengthDamage = 0.0;

    private double criticalStrengthDamage = 0.0;

    private double weaknessDamage = 0.0;

    private double finalDamage = 0.0;

    // Process the damage.
    private double getFinalDamage() {
        // Undo all the Vanilla math using base damage numbers
        double calculatedBase = initialDamage - extraDamage - baseStrengthDamage - weaknessDamage - baseDamage;

        // Undo all the Vanilla math using critical damage numbers
        double calculatedCritical = initialDamage - extraDamage - criticalStrengthDamage - weaknessDamage
                - criticalDamage;

        // Return damage using base damage numbers
        if (calculatedBase > -1.5 && calculatedBase < 1.5) {
            finalDamage += baseDamage;
            return finalDamage;
        }

        // Return damage using critical damage numbers
        if (calculatedCritical > -1.5 && calculatedCritical < 1.5) {
            finalDamage += criticalDamage;
            return finalDamage;
        }

        // Damage for unknown attack can't be determined
        return 0.0;
    }

    // Apply the effects to the player.
    @SuppressWarnings("deprecation")
    private void setPlayer(Player player) {
        // Apply weapon properties in damage calculations
        ItemStack weapon;
        if (LegacyPotions.getInstance().serverVersion[0] == 1
                && LegacyPotions.getInstance().serverVersion[1] >= 9) {
            weapon = player.getInventory().getItemInMainHand();
        } else {
            weapon = player.getInventory().getItemInHand();
        }

        if (weapon != null) {
            setWeapon(weapon);
        }

        // Apply strength and weakness effects in damage calculations
        for (PotionEffect e : player.getActivePotionEffects()) {
            if (e.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
                setStrength(e.getAmplifier());
            } else if (e.getType().equals(PotionEffectType.WEAKNESS)) {
                setWeakness(e.getAmplifier());
            }
        }
    }

    // Set the strength amount for the attack.
    private void setStrength(int amplifier) {
        // This is the Vanilla damage bonus that is added to a normal attack
        baseStrengthDamage = baseDamage * (amplifier + 1) * 1.3;

        // This is the Vanilla damage bonus that is added to a critical attack
        criticalStrengthDamage = criticalDamage * (amplifier + 1) * 1.3;

        // Add +3 or +6 to the damage depending on Strength level
        finalDamage += damagePerLevel << amplifier;
    }

    // Take into consideration the weakness effect.
    private void setWeakness(int amplifier) {
        // This is the vanilla damage reduction from Weakness
        weaknessDamage = (amplifier + 1) * -0.5;

        // Reduce damage by appropriate Weakness amount
        finalDamage += weaknessDamage;
    }

    // Find out the damage caused by the item.
    private void setWeapon(ItemStack item) {
        Material type = item.getType();

        // Find the base damage values for this weapon type
        //
        // The base damage is the weapon's damage alone, before any additional
        // damage like
        // Sharpness or Strength is applied.
        //
        // We need to determine this value for both the normal and critical
        // attack types.
        //
        // The base damage is necessary to determine how to undo all the math
        // that has been done
        // from the Strength effect.
        baseDamage = baseDamageValues.containsKey(type) ? baseDamageValues.get(type) : 1.0;
        criticalDamage = criticalDamageValues.containsKey(type) ? criticalDamageValues.get(type) : 0.0;

        // Do nothing if weapon doesn't have Sharpness
        if (!item.containsEnchantment(Enchantment.DAMAGE_ALL))
            return;

        // Calculate Sharpness damage
        extraDamage += item.getEnchantmentLevel(Enchantment.DAMAGE_ALL) * 1.25;

        // Add the calculated Sharpness damage to the damage
        finalDamage += extraDamage;
    }

}
