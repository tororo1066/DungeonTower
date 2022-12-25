package tororo1066.dungeontower.inventory

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import tororo1066.dungeontower.DungeonTower
import tororo1066.dungeontower.DungeonTower.Companion.sendPrefixMsg
import tororo1066.dungeontower.data.FloorData
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem

class CreateFloor(val floor: FloorData): LargeSInventory(DungeonTower.plugin,"フロアを作成する") {

    override fun renderMenu(): Boolean {
        val items = arrayListOf(
            createInputItem(SItem(Material.DIRT).setDisplayName("§a内部名を設定する").addLore("§d現在の値:§c${floor.includeName}"),String::class.java) { str, _ ->
                floor.includeName = str
            },
            SInventoryItem(Material.EMERALD_BLOCK).setDisplayName("§aタスクを設定する").addLore("§d現在の値:§c${floor.clearTask}").setCanClick(false).setClickEvent {
                val inv = object : LargeSInventory(DungeonTower.plugin, "タスクを設定する") {
                    override fun renderMenu(): Boolean {
                        val items = arrayListOf(
                            SInventoryItem(Material.SPAWNER).setDisplayName("§aスポナーのモブをキルする").addLore(if (floor.clearTask.any { any-> any.type == FloorData.ClearTaskEnum.KILL_SPAWNER_MOBS }) "§f§l[§a§l有効§f§l]" else "§f§l[§c§l無効§f§l]").setCanClick(false).setClickEvent {
                                if (floor.clearTask.any { any-> any.type == FloorData.ClearTaskEnum.KILL_SPAWNER_MOBS }){
                                    floor.clearTask.removeIf { remove-> remove.type == FloorData.ClearTaskEnum.KILL_SPAWNER_MOBS }
                                } else {
                                    floor.clearTask.add(FloorData.ClearTask(FloorData.ClearTaskEnum.KILL_SPAWNER_MOBS))
                                }
                                allRenderMenu()
                            },
                            createInputItem(SItem(Material.COMMAND_BLOCK).setDisplayName("§aコマンドを実行する(/dtask PlayerInRadius{<半径>})").addLore(if (floor.clearTask.any { any-> any.type == FloorData.ClearTaskEnum.ENTER_COMMAND }) "§f§l[§a§l有効§f§l]" else "§f§l[§c§l無効§f§l]"),
                            Int::class.java,"コマンドを実行しないといけない数") { int, _ ->
                                if (floor.clearTask.any { any-> any.type == FloorData.ClearTaskEnum.ENTER_COMMAND }){
                                    floor.clearTask.removeIf { remove-> remove.type == FloorData.ClearTaskEnum.ENTER_COMMAND }
                                } else {
                                    floor.clearTask.add(FloorData.ClearTask(FloorData.ClearTaskEnum.KILL_SPAWNER_MOBS, need = int))
                                }
                            }
                        )

                        setResourceItems(items)
                        return true
                    }
                }
                moveChildInventory(inv,it.whoClicked as Player)
            },
            SInventoryItem(Material.COMMAND_BLOCK).setDisplayName("§aフロアに入ったときに実行するコマンド").addLore("§d現在の値:§c${floor.joinCommands}").setCanClick(false).setClickEvent {
                val inv = object : LargeSInventory(DungeonTower.plugin, "コマンドを設定する") {
                    override fun renderMenu(): Boolean {
                        val items = arrayListOf<SInventoryItem>()
                        floor.joinCommands.forEach {  str ->
                            items.add(SInventoryItem(Material.REDSTONE_BLOCK).setDisplayName(str).addLore("§cシフト左クリックで削除").setCanClick(false).setClickEvent { e ->
                                if (e.click != ClickType.SHIFT_LEFT){
                                    floor.joinCommands.remove(str)
                                    allRenderMenu()
                                }
                            })
                        }
                        items.add(createInputItem(SItem(Material.EMERALD_BLOCK).setDisplayName("§a追加").addLore("§e<player>でプレイヤーの名前"),String::class.java) { str, _ ->
                            floor.joinCommands.add(str)
                        })
                        return true
                    }
                }
                moveChildInventory(inv,it.whoClicked as Player)
            },
            createInputItem(SItem(Material.WRITABLE_BOOK).setDisplayName("§a保存"),String::class.java,"§a保存するファイルを指定してください(例:testやtest/test)",true) { str, p ->
                val yml = SJavaPlugin.sConfig.getConfig("floors/$str")?:YamlConfiguration()
                val section = yml.createSection(floor.includeName)
                section.set("startLoc","${floor.startLoc.blockX},${floor.startLoc.blockY},${floor.startLoc.blockZ}")
                section.set("endLoc","${floor.endLoc.blockX},${floor.endLoc.blockY},${floor.endLoc.blockZ}")
                val clearTasks = ArrayList<String>()
                floor.clearTask.forEach {
                    when(it.type){
                        FloorData.ClearTaskEnum.KILL_SPAWNER_MOBS->{
                            clearTasks.add(it.type.name)
                        }
                        FloorData.ClearTaskEnum.ENTER_COMMAND->{
                            clearTasks.add("${it.type.name},${it.need}")
                        }
                    }
                }
                section.set("clearTasks",clearTasks)
                section.set("joinCommands",floor.joinCommands)
                if (SJavaPlugin.sConfig.saveConfig(yml,"floors/$str")){
                    DungeonTower.floorData[floor.includeName] = floor
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