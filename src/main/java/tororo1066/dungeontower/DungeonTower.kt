package tororo1066.dungeontower

import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.dungeontower.commands.DungeonTaskCommand
import tororo1066.dungeontower.data.*
import tororo1066.tororopluginapi.SInput
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.utils.sendMessage
import java.io.File
import java.util.UUID

class DungeonTower: SJavaPlugin(UseOption.SConfig) {

    companion object{
        //タワーのデータ keyが内部名
        val towerData = HashMap<String,TowerData>()
        //フロアのデータ keyが内部名
        val floorData = HashMap<String,FloorData>()
        //スポナーのデータ keyが内部名
        val spawnerData = HashMap<String,SpawnerData>()
        //ルートテーブルのデータ keyが内部名
        val lootData = HashMap<String,LootData>()
        //パーティのデータ valueがnull許容値なのはnullじゃないかどうかでパーティリーダーか見るため
        val partiesData = HashMap<UUID,PartyData?>()
        //タワーに挑戦中のプレイヤー一覧
        val playNow = ArrayList<UUID>()
        lateinit var dungeonWorld: World
        lateinit var floorWorld: World
        lateinit var lobbyLocation: Location
        val prefix = SStr("&d[&cDungeon&4Tower&d]&r")
        lateinit var sInput: SInput
        lateinit var plugin: DungeonTower
        lateinit var mythic: MythicBukkit

        var callX = 0

        fun CommandSender.sendPrefixMsg(str: SStr){
            this.sendMessage(prefix.toTextComponent().append(str.toTextComponent()))
        }
    }

    override fun onStart() {
        plugin = this
        sInput = SInput(this)
        mythic = MythicBukkit.inst()
        dungeonWorld = Bukkit.getWorld(config.getString("dungeonWorld")!!)!!
        floorWorld = Bukkit.getWorld(config.getString("floorWorld")!!)!!
        lobbyLocation = config.getLocation("lobbyLocation", Location(floorWorld,0.0,0.0,0.0))!!

        val spawnerFiles = loadAllFiles("spawners")
        spawnerFiles.forEach {
            if (it.extension != "yml")return@forEach
            spawnerData.putAll(SpawnerData.loadFromYml(YamlConfiguration.loadConfiguration(it)))
        }

        val floorFiles = loadAllFiles("floors")
        floorFiles.forEach {
            if (it.extension != "yml")return@forEach
            floorData.putAll(FloorData.loadFromYml(YamlConfiguration.loadConfiguration(it)))
        }

        val towerFiles = loadAllFiles("towers")
        towerFiles.forEach {
            if (it.extension != "yml")return@forEach
            towerData[it.nameWithoutExtension] = TowerData.loadFromYml(it)
        }

        val lootFiles = loadAllFiles("loots")
        lootFiles.forEach {
            if (it.extension != "yml")return@forEach
            lootData.putAll(LootData.loadFromYml(YamlConfiguration.loadConfiguration(it)))
        }

        getCommand("dungeontask")?.setExecutor(DungeonTaskCommand())


    }

    fun loadAllFiles(folder: File): List<File> {
        if (!folder.exists()) return emptyList()
        val fileList = ArrayList<File>()
        (folder.listFiles()?:return emptyList()).forEach {
            if (it.isDirectory){
                fileList.addAll(loadAllFiles(it))
            } else {
                fileList.add(it)
            }
        }
        return fileList
    }

    fun loadAllFiles(path: String): List<File> {
        val file = File(plugin.dataFolder.path + File.separator + path + File.separator)
        if (!file.exists()) return emptyList()
        return loadAllFiles(file)
    }
}