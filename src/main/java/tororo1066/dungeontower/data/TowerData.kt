package tororo1066.dungeontower.data

import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.dungeontower.DungeonTower
import java.io.File
import kotlin.random.Random
import kotlin.random.nextInt

class TowerData: Cloneable {

    //内部名
    var includeName = ""
    //名前
    var name = ""
    //挑戦可能な最大人数 -1でなし
    var partyLimit = -1
    //フロアたち keyは階層、Pairのfirstは確率
    val floors = HashMap<Int,ArrayList<Pair<Int,FloorData>>>()

    //ランダムにフロアを出す(確率を決めれる)
    fun randomFloor(floorNum: Int): FloorData {
        val random = Random.nextInt(1..1000000)
        var preventRandom = 0
        for (floor in floors[floorNum]!!){
            if (preventRandom < random && floor.first + preventRandom > random){
                return floor.second
            }
            preventRandom = floor.first
        }
        throw NullPointerException("Can't find floor data. Maybe sum percentage is not 1000000.")
    }

    companion object{
        fun loadFromYml(file: File): TowerData {
            val yml = YamlConfiguration.loadConfiguration(file)
            val data = TowerData()
            data.includeName = file.nameWithoutExtension
            data.name = yml.getString("name","null")!!
            data.partyLimit = yml.getInt("partyLimit",-1)

            val floors = yml.getConfigurationSection("floors")!!
            for (key in floors.getKeys(false)){
                val floorNum = key.substring(0,1).toInt()
                data.floors[floorNum] = arrayListOf()
                floors.getStringList(key).forEach {
                    val split = it.split(",")
                    val floorData = (DungeonTower.floorData[split[1]]?:throw NullPointerException("(FloorData) Failed load to ${split[1]} in ${file.nameWithoutExtension}.")).clone()
                    data.floors[floorNum]!!.add(Pair(split[0].toInt(), floorData))
                }
            }

            return data
        }
    }

    public override fun clone(): TowerData {
        return super.clone() as TowerData
    }
}