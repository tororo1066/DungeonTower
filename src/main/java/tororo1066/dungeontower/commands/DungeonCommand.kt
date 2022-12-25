package tororo1066.dungeontower.commands

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import tororo1066.dungeontower.DungeonTask
import tororo1066.dungeontower.DungeonTower
import tororo1066.dungeontower.DungeonTower.Companion.sendPrefixMsg
import tororo1066.dungeontower.data.*
import tororo1066.dungeontower.inventory.CreateFloor
import tororo1066.dungeontower.inventory.CreateLoot
import tororo1066.dungeontower.inventory.CreateSpawner
import tororo1066.dungeontower.inventory.CreateTower
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.annotation.SCommandBody
import tororo1066.tororopluginapi.sCommand.SCommand
import tororo1066.tororopluginapi.sCommand.SCommandArg
import tororo1066.tororopluginapi.sCommand.SCommandArgType
import tororo1066.tororopluginapi.sCommand.SCommandObject
import tororo1066.tororopluginapi.sItem.SItem
import tororo1066.tororopluginapi.utils.toPlayer
import java.util.UUID

class DungeonCommand: SCommand("dungeontower",DungeonTower.prefix.toString(),"tower.user") {

    //パーティ承認待ちのUUID keyがパーティリーダーでvalueが承認待ちのUUIDのリスト
    private val accepts = HashMap<UUID,ArrayList<UUID>>()

    //パーティコマンド
    private fun party(): SCommandObject {
        return command().addArg(SCommandArg("party"))
    }

