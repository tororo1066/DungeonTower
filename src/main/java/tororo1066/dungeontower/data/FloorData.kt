package tororo1066.dungeontower.data

import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import org.bukkit.*
import org.bukkit.block.Chest
import org.bukkit.block.CommandBlock
import org.bukkit.block.Sign
import org.bukkit.block.data.Directional
import org.bukkit.block.data.MultipleFacing
import org.bukkit.block.data.Rotatable
import org.bukkit.block.data.type.EndPortalFrame
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.loot.LootContext
import org.bukkit.loot.LootTable
import org.bukkit.loot.LootTables
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import tororo1066.dungeontower.DungeonTower
import tororo1066.tororopluginapi.sEvent.SEvent
import tororo1066.tororopluginapi.utils.LocType
import tororo1066.tororopluginapi.utils.toLocString
import java.util.UUID
import kotlin.math.min
import kotlin.random.Random

class FloorData: Cloneable {

    enum class ClearTaskEnum{
        KILL_SPAWNER_MOBS,
        ENTER_COMMAND
    }

    class ClearTask(val type: ClearTaskEnum,var need: Int = 0, var count: Int = 0, var clear: Boolean = false)
    var includeName = ""

    lateinit var startLoc: Location
    lateinit var endLoc: Location

    var lastFloor = true

    lateinit var preventFloor: Location
    lateinit var nextFloor: Location
    val spawners = ArrayList<BukkitTask>()

    val joinCommands = ArrayList<String>()

    val clearTask = ArrayList<ClearTask>()
    val spawnerClearTasks = HashMap<UUID,Boolean>()

