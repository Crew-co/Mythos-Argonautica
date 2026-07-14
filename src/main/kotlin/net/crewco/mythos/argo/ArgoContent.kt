package net.crewco.mythos.argo

import net.crewco.mythos.api.era.EraDefinition
import net.crewco.mythos.api.era.Objective
import net.crewco.mythos.api.role.ClaimRules
import net.crewco.mythos.api.role.Endurance
import net.crewco.mythos.api.role.RoleDefinition
import net.crewco.mythos.api.role.RoleTier
import net.crewco.mythos.api.story.beats
import net.crewco.mythos.api.story.line
import net.crewco.mythos.api.story.pause
import net.crewco.mythos.api.story.title

/**
 * Story #9 — **the first one that needs a crowd.**
 *
 * Every chapter so far has had one or two players at the centre and everyone else watching. This one
 * doesn't work at all without fifty people on a boat, and it is the first time the server itself is
 * the protagonist.
 *
 * And at the middle of it is **Medea**, who is under no obligation whatsoever. She is a player. She
 * can simply decline, and the expedition dies in a field outside Colchis, and that is a legitimate
 * ending which the addon will narrate without complaint.
 */
object ArgoContent {

    const val ERA = "the-golden-fleece"
    const val ABOARD = "argo.aboard"

    val VOYAGE = EraDefinition(
        id = ERA,
        displayName = "The Golden Fleece",
        order = 6,
        next = "the-labyrinth",
        subtitle = "fifty of them, and one of her",
        lore = listOf(
            "A king who stole a throne is told to fear a man with one sandal. A man arrives with one sandal.",
            "So he sends him to the end of the world for a sheepskin, and expects that to be the end of it.",
        ),
        prologue = beats {
            pause(20)
            title("<gold>The Golden Fleece", "<gray>fifty of them, and one of her", sound = "minecraft:entity.boat.paddle_water")
            pause(50)
            line("<gray>A king who stole a throne was told to fear a man with one sandal.", delayTicks = 50)
            line("<dark_gray><i>A young man walks into the city having lost a sandal in a river, helping an old woman across.", delayTicks = 60)
            pause(45)
            line("<gray>So the king sends him to the end of the world for a sheepskin, and expects that to be that.", delayTicks = 55)
            pause(40)
            line("<white>/claim jason <dark_gray>· <white>/claim argonaut <dark_gray>— <gray>fifty seats on one boat. Take one.", delayTicks = 35)
            line("<dark_gray><i>And somebody should take Medea. She is the only person in this who has a choice.", delayTicks = 45)
        },
        epilogue = beats {
            pause(30)
            title("<dark_red>And Then He Left Her", "<gray>after everything", delayTicks = 20, sound = "minecraft:entity.ender_dragon.growl")
            pause(60)
            line("<gray>She betrayed her father for him. She killed her own brother to slow the pursuit.", delayTicks = 55)
            line("<gray>She gave him the fleece, the ship, the kingdom and ten years.", delayTicks = 50)
            pause(45)
            line("<dark_gray><i>And then he found somebody better connected, and explained to her, reasonably, that it was for the best.", delayTicks = 60)
            pause(50)
            line("<gray>Everything that happens next, he earned.", delayTicks = 55)
            pause(60)
        },
        objectives = listOf(
            Objective("the_crew", "Fifty of them sign on to a boat that has not been built"),
            Objective("the_launch", "The Argo goes into the water"),
            Objective("the_rocks", "Something that closes is gone through anyway"),
            Objective("the_tasks", "A king sets three impossible tasks, expecting to be rid of him"),
            Objective("the_dragon", "The thing that never sleeps is put to sleep"),
            Objective("the_fleece", "A sheepskin is taken off a tree"),
            Objective("the_betrayal", "She gives up everything she has for a man who will not keep it", hidden = true),
        ),
    )

    val JASON = RoleDefinition(
        id = "jason",
        displayName = "Jason",
        tier = RoleTier.HERO,
        era = ERA,
        domains = listOf("the ship", "the crew", "other people's competence"),
        color = "<gold>",
        lore = listOf(
            "You will complete every task set to you, and you will not personally solve a single one of them.",
            "Every single thing you achieve is done by somebody else, for you, because they like you. Think about that.",
        ),
        powers = listOf("sail", "dove"),
        claimRules = listOf(ClaimRules.sinceEra(ERA), ClaimRules.essenceCost(30)),
        endurance = Endurance.ETERNAL,
    )

    /**
     * **Medea, who does not have to help him.**
     *
     * Every other obstacle in this chapter is solved *by her*: the bulls, the teeth, the dragon, the
     * escape. She is a player, she is under no obligation, and if she declines the expedition simply
     * fails and the addon narrates that ending without a word of complaint.
     *
     * Which makes what he does to her afterwards the single most damning thing any hero does in Greek
     * myth — and the reason it is the *only* chapter where the epilogue takes a side.
     */
    val MEDEA = RoleDefinition(
        id = "medea",
        displayName = "Medea",
        tier = RoleTier.DEMIGOD,
        era = ERA,
        domains = listOf("the drug", "the choice", "the price"),
        color = "<dark_purple>",
        lore = listOf(
            "Granddaughter of the sun. You know every plant that grows and what it does to a man.",
            "Nothing in this story works without you, and everyone in it is going to treat you as a complication.",
        ),
        powers = listOf("charm", "unguent", "betray"),
        claimRules = listOf(ClaimRules.sinceEra(ERA), ClaimRules.essenceCost(50)),
        endurance = Endurance.ETERNAL,
    )

    /** Fifty seats. This is where the server goes. */
    val ARGONAUT = RoleDefinition(
        id = "argonaut",
        displayName = "An Argonaut",
        tier = RoleTier.HERO,
        era = ERA,
        domains = listOf("the oar", "the long way"),
        maxHolders = 50,
        color = "<aqua>",
        lore = listOf(
            "You rowed. You are in the poem — most of you are in it as a *name*, in a list, once.",
            "That is still better than almost everybody gets.",
        ),
        powers = listOf("row"),
        claimRules = listOf(ClaimRules.sinceEra(ERA)),
        endurance = Endurance.ERA,
    )

    val AEETES = RoleDefinition(
        id = "aeetes",
        displayName = "Aeëtes",
        tier = RoleTier.MORTAL,
        era = ERA,
        domains = listOf("the fleece", "three impossible things"),
        color = "<dark_green>",
        lore = listOf(
            "You have the fleece, a dragon that does not sleep, and a daughter who is cleverer than you.",
            "You are going to lose all three in one night, and it is going to be the third one that does it.",
        ),
        powers = listOf("set_task"),
        claimRules = listOf(ClaimRules.sinceEra(ERA)),
        endurance = Endurance.ERA,
    )
}
