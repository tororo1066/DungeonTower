package tororo1066.dungeontower.data

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootContext
import org.bukkit.loot.LootTable
import tororo1066.dungeontower.DungeonTower
import tororo1066.tororopluginapi.sItem.SItem
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class LootData: LootTable, Cloneable {
    var includeName = ""
    val items = ArrayList<Triple<Int,IntRange,SItem>>()//確率、個数、ItemStack
    var rollAmount = 0

    override fun populateLoot(random: Random, context: LootContext): MutableCollection<ItemStack> {

        val returnItems = ArrayList<ItemStack>()



        first@
        for (i in 0..rollAmount) {
            val randomNum = random.nextInt(999999) + 1
            var preventRandom = 0
            for (item in items){
                if (preventRandom < randomNum && item.first + preventRandom > randomNum){
                    val sumAmount = item.second.random()
                    val stackAmount = sumAmount / 64
                    val amount = sumAmount % 64
                    (1..stackAmount).forEach {
                        returnItems.add(item.third.clone().setItemAmount(64))
                    }
                    Bukkit.broadcastMessage(item.third.getDisplayName())
                    returnItems.add(item.third.clone().setItemAmount(amount))
                    continue@first
                }
                preventRandom = item.first
            }
        }
        return returnItems
    }

    override fun fillInventory(inventory: Inventory, random: Random, context: LootContext) {
        inventory.setContents(populateLoot(random, context).toTypedArray())
        inventory.shuffled(random)
    }

    override fun getKey(): NamespacedKey {
        return NamespacedKey(DungeonTower.plugin,includeName)
    }

    public override fun clone(): LootData {
        return super.clone() as LootData
    }

    companion object{
        fun loadFromYml(yml: YamlConfiguration): HashMap<String, LootData> {
            val dataMap = HashMap<String,LootData>()
            for (key in yml.getKeys(false)){
                val section = yml.getConfigurationSection(key)!!
                val data = LootData()
                data.includeName = key
                data.rollAmount = section.getInt("roll")
                val items = section.getList("items") as List<ItemStack>
                val amounts = section.getStringList("amounts").map { it.split("to")[0].toInt()..it.split("to")[1].toInt() }
                val chances = section.getIntegerList("chances")
                for (i in items.indices){
                    data.items.add(Triple(chances[i],amounts[i],SItem(items[i])))
                }

                dataMap[key] = data
            }
            return dataMap
        }
    }
}