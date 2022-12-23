package tororo1066.dungeontower.inventory

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import tororo1066.dungeontower.DungeonTower
import tororo1066.dungeontower.DungeonTower.Companion.sendPrefixMsg
import tororo1066.dungeontower.data.FloorData
import tororo1066.dungeontower.data.TowerData
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem

class CreateTower(val tower: TowerData): LargeSInventory(DungeonTower.plugin,"フロアを作成する") {

    override fun renderMenu(): Boolean {
        val items = arrayListOf(
            createInputItem(SItem(Material.DIRT).setDisplayName("§a内部名を設定する").addLore("§d現在の値:§c${tower.includeName}"),String::class.java) { str, _ ->
                tower.includeName = str
            },
            createInputItem(SItem(Material.GRASS_BLOCK).setDisplayName("§a名前を設定する").addLore("§d現在の値:§c${tower.name}"),String::class.java) { str, _ ->
                tower.name = str
            },
            createInputItem(SItem(Material.EMERALD_BLOCK).setDisplayName("§a最大人数を設定する").addLore("§d現在の値:§c${tower.partyLimit}"),Int::class.java) { int, _ ->
                tower.partyLimit = int
            },
            SInventoryItem(Material.COMMAND_BLOCK).setDisplayName("§aフロアの設定").setCanClick(false).setClickEvent { e ->
                val inv = object : LargeSInventory(DungeonTower.plugin, "フロアの設定") {
                    override fun renderMenu(): Boolean {
                        val items = arrayListOf<SInventoryItem>()
                        tower.floors.keys.forEach { int ->
                            items.add(SInventoryItem(Material.REDSTONE_BLOCK).setDisplayName("${int}f").setCanClick(false).setClickEvent {
                                val inv = object : LargeSInventory(DungeonTower.plugin, "${int}f") {
                                    override fun renderMenu(): Boolean {
                                        val newItems = arrayListOf<SInventoryItem>()
                                        tower.floors[int]!!.forEach { pair ->
                                            newItems.add(SInventoryItem(Material.REDSTONE_BLOCK).setDisplayName("確率:${pair.first}/1000000,フロア:${pair.second.includeName}").addLore("§cシフト左クリックで削除").setCanClick(false).setClickEvent second@ { e ->
                                                if (e.click != ClickType.SHIFT_LEFT)return@second
                                                tower.floors[int]!!.remove(pair)
                                                allRenderMenu()

                                            })
                                        }

                                        newItems.add(createInputItem(SItem(Material.EMERALD_BLOCK).setDisplayName("§a追加").addLore("§a合計の確率:${tower.floors[int]!!.sumOf { sum -> sum.first }}/1000000"),String::class.java,"§dフロア名を入れてください",true) { str, p ->
                                            val floor = DungeonTower.floorData[str]
                                            if (floor == null){
                                                p.sendPrefixMsg(SStr("&cフロアが存在しません"))
                                                open(p)
                                                return@createInputItem
                                            }
                                            DungeonTower.sInput.sendInputCUI(p,Int::class.java,"§d確率を入れてください") { chance ->
                                                tower.floors[int]!!.add(Pair(chance,floor.clone()))
                                                open(p)
                                            }
                                        })
                                        setResourceItems(newItems)
                                        return true
                                    }
                                }
                                moveChildInventory(inv,e.whoClicked as Player)
                            })
                        }
                        items.add(SInventoryItem(Material.EMERALD_BLOCK).setDisplayName("§a追加").setCanClick(false).setClickEvent {
                            tower.floors[(tower.floors.keys.maxOrNull()?:0)+1] = arrayListOf()
                            allRenderMenu()
                        })
                        setResourceItems(items)
                        return true
                    }
                }
                moveChildInventory(inv,e.whoClicked as Player)
            },
            createInputItem(SItem(Material.WRITABLE_BOOK).setDisplayName("§a保存"),String::class.java,"§a保存するファイルを指定してください(例:testやtest/test)",true) { str, p ->
                val yml = YamlConfiguration()
                yml.set("name",tower.name)
                yml.set("partyLimit",tower.partyLimit)

                tower.floors.forEach { (floorNum, array) ->
                    val list = ArrayList<String>()
                    array.forEach {
                        list.add("${it.first},${it.second.includeName}")
                    }
                    yml.set("floors.${floorNum}f",list)
                }
                if (SJavaPlugin.sConfig.saveConfig(yml,"towers/$str")){
                    DungeonTower.towerData[tower.includeName] = tower
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