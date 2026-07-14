package net.crewco.mythos.argo

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.power.Power
import net.crewco.mythos.api.power.PowerContext
import net.crewco.mythos.api.story.Beat
import net.crewco.mythos.argo.ArgoContent.ABOARD
import net.crewco.mythos.argo.ArgoContent.ERA
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * **The Argo sails, and it takes everybody standing on it.**
 *
 * Not a moving build — a *moving crew*. Jason sails, and every player within twelve blocks of the
 * ship goes with him, together, hundreds of blocks across the sea. Everyone who wandered off to look
 * at something is left standing on a beach watching it go, which is exactly what happened to Heracles
 * and he never caught up.
 */
class SailPower(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val state: ArgoState,
    private val voyage: Voyage,
) : Power {
    override val id = "sail"
    override val displayName = "Sail"
    override val description = "Everyone aboard goes with you. Everyone who isn't, doesn't. /power sail"
    override val cooldownSeconds = 30

    override fun use(ctx: PowerContext): Boolean {
        val jason = ctx.player

        // First use: the ship is built where he stands and it goes in the water.
        if (state.argo == null) {
            state.argo = jason.location.clone()
            state.save()
            val scar = mythos.terraform.scar("argo:hull")
            for (x in -2..2) for (z in -4..4) {
                scar.set(jason.location.clone().add(x.toDouble(), -1.0, z.toDouble()).block, Material.OAK_PLANKS)
            }
            jason.sendMessage(mm("<gold>The Argo. <gray>Fifty oars, and a beam from a talking tree, which will have opinions later."))
            Bukkit.getServer().sendMessage(mm("<dark_gray>» <gold>The Argo <gray>goes into the water at <white>${jason.location.blockX}, ${jason.location.blockZ}<gray>."))
            Bukkit.getServer().sendMessage(mm("<dark_gray><i>   Stand on it. <white>/claim argonaut <dark_gray><i>— fifty seats, and the ship leaves when he says."))
            mythos.eras.complete(ERA, "the_launch", "the ship went into the water")
            return true
        }

        val crew = voyage.aboard(jason)
        if (crew.size < mythos.dev.threshold(4)) {
            jason.sendMessage(mm("<red>There are ${crew.size} of you. <gray>It is a fifty-oar ship and the sea is a long way across."))
            return false
        }

        val next = voyage.next(state.leg)
        if (next == null) {
            jason.sendMessage(mm("<gold>There is nowhere else to go. <dark_gray><i>You are there. It is smaller than you imagined."))
            return false
        }

        // The Clashing Rocks: you go through when they're apart, or you don't go through.
        if (next.id == "symplegades" && System.currentTimeMillis() > state.windowUntil) {
            jason.sendMessage(mm("<red>They close. <gray>Every time. On everything."))
            jason.sendMessage(mm("<dark_gray><i>Send a bird through first and watch what happens to it. <white>/power dove"))
            return false
        }

        voyage.sailTo(jason, crew, next)
        return true
    }
}

/** You do not test the Clashing Rocks with the ship. You test them with something you can spare. */
class DovePower(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val state: ArgoState,
) : Power {
    override val id = "dove"
    override val displayName = "Send the Dove"
    override val description = "Something you can spare, first. /power dove"
    override val cooldownSeconds = 60

    override fun use(ctx: PowerContext): Boolean {
        val jason = ctx.player
        val dove = jason.world.spawnEntity(jason.location.clone().add(0.0, 2.0, 0.0), EntityType.ALLAY) as LivingEntity
        dove.customName(mm("<white>A Dove"))
        dove.isCustomNameVisible = true

        jason.sendMessage(mm("<gray>You let it go, and it flies at the gap, and the rocks come together on it."))

        context.schedulers.globalDelayed(60) {
            dove.remove()
            state.windowUntil = System.currentTimeMillis() + 30_000
            Bukkit.getServer().sendMessage(mm("<dark_gray>» <gray>The rocks close on the bird, and take its tail feathers, and grind apart again."))
            Bukkit.getServer().sendMessage(mm("<white>Thirty seconds. <dark_gray><i>Row. <white>/power sail"))
        }
        return true
    }
}

