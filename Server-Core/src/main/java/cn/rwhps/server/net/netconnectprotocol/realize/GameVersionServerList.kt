/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package cn.rwhps.server.net.netconnectprotocol.realize

import cn.rwhps.server.data.global.Data
import cn.rwhps.server.data.global.NetStaticData
import cn.rwhps.server.data.player.Player
import cn.rwhps.server.io.GameInputStream
import cn.rwhps.server.io.GameOutputStream
import cn.rwhps.server.io.output.CompressOutputStream
import cn.rwhps.server.io.packet.Packet
import cn.rwhps.server.net.core.ConnectionAgreement
import cn.rwhps.server.struct.Seq
import cn.rwhps.server.util.PacketType
import cn.rwhps.server.util.game.CommandHandler
import cn.rwhps.server.util.log.Log
import java.io.IOException
import kotlin.math.min

class GameVersionServerList(connectionAgreement: ConnectionAgreement) : GameVersionServer(connectionAgreement) {

    private var versionCil = 170

    override fun getPlayerInfo(p: Packet): Boolean {
        GameInputStream(p).use { stream ->
            stream.readString()
            stream.readInt()
            versionCil = stream.readInt()
            stream.readInt()
            var name = stream.readString()
            Log.debug("name", name)
            val passwd = stream.isReadString()
            Log.debug("passwd", passwd)
            stream.readString()
            val uuid = stream.readString()
            Log.debug("uuid", uuid)
            Log.debug("?", stream.readInt())
            val token = stream.readString()
            Log.debug("token", token)
            Log.debug(token, connectKey!!)

            sendTeamData0(team())
            sendServerInfo0(false)


            sendSystemMessage("对着你需要的信息点击, 就和操作玩家一样\n点击踢出就可以得到相应的信息")
        }
        return true
    }

    private fun team(): CompressOutputStream {
        val enc = CompressOutputStream.getGzipOutputStream("teams", true)
         for (player in adList.items) {
            try {
                if (player == null) {
                    enc.writeBoolean(false)
                } else {
                    enc.writeBoolean(true)
                    enc.writeInt(0)
                     if (versionCil == 151) {
                         writePlayer(player, enc)
                     } else {
                         NetStaticData.RwHps.abstractNetPacket.writePlayer(player, enc)
                     }

                }
            } catch (e: Exception) {
                Log.error("[ALL/Player] Get Server Team Info", e)
            }
        }
        return enc
    }

    private fun sendTeamData0(gzip: CompressOutputStream) {
        try {
            val o = GameOutputStream()
            /* Player position */
            o.writeInt(0)
            o.writeBoolean(Data.game.isStartGame)
            /* Largest player */
            o.writeInt(100)
            o.flushEncodeData(gzip)
            /* 迷雾 */
            o.writeInt(Data.game.mist)
            o.writeInt(Data.game.credits)
            o.writeBoolean(true)
            /* AI Difficulty ?*/
            o.writeInt(1)
            o.writeByte(5)
            o.writeInt(Data.config.MaxUnit)
            o.writeInt(Data.config.MaxUnit)
            /* 初始单位 */
            o.writeInt(Data.game.initUnit)
            /* 倍速 */
            o.writeFloat(Data.game.income)
            /* NO Nukes */
            o.writeBoolean(Data.game.noNukes)
            o.writeBoolean(false)
            o.writeBoolean(false)
            /* 共享控制 */
            o.writeBoolean(Data.game.sharedControl)
            /* 游戏暂停 */
            o.writeBoolean(Data.game.gamePaused)
            sendPacket(o.createPacket(PacketType.TEAM_LIST))
        } catch (e: IOException) {
            Log.error("Team", e)
        }
    }

