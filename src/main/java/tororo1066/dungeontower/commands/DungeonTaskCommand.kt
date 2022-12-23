package tororo1066.dungeontower.commands

import io.papermc.paper.command.CommandBlockHolder
import org.bukkit.Bukkit
import org.bukkit.block.CommandBlock
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import tororo1066.dungeontower.DungeonTask
import tororo1066.dungeontower.DungeonTower
import tororo1066.dungeontower.data.FloorData
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.sCommand.SCommand
import tororo1066.tororopluginapi.utils.toPlayer

class DungeonTaskCommand: CommandExecutor {

    // /dtask <Player>
    // /dtask PlayerInRadius{30}
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args[0].contains("PlayerInRadius") && sender is BlockCommandSender){
            val radius = args[0].split("{")[1].split("}")[0].toInt()
            sender.block.location.getNearbyPlayers(radius.toDouble()).forEach {
                Bukkit.dispatchCommand(sender, "dtask ${it.name}")
            }
            return true
        }

        val p = args[0].toPlayer()!!
        val data = DungeonTower.partiesData[p.uniqueId]?:return true
        val nowTask = data.nowTask?:return true
        if (!nowTask.nowFloor.clearTask.contains(FloorData.ClearTask.ENTER_COMMAND))return true
        val find = nowTask.nowFloor.clearTask.find { it.name == FloorData.ClearTask.ENTER_COMMAND.name }?:return true
        if (find.clear)return true
        find.count++
        if (find.count >= find.need){
            find.clear = true
        }

        return true
    }

}