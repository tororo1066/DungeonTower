package tororo1066.dungeontower.data

import io.lumine.mythic.api.mobs.MythicMob
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.dungeontower.DungeonTower

class SpawnerData: Cloneable {
    var includeName = ""
    lateinit var mob: MythicMob
    var count = 0
    var coolTime = 0
    var max = 0
    var radius = 0
    var level = 0.0
    var activateRange = 0

    var kill = 0
    var navigateKill = 3

    public override fun clone(): SpawnerData {
        return super.clone() as SpawnerData
    }

    companion object{
        fun loadFromYml(yml: YamlConfiguration): HashMap<String, SpawnerData> {
            val dataMap = HashMap<String,SpawnerData>()
            for (key in yml.getKeys(false)){
                val section = yml.getConfigurationSection(key)!!
                val data = SpawnerData()
                data.includeName = key
                data.mob = DungeonTower.mythic.apiHelper.getMythicMob(section.getString("mob"))
                data.coolTime = section.getInt("cooltime")
                data.max = section.getInt("max")
                data.radius = section.getInt("radius")
                data.level = section.getDouble("level")
                data.activateRange = section.getInt("activateRange")
                data.navigateKill = section.getInt("navigate")
                dataMap[key] = data
            }
            return dataMap
        }
    }
}