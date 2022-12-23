package tororo1066.dungeontower.inventory

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.dungeontower.DungeonTower
import tororo1066.dungeontower.DungeonTower.Companion.sendPrefixMsg
import tororo1066.dungeontower.data.FloorData
import tororo1066.dungeontower.data.SpawnerData
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem

class CreateSpawner(val spawner: SpawnerData): LargeSInventory(DungeonTower.plugin,"スポナーを作成する") {

    override fun renderMenu(): Boolean {
        val items = arrayListOf(
            createInputItem(SItem(Material.PLAYER_HEAD).setDisplayName("§aMythicMobを設定する").addLore("§d現在の値:§c${spawner.mob.internalName}"),String::class.java){ str, _ ->
                spawner.mob = DungeonTower.mythic.apiHelper.getMythicMob(str)?:return@createInputItem
            },
            createInputItem(SItem(Material.CLOCK).setDisplayName("§aCoolTime(tick)を設定する").addLore("§d現在の値:§c${spawner.coolTime}"),Int::class.java){ int, _ ->
                spawner.coolTime = int
            },
            createInputItem(SItem(Material.REDSTONE_BLOCK).setDisplayName("§a湧く量を設定する").addLore("§d現在の値:§c${spawner.max}"),Int::class.java){ int, _ ->
                spawner.max = int
            },
            createInputItem(SItem(Material.DIAMOND_BLOCK).setDisplayName("§a湧く半径を設定する").addLore("§d現在の値:§c${spawner.radius}"),Int::class.java){ int, _ ->
                spawner.radius = int
            },
            createInputItem(SItem(Material.EXPERIENCE_BOTTLE).setDisplayName("§aMythicMobのlevelを設定する").addLore("§d現在の値:§c${spawner.level}"),Double::class.java){ double, _ ->
                spawner.level = double
            },
            createInputItem(SItem(Material.BARRIER).setDisplayName("§a稼働する半径を設定する").addLore("§d現在の値:§c${spawner.activateRange}"),Int::class.java){ int, _ ->
                spawner.activateRange = int
            },
            createInputItem(SItem(Material.DIAMOND_SWORD).setDisplayName("§aクリア条件を達成するために倒す数を設定する").addLore("§d現在の値:§c${spawner.navigateKill}"),Int::class.java){ int, _ ->
                spawner.navigateKill = int
            },
            createInputItem(SItem(Material.WRITABLE_BOOK).setDisplayName("§a保存"),String::class.java,"§a保存するファイルを指定してください(例:testやtest/test)",true) { str, p ->
                val yml = SJavaPlugin.sConfig.getConfig("spawners/$str")?: YamlConfiguration()
                val section = yml.createSection(spawner.includeName)
                section.set("mob",spawner.mob.internalName)
                section.set("cooltime",spawner.coolTime)
                section.set("max",spawner.max)
                section.set("radius",spawner.radius)
                section.set("level",spawner.level)
                section.set("activateRange",spawner.activateRange)
                section.set("navigate",spawner.navigateKill)
                if (SJavaPlugin.sConfig.saveConfig(yml,"spawners/$str")){
                    DungeonTower.spawnerData[spawner.includeName] = spawner
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