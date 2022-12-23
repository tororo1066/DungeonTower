package tororo1066.dungeontower.inventory

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import tororo1066.dungeontower.DungeonTower
import tororo1066.dungeontower.DungeonTower.Companion.sendPrefixMsg
import tororo1066.dungeontower.data.LootData
import tororo1066.dungeontower.data.SpawnerData
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem

class CreateLoot(val loot: LootData): LargeSInventory(DungeonTower.plugin,"ルートテーブルを作成する") {

    override fun renderMenu(): Boolean {
        val items = arrayListOf(
            createInputItem(SItem(Material.DIRT).setDisplayName("§a内部名を設定する").addLore("§d現在の値:§c${loot.includeName}"),String::class.java) { str, _ ->
                loot.includeName = str
            },
            createInputItem(SItem(Material.CLOCK).setDisplayName("§a抽選回数を設定する").addLore("§d現在の値:§c${loot.rollAmount}"),Int::class.java){ int, _ ->
                loot.rollAmount = int
            },
            SInventoryItem(Material.CHEST).setDisplayName("§a中身を設定する").setCanClick(false).setClickEvent {
                val inv = object : LargeSInventory(DungeonTower.plugin,"§a中身を設定する") {
                    override fun renderMenu(): Boolean {
                        val items = arrayListOf<SInventoryItem>()
                        loot.items.forEach {
                            items.add(SInventoryItem(it.third).addLore("§a確率:${it.first}/1000000,§a個数:${it.second}","§cシフト左クリックで削除").setCanClick(false).setClickEvent second@ { e ->
                                if (e.click != ClickType.SHIFT_LEFT)return@second
                                loot.items.remove(it)
                                allRenderMenu()
                            })
                        }
                        items.add(createInputItem(SItem(Material.EMERALD_BLOCK).setDisplayName("§a追加").addLore("§a合計の確率:${loot.items.sumOf { it.first }}/1000000"),Int::class.java,"§d確率を設定してください(手に登録するアイテムを持ってください)",true) { int, p ->
                            val item = p.inventory.itemInMainHand
                            if (item.type.isAir){
                                p.sendPrefixMsg(SStr("§c手にアイテムを持ってください！"))
                                open(p)
                            }

                            DungeonTower.sInput.sendInputCUI(p,IntRange::class.java,"§d個数を入力してください(<最低>..<最高>)") { intRange ->
                                loot.items.add(Triple(int,intRange, SItem(item)))
                                open(p)
                            }
                        })
                        setResourceItems(items)
                        return true
                    }
                }
                moveChildInventory(inv, it.whoClicked as Player)
            },
            createInputItem(SItem(Material.WRITABLE_BOOK).setDisplayName("§a保存"),String::class.java,"§a保存するファイルを指定してください(例:testやtest/test)",true) { str, p ->
                val yml = SJavaPlugin.sConfig.getConfig("loots/$str")?: YamlConfiguration()
                val section = yml.createSection(loot.includeName)
                section.set("roll",loot.rollAmount)
                val items = ArrayList<ItemStack>()
                val chances = ArrayList<Int>()
                val amounts = ArrayList<String>()
                loot.items.forEach {
                    items.add(ItemStack(it.third))
                    chances.add(it.first)
                    amounts.add("${it.second.first}to${it.second.last}")
                }
                section.set("items",items)
                section.set("chances",chances)
                section.set("amounts",amounts)
                if (SJavaPlugin.sConfig.saveConfig(yml,"loots/$str")){
                    DungeonTower.lootData[loot.includeName] = loot
                    p.sendPrefixMsg(SStr("&a保存に成功しました"))
                } else {
                    p.sendPrefixMsg(SStr("&c保存に失敗しました"))
                }
            }
        )
        setResourceItems(items)
        return true
    }
}