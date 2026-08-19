package cg.headpop.waypointrpg

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Keeps private glow applied even when Typewriter refreshes a packet-only NPC's metadata. */
@Singleton
object WaypointGlowController : Initializable {
    private data class GlowKey(val playerId: UUID, val entityId: Int)
    private class GlowState(@Volatile var baseFlags: Byte) {
        val references = AtomicInteger(1)
    }
    private val activeGlows = ConcurrentHashMap<GlowKey, GlowState>()

    private val listener = object : PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
        override fun onPacketSend(event: PacketSendEvent) {
            if (event.packetType != PacketType.Play.Server.ENTITY_METADATA) return
            val player = event.getPlayer<Any>() as? Player ?: return
            val wrapper = WrapperPlayServerEntityMetadata(event)
            val key = GlowKey(player.uniqueId, wrapper.entityId)
            val state = activeGlows[key] ?: return
            val metadata = wrapper.entityMetadata.toMutableList()
            val flagsIndex = metadata.indexOfFirst { it.index == 0 && it.value is Byte }
            if (flagsIndex < 0) return
            val incoming = metadata[flagsIndex].value as Byte
            val preserved = ((incoming.toInt() and 0x40.inv()) or (state.baseFlags.toInt() and 0x40)).toByte()
            state.baseFlags = preserved
            metadata[flagsIndex] = EntityData(0, EntityDataTypes.BYTE, (preserved.toInt() or 0x40).toByte())
            wrapper.entityMetadata = metadata
        }
    }

    override suspend fun initialize() {
        runCatching { PacketEvents.getAPI().eventManager.registerListener(listener) }
            .onFailure { Bukkit.getLogger().warning("[WaypointRPG] Could not register NPC glow metadata listener: ${it.message}") }
    }

    override suspend fun shutdown() {
        runCatching { PacketEvents.getAPI().eventManager.unregisterListener(listener) }
        activeGlows.clear()
    }

    fun activate(player: Player, entityId: Int, base: Byte) {
        val key = GlowKey(player.uniqueId, entityId)
        val state = activeGlows.compute(key) { _, current ->
            current?.also {
                it.references.incrementAndGet()
                it.baseFlags = ((base.toInt() and 0x40.inv()) or (it.baseFlags.toInt() and 0x40)).toByte()
            } ?: GlowState(base)
        }!!
        send(player, entityId, (state.baseFlags.toInt() or 0x40).toByte())
    }

    fun deactivate(player: Player, entityId: Int) {
        val key = GlowKey(player.uniqueId, entityId)
        var restore: Byte? = null
        activeGlows.computeIfPresent(key) { _, state ->
            if (state.references.decrementAndGet() <= 0) {
                restore = state.baseFlags
                null
            } else state
        }
        restore?.let { send(player, entityId, it) }
    }

    fun clearPlayer(playerId: UUID) {
        activeGlows.keys.removeIf { it.playerId == playerId }
    }

    private fun send(player: Player, entityId: Int, flags: Byte) {
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(
                WrapperPlayServerEntityMetadata(
                    entityId,
                    listOf(EntityData(0, EntityDataTypes.BYTE, flags)),
                )
            )
        }.onFailure {
            Bukkit.getLogger().warning("[WaypointRPG] Private glow update failed for entity $entityId: ${it.message}")
        }
    }
}
