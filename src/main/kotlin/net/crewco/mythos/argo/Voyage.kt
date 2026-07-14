package net.crewco.mythos.argo

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.event.MythosResetEvent
import net.crewco.mythos.api.story.Beat
import net.crewco.mythos.argo.ArgoContent.ERA
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The voyage: a list of places, in order, and a boat that takes everybody standing on it.
 */
class Voyage(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val state: ArgoState,
) : Listener {

    val landfalls = CopyOnWriteArrayList<Landfall>()

    fun next(afterOrder: Int): Landfall? =
        landfalls.filter { it.order > afterOrder }.minByOrNull { it.order }

    /**
     * Everybody standing on the ship. Wander off to look at something and the ship leaves without
     * you — which is exactly what happened to Heracles, and he never caught up.
     */
    fun aboard(jason: Player): List<Player> {
        val argo = state.argo ?: return emptyList()
        return jason.getNearbyEntities(14.0, 8.0, 14.0)
            .filterIsInstance<Player>()
            .filter { mythos.roles.roleOf(it.uniqueId)?.id in setOf("argonaut", "medea", "orpheus") }
            .plus(jason)
    }

    fun sailTo(jason: Player, crew: List<Player>, landfall: Landfall) {
        val from = jason.location.clone()
        val angle = Math.random() * Math.PI * 2
        val shore = Location(
            from.world,
            from.x + Math.cos(angle) * landfall.distance,
            from.y,
            from.z + Math.sin(angle) * landfall.distance,
        ).apply { y = from.world.getHighestBlockYAt(this) + 1.0 }

        state.argo = shore
        state.leg = landfall.order
        state.save()

        crew.forEach { hand ->
            hand.teleportAsync(shore.clone().add(Math.random() * 6 - 3, 0.0, Math.random() * 6 - 3)).thenRun {
                context.schedulers.entity(hand) {
                    hand.sendMessage(mm("<aqua>Land."))
                }
            }
        }

        mythos.narrator.tell(
            listOf(
                Beat(20, text = "<dark_gray>» <gold>The Argo <gray>comes in at <white>${landfall.name}<gray>, with <white>${crew.size}</white> aboard.", sound = "minecraft:entity.boat.paddle_water"),
                Beat(55, text = "<dark_gray><i>${landfall.brief}"),
            ),
        )
        mythos.chronicle.record("story", "<gold>The Argo <gray>reached ${landfall.name} with ${crew.size} aboard.")

        context.schedulers.globalDelayed(60) { landfall.onArrival(crew) }
        if (landfall.id == "symplegades") mythos.eras.complete(ERA, "the_rocks", "they went through anyway")
    }

    // ---- the fleece ----------------------------------------------------------

    /** It is on a tree, and the thing under the tree does not sleep. Unless somebody sings to it. */
    fun hangTheFleece(at: Location) {
        val scar = mythos.terraform.scar("argo:grove")
        scar.set(at.clone().add(0.0, 3.0, 0.0).block, Material.GOLD_BLOCK)
        for (y in 0..2) scar.set(at.clone().add(0.0, y.toDouble(), 0.0).block, Material.OAK_LOG)

        val dragon = at.world.spawnEntity(at.clone().add(2.0, 0.0, 0.0), EntityType.WARDEN) as LivingEntity
        dragon.customName(mm("<dark_green>The Dragon That Does Not Sleep"))
        dragon.isCustomNameVisible = true
        dragon.removeWhenFarAway = false
        dragon.persistentDataContainer.set(NamespacedKey(context.plugin, "dragon"), PersistentDataType.BYTE, 1)
    }

    @EventHandler
    fun onTakeFleece(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.type != Material.GOLD_BLOCK) return
        if (state.fleeceTaken) return
        if (mythos.eras.currentId() != ERA) return

        // The dragon has to actually be asleep. She has to have said yes.
        if (!state.medeaConsented) {
            event.player.sendMessage(mm("<red>It is awake. <gray>It has always been awake. <dark_gray><i>It has never once slept."))
            event.player.sendMessage(mm("<dark_gray><i>There is exactly one person who can change that, and she does not owe you anything."))
            return
        }

        event.isCancelled = true
        block.type = Material.AIR
        state.fleeceTaken = true
        state.save()

        event.player.inventory.addItem(fleece(context))
        mythos.narrator.tell(
            listOf(
                Beat(20, title = "<gold>The Golden Fleece", subtitle = "<gray>a sheepskin, off a tree", sound = "minecraft:block.beacon.activate"),
                Beat(60, text = "<gray>It is a sheepskin. It is a sheepskin on a tree, and men have died for it, and men will."),
                Beat(60, text = "<dark_gray><i>Run. Her father is already coming, and he has ships."),
            ),
        )
        mythos.chronicle.record("story", "<gold>The Golden Fleece <gray>was taken off the tree by <white>${event.player.name}<gray>.")
        mythos.eras.complete(ERA, "the_fleece", "a sheepskin was taken off a tree")

        mythos.roles.holders("medea").mapNotNull { Bukkit.getPlayer(it) }.forEach { her ->
            context.schedulers.entity(her) {
                her.sendMessage(mm("<dark_purple>They have it. <gray>Your father's ships are already in the water."))
                her.sendMessage(mm("<dark_gray><i>There is a way to slow them down and you know exactly what it is. <white>/power betray"))
            }
        }
    }

    @EventHandler
    fun onBullDeath(event: EntityDeathEvent) {
        val key = NamespacedKey(context.plugin, "bull")
        if (!event.entity.persistentDataContainer.has(key, PersistentDataType.BYTE)) return
        event.drops.clear()
        event.droppedExp = 0

        // Sow the teeth. Men come up out of the ground, and there are always more men.
        val at = event.entity.location
        context.schedulers.regionDelayed(at, 40) {
            repeat(4) { index ->
                val sown = at.world.spawnEntity(at.clone().add(index.toDouble() * 2 - 3, 0.0, 2.0), EntityType.SKELETON) as LivingEntity
                sown.customName(mm("<gray>A Sown Man"))
                sown.equipment?.setItemInMainHand(ItemStack(Material.IRON_SWORD))
                sown.removeWhenFarAway = false
            }
            at.world.players.forEach { near ->
                near.sendMessage(mm("<gray>Armed men come up out of the furrows. <dark_gray><i>Throw a rock among them. They will do the rest."))
            }
        }
    }

    @EventHandler
    fun onReset(event: MythosResetEvent) {
        if (event.scope == MythosResetEvent.Scope.PLAYER) return
        state.clear()
        mythos.terraform.heal("argo:hull")
        mythos.terraform.heal("argo:grove")
        context.logger.info("The Argo was never built. Nobody has been betrayed yet.")
    }

    companion object {
        fun fleece(context: AddonContext): ItemStack = ItemStack(Material.GOLD_BLOCK).apply {
            editMeta { meta ->
                meta.displayName(mm("<!i><gold>The Golden Fleece"))
                meta.lore(
                    listOf(
                        mm("<!i><dark_gray><i>It is a sheepskin."),
                        mm("<!i><dark_gray><i>Men have died for it, and men will, and it is a sheepskin."),
                    ),
                )
            }
        }
    }
}