    fun callFloor(loc: Location): Boolean {

        val lowX = min(startLoc.blockX, endLoc.blockX)
        val lowY = min(startLoc.blockY, endLoc.blockY)
        val lowZ = min(startLoc.blockZ, endLoc.blockZ)
        val highX = if (lowX == startLoc.blockX) endLoc.blockX else startLoc.blockX
        val highY = if (lowY == startLoc.blockY) endLoc.blockY else startLoc.blockY
        val highZ = if (lowZ == startLoc.blockZ) endLoc.blockZ else startLoc.blockZ

        for ((indexX, x) in (lowX..highX).withIndex()){
            for ((indexY, y) in (lowY..highY).withIndex()){
                for ((indexZ, z) in (lowZ..highZ).withIndex()){
                    val block = DungeonTower.floorWorld.getBlockAt(x,y,z)
                    val placeLoc = loc.clone().add(indexX.toDouble(),indexY.toDouble(),indexZ.toDouble())
                    placeLoc.block.type = block.type
                    placeLoc.block.blockData = block.blockData
                    placeLoc.block.state.data = DungeonTower.floorWorld.getBlockState(x,y,z).data
                    placeLoc.block.state.update()

                    when(block.type){

                        Material.OAK_SIGN->{
                            val data = DungeonTower.floorWorld.getBlockState(x,y,z) as Sign
                            when(data.getLine(0)){
                                "loot"->{
                                    val loot = (DungeonTower.lootData[data.getLine(1)]?:continue).clone()
                                    placeLoc.block.type = Material.CHEST
                                    val chest = placeLoc.block.state as Chest
                                    chest.update()
                                    loot.fillInventory(chest.inventory,java.util.Random(),LootContext.Builder(chest.location).build())
                                    val blockData = chest.blockData as Directional
                                    blockData.facing = (data.blockData as org.bukkit.block.data.type.Sign).rotation
                                    chest.blockData = blockData
                                }
                                "spawner"->{
                                    val spawner = (DungeonTower.spawnerData[data.getLine(1)]?:continue).clone()
                                    val locSave = placeLoc.clone()
                                    locSave.block.type = Material.END_PORTAL_FRAME
                                    val portal = locSave.block.blockData as EndPortalFrame
                                    portal.setEye(true)

                                    locSave.block.blockData = portal
                                    val randUUID = UUID.randomUUID()
                                    spawnerClearTasks[randUUID] = spawner.navigateKill <= 0
                                    spawners.add(
                                        object : BukkitRunnable() {
                                            val sEvent = SEvent(DungeonTower.plugin)
                                            init {
                                                sEvent.register(MythicMobDeathEvent::class.java) { e ->
                                                    if (e.mob.entity.dataContainer[NamespacedKey(DungeonTower.plugin,"dmob"), PersistentDataType.STRING] != randUUID.toString())return@register
                                                    spawner.kill++
                                                    if (spawner.kill >= spawner.navigateKill){
                                                        spawnerClearTasks[randUUID] = true
                                                        if (spawnerClearTasks.values.none { !it }){
                                                            clearTask.filter { it.type == ClearTaskEnum.KILL_SPAWNER_MOBS }.forEach {
                                                                it.clear = true
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            override fun cancel() {
                                                sEvent.unregisterAll()
                                                Bukkit.getScheduler().runTask(DungeonTower.plugin, Runnable {
                                                    DungeonTower.dungeonWorld.entities.filter { it.persistentDataContainer.get(NamespacedKey(DungeonTower.plugin, "dmob"),
                                                        PersistentDataType.STRING) == randUUID.toString() }.forEach {
                                                            it.remove()
                                                    }
                                                })
                                                super.cancel()
                                            }

                                            override fun run() {
                                                if (spawner.count >= spawner.max)return
                                                if (locSave.getNearbyPlayers(spawner.activateRange.toDouble()).isEmpty())return
                                                val spawnLoc = locSave.clone().add((-spawner.radius..spawner.radius).random().toDouble(), 0.0,(-spawner.radius..spawner.radius).random().toDouble())
                                                val mob = spawner.mob.spawn(BukkitAdapter.adapt(spawnLoc),spawner.level)
                                                locSave.world.playSound(locSave, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f)
                                                locSave.world.spawnParticle(Particle.FLAME, locSave, 15)
                                                mob.entity.dataContainer.set(NamespacedKey(DungeonTower.plugin,"dmob"),
                                                    PersistentDataType.STRING,
                                                    randUUID.toString())
                                                spawner.count++
                                                if (spawner.count >= spawner.max){
                                                    portal.setEye(false)
                                                    locSave.block.blockData = portal
                                                }

                                            }
                                        }.runTaskTimer(DungeonTower.plugin,spawner.coolTime.toLong(),spawner.coolTime.toLong())
                                    )
                                }
                            }

                        }
                        Material.WARPED_STAIRS->{
                            nextFloor = placeLoc.clone().add(0.0,1.0,0.0)
                            lastFloor = false
                        }
                        Material.CRIMSON_STAIRS->{
                            preventFloor = placeLoc.clone().add(0.0,1.0,0.0)
                        }

                        else->{}
                    }
                }
            }
        }

        DungeonTower.callX += (highX - lowX) * 2 + 1

        return true
    }

    fun removeFloor(loc: Location){
        spawners.forEach {
            it.cancel()
        }
        spawners.clear()

        val lowX = min(startLoc.blockX, endLoc.blockX)
        val lowY = min(startLoc.blockY, endLoc.blockY)
        val lowZ = min(startLoc.blockZ, endLoc.blockZ)
        val highX = if (lowX == startLoc.blockX) endLoc.blockX else startLoc.blockX
        val highY = if (lowY == startLoc.blockY) endLoc.blockY else startLoc.blockY
        val highZ = if (lowZ == startLoc.blockZ) endLoc.blockZ else startLoc.blockZ

        for ((indexX, x) in (lowX..highX).withIndex()){
            for ((indexY, _) in (lowY..highY).withIndex()){
                for ((indexZ, z) in (lowZ..highZ).withIndex()){
                    val placeLoc = loc.clone().add(indexX.toDouble(),indexY.toDouble(),indexZ.toDouble())
                    placeLoc.block.type = Material.AIR
                }
            }
        }

    }

    public override fun clone(): FloorData {
        return super.clone() as FloorData
    }

    companion object{
        fun loadFromYml(yml: YamlConfiguration): HashMap<String,FloorData> {
            val dataMap = HashMap<String,FloorData>()
            for (key in yml.getKeys(false)){
                val section = yml.getConfigurationSection(key)!!
                val data = FloorData()
                data.includeName = key
                val start = section.getString("startLoc")!!.split(",").map { it.toInt().toDouble() }
                data.startLoc = Location(DungeonTower.dungeonWorld,start[0],start[1],start[2])
                val end = section.getString("endLoc")!!.split(",").map { it.toInt().toDouble() }
                data.endLoc = Location(DungeonTower.dungeonWorld,end[0],end[1],end[2])
                data.joinCommands.addAll(section.getStringList("joinCommands"))
                section.getStringList("clearTasks").forEach {
                    val split = it.split(",")
                    val taskEnum = ClearTaskEnum.valueOf(split[0].uppercase())
                    val task = ClearTask(taskEnum)
                    if (taskEnum == ClearTaskEnum.ENTER_COMMAND){
                        task.need = split[1].toInt()
                    }
                    data.clearTask.add(task)
                }
                dataMap[key] = data
            }
            return dataMap
        }
    }
}