    private fun sendServerInfo0(utilData: Boolean) {
        val o = GameOutputStream()
        o.writeString(Data.SERVER_ID)
        o.writeInt(supportedVersionInt)
        /* 地图 */
        o.writeInt(Data.game.maps.mapType.ordinal)
        o.writeString(Data.game.maps.mapPlayer + Data.game.maps.mapName)
        o.writeInt(Data.game.credits)
        o.writeInt(Data.game.mist)
        o.writeBoolean(true)
        o.writeInt(1)
        o.writeByte(7)
        o.writeBoolean(false)
        /* Admin Ui */
        o.writeBoolean(true)
        o.writeInt(Data.config.MaxUnit)
        o.writeInt(Data.config.MaxUnit)
        o.writeInt(Data.game.initUnit)
        o.writeFloat(Data.game.income)
        /* NO Nukes */
        o.writeBoolean(Data.game.noNukes)
        o.writeBoolean(false)
        o.writeBoolean(utilData)
        if (utilData) {
            o.flushEncodeData(Data.utilData)
        }

        /* 共享控制 */
        o.writeBoolean(Data.game.sharedControl)
        o.writeBoolean(false)
        o.writeBoolean(false)
        // 允许观众
        o.writeBoolean(true)
        o.writeBoolean(false)
        sendPacket(o.createPacket(PacketType.SERVER_INFO))
    }

    @Throws(IOException::class)
    override fun receiveChat(p: Packet) {
        GameInputStream(p).use { stream ->
            val message: String = stream.readString()
            var response: CommandHandler.CommandResponse? = null

            // Msg Command
            if (message.startsWith(".") || message.startsWith("-") || message.startsWith("_")) {
                val strEnd = min(message.length, 3)
                val args = message.substring(if ("qc" == message.substring(1, strEnd)) 5 else 1).split(" ")
                if (args[0].equals("kick",ignoreCase = true)) {
                    if (args.size > 1) {
                        try {
                            val info = adInfoList[args[1].toInt() -1]
                            if (info.isEmpty()) {
                                return@use
                            }
                            sendRelayServerType(info) {}
                        } catch (e : Exception) {
                        }
                    }
                }
            }
        }
    }

    fun writePlayer(player: Player, stream: GameOutputStream) {
        if (Data.game.isStartGame) {
            stream.writeByte(player.site)
            stream.writeInt(player.ping)
            stream.writeBoolean(Data.game.sharedControl)
            stream.writeBoolean(player.controlThePlayer)
            return
        }
        stream.writeByte(player.site)
        // 并没有什么用
        stream.writeInt(player.credits)
        stream.writeInt(player.team)
        stream.writeBoolean(true)
        stream.writeString(player.name)
        stream.writeBoolean(false)

        /* -1 N/A  -2 -   -99 HOST */
        stream.writeInt(player.ping)
        stream.writeLong(System.currentTimeMillis())

        /* Is AI */
        stream.writeBoolean(false)
        /* AI Difficu */
        stream.writeInt(0)

        stream.writeInt(player.site)
        stream.writeByte(0)

        /* 共享控制 */
        stream.writeBoolean(Data.game.sharedControl)
        /* 是否掉线 */
        stream.writeBoolean(player.sharedControl)

        /* 是否投降 */
        stream.writeBoolean(false)
        stream.writeBoolean(false)
        stream.writeInt(-9999)
        stream.writeBoolean(false)
        // 延迟后显示 （HOST) [房主]
        stream.writeInt(if (player.isAdmin) 1 else 0)
    }

    companion object {
        val adList = Seq<Player>(100)
        val adInfoList = Seq<String>(100)

        fun addAD(ad: String, info: String) {
            if (adList.size() >= 99) {
                return
            }
            val ad = Player(null,"",ad, Data.i18NBundle)
            ad.team = 1
            ad.site = adList.size()
            ad.ping = 0
            adList.add(ad)
            adInfoList.add(info)
        }


        init {
            addAD("这里是战队信息-数据来源[中文网]","")
            addAD("星联","751683594")
            addAD("起源","1159654906")
            addAD("鹤影","695487561")
            addAD("逆风者","432780995")
            addAD("E.C.A","932050316")
            addAD("这里是游戏交流群-数据来源[中文网]","")
            addAD("[无数据]","")
            addAD("这里是Mod群-数据来源[中文网/群]","")
            addAD("白菜-模组","[ 657468243 ]\n白菜的模组交流群\n可以玩到还在测试的白菜模组\n群友们可以一起聊天打铁\n不定期举办活动")
            addAD("copyright RCN. ","")
            addAD("仅供测试","")
            addAD("目标是提供一个公告版","")
            addAD("RW-HPS@der.kim","")
        }
    }
}