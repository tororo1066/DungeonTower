package tororo1066.dungeontower.data

import java.util.UUID

class UserData() {
    lateinit var uuid: UUID
    var mcid = ""

    constructor(uuid: UUID, mcid: String) : this() {
        this.uuid = uuid
        this.mcid = mcid
    }
}