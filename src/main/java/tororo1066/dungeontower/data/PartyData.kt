package tororo1066.dungeontower.data

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import tororo1066.dungeontower.DungeonTask
import tororo1066.dungeontower.DungeonTower
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.utils.sendMessage
import tororo1066.tororopluginapi.utils.toPlayer
import java.util.UUID

class PartyData: Cloneable {
    val players = HashMap<UUID,UserData>()
    lateinit var parent: UUID
    var nowTask: DungeonTask? = null

    fun broadCast(str: SStr){
        players.keys.forEach {
            it.toPlayer()?.sendMessage(DungeonTower.prefix.toTextComponent().append(str.toTextComponent()))
        }
    }

    fun smokeStan(tick: Int){
        players.keys.forEach {
            val p = it.toPlayer()?:return@forEach
            p.addPotionEffect(PotionEffect(PotionEffectType.SLOW,tick,10,false,false,false))
            p.addPotionEffect(PotionEffect(PotionEffectType.JUMP,tick,200,false,false,false))
            p.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS,tick,1,false,false,false))
        }
    }

    fun teleport(loc: Location){
        players.keys.forEach {
            it.toPlayer()?.teleport(loc)
        }
    }

    fun actionBar(str: SStr){
        players.keys.forEach {
            it.toPlayer()?.sendActionBar(str.toTextComponent())
        }
    }

    public override fun clone(): PartyData {
        return super.clone() as PartyData
    }
}