/** Everyone rows. It is the only thing the crew can do and it is what wins the whole thing. */
class RowPower(private val mythos: Mythos, private val state: ArgoState) : Power {
    override val id = "row"
    override val displayName = "Row"
    override val description = "Put your back into it. Everybody has to. /power row"
    override val cooldownSeconds = 15

    override fun use(ctx: PowerContext): Boolean {
        val hand = ctx.player
        hand.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, 300, 0, false, false))
        hand.addPotionEffect(PotionEffect(PotionEffectType.DOLPHINS_GRACE, 600, 0, false, false))
        hand.sendMessage(mm("<aqua>You row. <dark_gray><i>You are in the poem. You are in it as a name, in a list, once — and that is better than almost anybody gets."))
        return true
    }
}

/** Three impossible things, and he expects them to kill the boy. */
class SetTaskPower(
    private val mythos: Mythos,
    private val context: AddonContext,
) : Power {
    override val id = "set_task"
    override val displayName = "Set Him Three Things"
    override val description = "Impossible ones. You are not going to give up the fleece. /power set_task"
    override val cooldownSeconds = 0

    override fun use(ctx: PowerContext): Boolean {
        val king = ctx.player
        if (mythos.eras.isComplete(ERA, "the_tasks")) {
            king.sendMessage(mm("<red>You have set them. <gray>He is doing them. <dark_gray><i>That is what is worrying you."))
            return false
        }
        val jason = mythos.roles.holders("jason").mapNotNull { Bukkit.getPlayer(it) }.firstOrNull()
            ?: return false.also { king.sendMessage(mm("<red>He is not here yet.")) }

        // Bulls with bronze feet and fire in them, and then the teeth.
        context.schedulers.entity(jason) {
            repeat(2) { index ->
                val bull = jason.world.spawnEntity(jason.location.clone().add((index * 6 - 3).toDouble(), 0.0, 8.0), EntityType.RAVAGER) as LivingEntity
                bull.customName(mm("<gold>A Bull of Bronze"))
                bull.isCustomNameVisible = true
                bull.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.baseValue = 100.0
                bull.health = 100.0
                bull.fireTicks = Int.MAX_VALUE
                bull.removeWhenFarAway = false
                bull.persistentDataContainer.set(NamespacedKey(context.plugin, "bull"), PersistentDataType.BYTE, 1)
            }
            jason.sendMessage(mm("<red>They breathe fire and their feet are bronze, and you are supposed to plough with them."))
            jason.sendMessage(mm("<dark_gray><i>You are going to be on fire in about four seconds. <gray>Unless somebody has thought ahead."))
        }

        mythos.narrator.tell(
            listOf(
                Beat(20, text = "<dark_gray>» <dark_green>Aeëtes <gray>sets three tasks, and smiles while he does it."),
                Beat(55, text = "<dark_gray><i>Plough a field with bulls that are on fire. Sow the field with dragon's teeth."),
                Beat(55, text = "<dark_gray><i>Kill the men that come up out of the ground. There are always more men."),
                Beat(60, text = "<gray>He expects this to be a funeral. <dark_gray><i>He has not been introduced to his daughter properly."),
            ),
        )
        mythos.chronicle.record("story", "<dark_green>Aeëtes <gray>set Jason three impossible tasks and expected to bury him.")
        mythos.eras.complete(ERA, "the_tasks", "a king set three impossible things")
        return true
    }
}

/**
 * **She does not have to do this.**
 *
 * The unguent is the only reason he survives the bulls. She hands it to him because she wants to, and
 * it is a choice a *player* makes, and she can simply not.
 */