    //タワーに挑戦する
    @SCommandBody
    val entryTower = command().addArg(SCommandArg("entry")).addArg(SCommandArg(DungeonTower.towerData.keys)).setPlayerExecutor {
        if (DungeonTower.playNow.contains(it.sender.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4プレイ中はこのコマンドを実行できません"))
            return@setPlayerExecutor
        }
        if (DungeonTower.partiesData[it.sender.uniqueId] == null){
            it.sender.sendPrefixMsg(SStr("&4パーティリーダーしか実行できません"))
            return@setPlayerExecutor
        }

        val tower = DungeonTower.towerData[it.args[1]]!!
        val partyData = DungeonTower.partiesData[it.sender.uniqueId]!!.clone()
        if (partyData.players.size > tower.partyLimit){
            it.sender.sendPrefixMsg(SStr("&4${tower.partyLimit}人以下でしか入れません (現在:${partyData.players.size}人)"))
            return@setPlayerExecutor
        }

        val task = DungeonTask(partyData, tower)
        partyData.players.keys.forEach { uuid ->
            DungeonTower.playNow.add(uuid)
        }
        //playNow書き込み パーティ削除？
        task.start()//仮置き
    }

    //パーティを作る
    @SCommandBody
    val createParty = party().addArg(SCommandArg("create")).setPlayerExecutor {
        if (DungeonTower.playNow.contains(it.sender.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4プレイ中はこのコマンドを実行できません"))
            return@setPlayerExecutor
        }
        if (DungeonTower.partiesData.containsKey(it.sender.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4既にパーティに入っています"))
            return@setPlayerExecutor
        }

        val party = PartyData()
        party.parent = it.sender.uniqueId
        party.players[it.sender.uniqueId] = UserData(it.sender.uniqueId,it.sender.name)
        DungeonTower.partiesData[it.sender.uniqueId] = party
        it.sender.sendPrefixMsg(SStr("&aパーティを作成しました"))
    }

    //パーティに入る申請を送る
    @SCommandBody
    val joinParty = party().addArg(SCommandArg("join")).addArg(SCommandArg(SCommandArgType.ONLINE_PLAYER)).setPlayerExecutor {
        if (DungeonTower.playNow.contains(it.sender.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4プレイ中はこのコマンドを実行できません"))
            return@setPlayerExecutor
        }
        if (DungeonTower.partiesData.containsKey(it.sender.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4既にパーティに入っています"))
            return@setPlayerExecutor
        }

        val p = it.args[2].toPlayer()!!

        if (DungeonTower.partiesData[p.uniqueId] == null){
            it.sender.sendPrefixMsg(SStr("&4パーティが存在しません"))
            return@setPlayerExecutor
        }

        if (!accepts.containsKey(p.uniqueId)) accepts[p.uniqueId] = arrayListOf()
        accepts[p.uniqueId]!!.add(it.sender.uniqueId)

        it.sender.sendPrefixMsg(SStr("&a申請を送りました"))

        p.sendPrefixMsg(SStr("&a${it.sender.name}から申請が来ています！"))
        p.sendPrefixMsg(SStr("&a&l[承諾にはここをクリック！]").commandText("/dungeontower accept ${it.sender.name}"))

    }

    //パーティに入る申請を許可する
    @SCommandBody
    val acceptParty = party().addArg(SCommandArg("accept")).addArg(SCommandArg(SCommandArgType.ONLINE_PLAYER)).setPlayerExecutor {
        if (DungeonTower.playNow.contains(it.sender.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4プレイ中はこのコマンドを実行できません"))
            return@setPlayerExecutor
        }
        if (DungeonTower.partiesData[it.sender.uniqueId] == null){
            it.sender.sendPrefixMsg(SStr("&4パーティリーダーしか実行できません"))
            return@setPlayerExecutor
        }

        val p = it.args[2].toPlayer()!!

        if (!accepts.containsKey(it.sender.uniqueId) || !accepts[it.sender.uniqueId]!!.contains(p.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4そのプレイヤーから申請が来ていません"))
            return@setPlayerExecutor
        }

        accepts[it.sender.uniqueId]!!.remove(p.uniqueId)

        if (DungeonTower.partiesData.containsKey(p.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4既にそのプレイヤーはパーティに入っています"))
            return@setPlayerExecutor
        }

        val partyData = DungeonTower.partiesData[it.sender.uniqueId]!!

        partyData.players[p.uniqueId] = UserData(p.uniqueId,p.name)

        p.sendPrefixMsg(SStr("&a${it.sender.name}のパーティに参加しました！"))
        it.sender.sendPrefixMsg(SStr("&a${p.name}がパーティに参加しました！"))
    }

    @SCommandBody
    val leaveParty = party().addArg(SCommandArg("leave")).setPlayerExecutor {
        if (DungeonTower.playNow.contains(it.sender.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4プレイ中はこのコマンドを実行できません"))
            return@setPlayerExecutor
        }
        if (!DungeonTower.partiesData.containsKey(it.sender.uniqueId)){
            it.sender.sendPrefixMsg(SStr("&4パーティに入っていません"))
            return@setPlayerExecutor
        }
        val data = DungeonTower.partiesData[it.sender.uniqueId]
        if (data == null){
            DungeonTower.partiesData.values.filterNotNull().forEach { party ->
                if (!party.players.containsKey(it.sender.uniqueId))return@forEach
                party.broadCast(SStr("&c${it.sender.name}がパーティから退出しました"))
                party.players.remove(it.sender.uniqueId)
                DungeonTower.partiesData.remove(it.sender.uniqueId)
            }
        } else {
            data.broadCast(SStr("&cパーティが解散されました"))
            data.players.keys.forEach { uuid ->
                DungeonTower.partiesData.remove(uuid)
            }
        }
    }

    private fun opCommand(): SCommandObject {
        return command().addArg(SCommandArg("op")).addNeedPermission("tower.op")
    }

    @SCommandBody
    val giveWand = opCommand().addArg(SCommandArg("wand")).setPlayerExecutor {
        val item = SItem(Material.STICK).setDisplayName("§a範囲を指定するわんど...みたいな").setCustomData(DungeonTower.plugin,"wand",
            PersistentDataType.INTEGER,1)
        it.sender.inventory.setItemInMainHand(item)

        it.sender.sendPrefixMsg(SStr("&aプレゼント"))
    }

    @SCommandBody
    val createFloor = opCommand().addArg(SCommandArg("floor")).addArg(SCommandArg("create")).addArg(SCommandArg(SCommandArgType.STRING).addAlias("内部名").addAlias(DungeonTower.floorData.keys)).setPlayerExecutor {
        if (DungeonTower.floorData.containsKey(it.args[3])){
            it.sender.sendPrefixMsg(SStr("&c既に存在してるよ！"))
            return@setPlayerExecutor
        }
        val meta = it.sender.inventory.itemInMainHand.itemMeta
        val firstLoc = meta.persistentDataContainer[NamespacedKey(DungeonTower.plugin,"firstloc"), PersistentDataType.STRING]
        val secondLoc = meta.persistentDataContainer[NamespacedKey(DungeonTower.plugin,"secondloc"), PersistentDataType.STRING]

        if (firstLoc == null || secondLoc == null){
            if (DungeonTower.floorData.containsKey(it.args[3])){
                CreateFloor(DungeonTower.floorData[it.args[3]]!!.clone()).open(it.sender)
                return@setPlayerExecutor
            }
            it.sender.sendPrefixMsg(SStr("&c範囲指定してね！"))
            return@setPlayerExecutor
        }
        val startLoc = firstLoc.split(",").map { map -> map.toDouble() }
        val endLoc = secondLoc.split(",").map { map -> map.toDouble() }

        if (DungeonTower.floorData.containsKey(it.args[3])){
            val data = DungeonTower.floorData[it.args[3]]!!.clone()
            data.startLoc = Location(DungeonTower.floorWorld,startLoc[0],startLoc[1],startLoc[2])
            data.endLoc = Location(DungeonTower.floorWorld,endLoc[0],endLoc[1],endLoc[2])
            CreateFloor(data).open(it.sender)
            return@setPlayerExecutor
        }
        val data = FloorData()
        data.includeName = it.args[3]
        data.startLoc = Location(DungeonTower.floorWorld,startLoc[0],startLoc[1],startLoc[2])
        data.endLoc = Location(DungeonTower.floorWorld,endLoc[0],endLoc[1],endLoc[2])
        CreateFloor(data).open(it.sender)
    }

    @SCommandBody
    val createTower = opCommand().addArg(SCommandArg("tower")).addArg(SCommandArg("create")).addArg(SCommandArg(SCommandArgType.STRING).addAlias("内部名").addAlias(DungeonTower.towerData.keys)).setPlayerExecutor {
        if (DungeonTower.towerData.containsKey(it.args[3])){
            CreateTower(DungeonTower.towerData[it.args[3]]!!.clone()).open(it.sender)
            return@setPlayerExecutor
        }
        val data = TowerData()
        data.includeName = it.args[3]
        CreateTower(data).open(it.sender)
    }


    @SCommandBody
    val createSpawner = opCommand().addArg(SCommandArg("spawner")).addArg(SCommandArg("create")).addArg(SCommandArg(SCommandArgType.STRING).addAlias("内部名").addAlias(DungeonTower.spawnerData.keys)).setPlayerExecutor {
        if (DungeonTower.spawnerData.containsKey(it.args[3])){
            CreateSpawner(DungeonTower.spawnerData[it.args[3]]!!.clone()).open(it.sender)
            return@setPlayerExecutor
        }
        val data = SpawnerData()
        data.includeName = it.args[3]
        CreateSpawner(data).open(it.sender)
    }

    @SCommandBody
    val createLoot = opCommand().addArg(SCommandArg("loot")).addArg(SCommandArg("create")).addArg(SCommandArg(SCommandArgType.STRING).addAlias("内部名").addAlias(DungeonTower.lootData.keys)).setPlayerExecutor {
        if (DungeonTower.lootData.containsKey(it.args[3])){
            CreateLoot(DungeonTower.lootData[it.args[3]]!!.clone()).open(it.sender)
            return@setPlayerExecutor
        }
        val data = LootData()
        data.includeName = it.args[3]
        CreateLoot(data).open(it.sender)
    }

    @SCommandBody
    val setLobby = opCommand().addArg(SCommandArg("setLobby")).setPlayerExecutor {
        DungeonTower.lobbyLocation = it.sender.location
        DungeonTower.plugin.config.set("lobbyLocation",it.sender.location)
        DungeonTower.plugin.saveConfig()
        it.sender.sendPrefixMsg(SStr("&a設定したよ"))
    }
}