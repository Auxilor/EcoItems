package com.willfp.ecoitems.proxy.v26_1_2

import com.willfp.ecoitems.nms.ComponentResult
import com.willfp.ecoitems.nms.ItemComponentsProxy
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.ShortTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack as NmsItemStack
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack

class ItemComponents : ItemComponentsProxy {
    override fun withComponents(item: ItemStack, components: Map<String, Any?>): ComponentResult {
        val errors = mutableListOf<String>()
        val nms = CraftItemStack.asNMSCopy(item)
        val ops = RegistryOps.create(NbtOps.INSTANCE, MinecraftServer.getServer().registryAccess())

        for ((key, value) in components) {
            try {
                apply(nms, key, value, ops)
            } catch (e: IllegalArgumentException) {
                errors += "$key: ${e.message}"
            }
        }

        return ComponentResult(CraftItemStack.asCraftMirror(nms), errors)
    }

    @Suppress("UNCHECKED_CAST")
    private fun apply(stack: NmsItemStack, key: String, value: Any?, ops: RegistryOps<Tag>) {
        val id = Identifier.tryParse(key)
            ?: throw IllegalArgumentException("invalid component id")

        val type = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(id).orElseThrow {
            IllegalArgumentException("unknown component")
        } as DataComponentType<Any>

        val codec = type.codec()
            ?: throw IllegalArgumentException("component cannot be set on items")

        val parsed = codec.parse(ops, value.toTag())
        val parsedValue = parsed.result().orElseThrow {
            IllegalArgumentException(parsed.error().map { it.message() }.orElse("invalid value"))
        }

        stack.set(type, parsedValue)
    }

    private fun Any?.toTag(): Tag = when (this) {
        is String -> StringTag.valueOf(this)
        is Boolean -> ByteTag.valueOf(this)
        is Byte -> ByteTag.valueOf(this)
        is Short -> ShortTag.valueOf(this)
        is Int -> IntTag.valueOf(this)
        is Long -> LongTag.valueOf(this)
        is Float -> FloatTag.valueOf(this)
        is Double -> DoubleTag.valueOf(this)
        is Iterable<*> -> ListTag().also { list -> forEach { list.add(it.toTag()) } }
        is Map<*, *> -> CompoundTag().also { tag ->
            for ((key, value) in this) {
                tag.put(key.toString(), value.toTag())
            }
        }
        else -> throw IllegalArgumentException("unsupported value: $this")
    }
}
