package tororo1066.dungeontower

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import tororo1066.dungeontower.data.FloorData
import tororo1066.dungeontower.data.PartyData
import tororo1066.dungeontower.data.TowerData
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.sEvent.SEvent
import java.util.UUID

class DungeonTask(val party: PartyData, val tower: TowerData): Thread() {

    class Lock{

        @Volatile
        private var isLock = false
        @Volatile
        private var hadLocked = false

        fun lock(){
            synchronized(this){
                if (hadLocked){
                    return
                }
                isLock = true
            }
            while (isLock){ sleep(1) }
        }

        fun unlock(){
            synchronized(this){
                hadLocked = true
                isLock = false
            }
        }
    }

    var nowFloorNum = 1
    lateinit var nowFloor: FloorData
    var nowCallX = 0
    var preventCallX = 0
    val sEvent = SEvent(DungeonTower.plugin)
    val nextFloorPlayers = ArrayList<UUID>()
    lateinit var nowThread: Thread

    var end = false

    private fun runTask(unit: ()->Unit){
        val lock = Lock()

        Bukkit.getScheduler().runTask(DungeonTower.plugin, Runnable {
            unit.invoke()
            lock.unlock()
        })

        lock.lock()
    }

    override fun run() {
        if (party.players.size == 0)return
        nowThread = this
        party.nowTask = this
        party.broadCast(SStr("&c${tower.name}&aにテレポート中..."))
        runTask { party.smokeStan(60) }
        nowCallX = DungeonTower.callX
        nowFloor = tower.randomFloor(nowFloorNum)

        runTask { nowFloor.callFloor(Location(DungeonTower.dungeonWorld, nowCallX.toDouble(), 50.0, 0.0)) }
        preventCallX = nowCallX
        runTask { party.teleport(nowFloor.preventFloor.add(0.0,1.0,0.0)) }
        sEvent.register(PlayerQuitEvent::class.java){ e ->
            nextFloorPlayers.remove(e.player.uniqueId)
            if (party.parent == e.player.uniqueId){
                val randomParent = party.players.entries.filter { it.key != e.player.uniqueId }.randomOrNull()?.key
                if (randomParent != null){
                    party.parent = randomParent
                }
            }
            party.players.remove(e.player.uniqueId)
            DungeonTower.partiesData[party.parent] = party
            DungeonTower.partiesData.remove(e.player.uniqueId)

            DungeonTower.playNow.remove(e.player.uniqueId)

            if (party.players.isEmpty()){
                runTask { nowFloor.removeFloor(Location(DungeonTower.dungeonWorld, preventCallX.toDouble(), 50.0, 0.0)) }
                end = true
                sEvent.unregisterAll()
                nowThread.interrupt()
            }
        }
        sEvent.register(PlayerMoveEvent::class.java){ e ->
            if (!party.players.containsKey(e.player.uniqueId))return@register
            nextFloorPlayers.remove(e.player.uniqueId)
            when(e.to.clone().subtract(0.0,1.0,0.0).block.type){
                Material.WARPED_STAIRS->{
                    nextFloorPlayers.add(e.player.uniqueId)
                    if (nowFloor.clearTask.any { !it.clear })return@register
                    if (party.players.size / 2 < nextFloorPlayers.size){
                        party.smokeStan(60)
                        nowFloorNum++
                        nowCallX = DungeonTower.callX
                        val preventFloor = nowFloor
                        nowFloor = tower.randomFloor(nowFloorNum)
                        nowFloor.callFloor(Location(DungeonTower.dungeonWorld, nowCallX.toDouble(), 50.0, 0.0))
                        party.teleport(nowFloor.preventFloor.add(0.0,1.0,0.0))
                        preventFloor.removeFloor(Location(DungeonTower.dungeonWorld, preventCallX.toDouble(), 50.0, 0.0))
                        preventCallX = DungeonTower.callX
                    }
                }
                Material.DIAMOND_BLOCK->{
                    if (nowFloor.clearTask.any { !it.clear })return@register
                    if (!nowFloor.lastFloor)return@register//どこでもクリアできるっていうのも面白いかもしれない
                    nextFloorPlayers.add(e.player.uniqueId)
                    if (party.players.size / 2 < nextFloorPlayers.size){
                        party.broadCast(SStr("&a&lクリア！"))
                        end = true
                        sEvent.unregisterAll()
                        party.teleport(DungeonTower.lobbyLocation)
                        nowFloor.removeFloor(Location(DungeonTower.dungeonWorld, preventCallX.toDouble(), 50.0, 0.0))

                        nowThread.interrupt()
                    }
                }
                else->{}
            }
        }


        sleep(3000)

        while (!end){
            if (nowFloor.clearTask.none { !it.clear }){
                party.actionBar(SStr("&a&l道が開いた！"))
            } else {
                party.actionBar(SStr("&c&l何かをしないといけない..."))
            }

            sleep(50)
        }



    }
}