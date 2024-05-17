package twomillions.plugin.advancedwish.utils.others;

import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.utils.exceptions.ExceptionUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 物品工具类，提供了一些有关物品的静态方法。
 *
 * @author 2000000
 * @date 2022/12/15 13:47
 */
@UtilityClass
@JsInteropJavaType
@SuppressWarnings("unused")
public class ItemUtils {
    /**
     * 将字符串转换为 Material 对象，如果字符串无法转换则发送错误信息并返回 Material.AIR。
     *
     * @param materialString 要转换的字符串，不支持 null
     * @param fileName 调用该方法的文件名或类名，不支持 null
     * @return 对应的 Material，或 Material.AIR 如果字符串无法转换
     * @throws NullPointerException 如果 materialString 或 fileName 为 null
     */
    public static Material materialValueOf(String materialString, String fileName) throws NullPointerException {
        try {
            return Material.valueOf(materialString.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            ExceptionUtils.sendUnknownWarn("物品", fileName, materialString);
            return Material.AIR;
        }
    }

    /**
     * 将字符串转换为 Material 对象，如果字符串无法转换则发送错误信息并返回 Material.AIR。
     *
     * @param materialString 要转换的字符串，不支持 null
     * @return 对应的 Material，或 Material.AIR 如果字符串无法转换
     * @throws NullPointerException 如果 materialString 为 null
     */
    public static Material materialValueOf(String materialString) throws NullPointerException {
        try {
            return Material.valueOf(materialString.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Material.AIR;
        }
    }

    /**
     * 从玩家背包中移除指定的物品数量。
     *
     * @param player 要移除物品的玩家
     * @param itemStack 要移除的物品 ItemStack
     * @param removeAmount 要移除的数量
     * @return 是否成功移除了指定的数量的物品
     */
    public static boolean removeItems(Player player, ItemStack itemStack, int removeAmount) {
        ConcurrentLinkedQueue<ItemStack> toRemove = Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(is -> is.isSimilar(itemStack))
                .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

        /*
         * 背包中没有足够的物品
         */
        if (toRemove.isEmpty() || toRemove.stream().mapToInt(ItemStack::getAmount).sum() < removeAmount) {
            return false;
        }

        int removedAmount = toRemove.stream()
                .limit(removeAmount)
                .mapToInt(ItemStack::getAmount)
                .sum();

        /*
         * 背包中没有足够的物品
         */
        if (removedAmount < removeAmount) {
            return false;
        }

        for (ItemStack stack : toRemove) {
            int amount = Math.min(stack.getAmount(), removeAmount);

            stack.setAmount(stack.getAmount() - amount);
            removeAmount -= amount;

            if (stack.getAmount() <= 0) {
                player.getInventory().remove(stack);
            }

            if (removeAmount <= 0) {
                break;
            }
        }

        return true;
    }
}