class UnguentPower(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val state: ArgoState,
) : Power {
    override val id = "unguent"
    override val displayName = "The Ointment"
    override val description = "Fireproof, for one day. Nobody has asked you to do this. /power unguent <player>"
    override val cooldownSeconds = 120

    override fun use(ctx: PowerContext): Boolean {
        val her = ctx.player
        val him = ctx.args.firstOrNull()?.let { Bukkit.getPlayerExact(it) }
            ?: return false.also { her.sendMessage(mm("<red>/power unguent <player>")) }

        state.medeaConsented = true
        state.save()

        context.schedulers.entity(him) {
            him.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0, false, false))
            him.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 6000, 1, false, false))
            him.sendMessage(mm("<dark_purple>She rubs something into your skin that smells like a burnt field, and does not explain it."))
            him.sendMessage(mm("<dark_gray><i>You are not going to burn. You did not do that. She did that."))
        }
        her.sendMessage(mm("<dark_purple>You have just betrayed your father for a man you met on Tuesday."))
        her.sendMessage(mm("<dark_gray><i>Nobody made you. Remember that, whatever he says later."))
        mythos.chronicle.record("story", "<dark_purple>Medea <gray>gave Jason the ointment. Nobody asked her to. She chose it.")
        return true
    }
}

/** The thing that never sleeps. She puts it to sleep. */
class CharmPower(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val state: ArgoState,
) : Power {
    override val id = "charm"
    override val displayName = "Sing It Down"
    override val description = "It has never slept. It is going to sleep now. /power charm"
    override val cooldownSeconds = 300

    override fun use(ctx: PowerContext): Boolean {
        val her = ctx.player

        val dragon = her.getNearbyEntities(20.0, 10.0, 20.0)
            .filterIsInstance<LivingEntity>()
            .firstOrNull { it.persistentDataContainer.has(NamespacedKey(context.plugin, "dragon"), PersistentDataType.BYTE) }
            ?: return false.also { her.sendMessage(mm("<red>It is not here. <gray>You would know. Everyone would know.")) }

        dragon.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 1200, 250, false, false))
        dragon.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 1200, 250, false, false))
        dragon.customName(mm("<dark_gray>The Dragon <i>(asleep)"))

        state.medeaConsented = true
        state.save()

        Bukkit.getServer().sendMessage(mm("<dark_gray>» <dark_purple>Medea <gray>sings to the thing that has never once slept, and it sleeps."))
        Bukkit.getServer().sendMessage(mm("<dark_gray><i>   Sixty seconds. Take it off the tree and go."))
        mythos.chronicle.record("story", "<dark_purple>Medea <gray>put the dragon to sleep. It had never slept before.")
        mythos.eras.complete(ERA, "the_dragon", "the thing that never sleeps was sung down")
        return true
    }
}

/**
 * **What she gave up, and what it cost.**
 *
 * She kills her own brother to slow the pursuit. It is the worst thing anyone does in this chapter and
 * she does it *for him*, and he is going to leave her for a better-connected woman in nine years.
 */
class BetrayPower(private val mythos: Mythos, private val state: ArgoState) : Power {
    override val id = "betray"
    override val displayName = "Slow Them Down"
    override val description = "There is a way to stop your father's ships. You know exactly what it is. /power betray"
    override val cooldownSeconds = 0

    override fun use(ctx: PowerContext): Boolean {
        val her = ctx.player
        if (!state.fleeceTaken) {
            her.sendMessage(mm("<red>Nobody is chasing you yet. <gray>Wait until they are."))
            return false
        }
        if (mythos.eras.isComplete(ERA, "the_betrayal")) return false

        mythos.narrator.tell(
            listOf(
                Beat(20, text = "<dark_gray>» <dark_purple>Medea <gray>does something to slow her father's ships down."),
                Beat(60, text = "<dark_gray><i>I am not going to describe it. Every version of this story finds a different way not to."),
                Beat(60, text = "<gray>The ships stop. They stop to gather up what she left in the water.", sound = "minecraft:entity.player.hurt"),
                Beat(65, text = "<dark_gray><i>She did that for him. Remember it in nine years, when he explains that he is marrying somebody else."),
            ),
        )
        mythos.roles.holders("aeetes").mapNotNull { Bukkit.getPlayer(it) }.forEach { father ->
            father.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 2400, 2, false, false))
            father.sendMessage(mm("<dark_green>You stop. <dark_gray><i>You have to stop. You know what she has left in the water for you."))
        }
        mythos.chronicle.record("story", "<dark_purple>Medea <gray>gave up everything she had for a man who would not keep it.")
        mythos.eras.complete(ERA, "the_betrayal", "she gave up everything for him")
        return true
    }
}
