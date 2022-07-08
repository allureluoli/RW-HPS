/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package cn.rwhps.server.io.packet

import cn.rwhps.server.io.GameInputStream
import cn.rwhps.server.io.GameOutputStream
import cn.rwhps.server.struct.SerializerTypeAll
import cn.rwhps.server.util.PacketType
import cn.rwhps.server.util.inline.toStringHex
import cn.rwhps.server.util.log.Log
import java.io.IOException

/**
 * @author RW-HPS/Dr
 */
class Packet(type0: Int, @JvmField val bytes: ByteArray) {
    val type = PacketType.from(type0)

    init {
        if (type == PacketType.NOT_RESOLVED) {
            Log.fatal("ERROR , $type0")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is PacketType) {
            return other.name == type.name && other.typeInt == type.typeInt
        }
        return false
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    /**
     * Return detailed Packet data
     * @return Packet String
     */
    override fun toString(): String {
        return  """
                Packet{
                    Bytes=${bytes.contentToString()}
                    BytesHex=${bytes.toStringHex()}
                    type=${type}
                }
                """.trimIndent()
    }

    companion object {
        /**
         * 序列化 反序列化 Packet
         * 与网络传输不相同
         */
        /**
         *    1 2 3 4  5  6  7  8  ...
         *   +-+-+-+-+-+-+-+-+---------------+
         *   |0|0|0|0|0 |0 |0 |0 | Data|
         *   +-+-+-+-+-+-+-+-+---------------+
         *   |  Type |Data length| Data
         *   +---------------+---------------+
         */
        internal val serializer = object : SerializerTypeAll.TypeSerializer<Packet> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: Packet) {
                stream.writeInt(objectData.type.typeInt)
                stream.writeBytesAndLength(objectData.bytes)
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): Packet {
                return Packet(stream.readInt(), stream.readStreamBytes())
            }
        }
    }
}