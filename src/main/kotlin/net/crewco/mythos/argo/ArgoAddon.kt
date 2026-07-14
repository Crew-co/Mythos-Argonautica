package net.crewco.mythos.argo

import net.crewco.mythos.addon.AddonBase
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.ext.consume
import net.crewco.mythos.command.CommandContext.Companion.mm
import java.io.File

/**
 * Story #9 — **the first one that does not work without a crowd.**
 *
 * Every chapter so far has had one or two players at the centre and the rest watching. This one needs
 * fifty people on a boat, and it is the first time the *server* is the protagonist.
 *
 * And Medea is under no obligation. She is a player. She can decline — and if she does, the bulls burn
 * him, the dragon never sleeps, the fleece stays on its tree, and the addon narrates that ending
 * without a word of complaint. **The most important character in the chapter is one who can simply say
 * no**, and no amount of being the hero gets you round her.
 */
class ArgoAddon : AddonBase() {

    override fun onEnable() {
        val mythos = Mythos.from(context)
        val state = ArgoState(File(context.dataFolder, "argo.yml"))
        val voyage = Voyage(mythos, context, state)

        mythos.eras.register(ArgoContent.VOYAGE)

        mythos.roles.register(ArgoContent.JASON)
        mythos.roles.register(ArgoContent.MEDEA)
        mythos.roles.register(ArgoContent.ARGONAUT)
        mythos.roles.register(ArgoContent.AEETES)

        mythos.powers.register(SailPower(mythos, context, state, voyage))
        mythos.powers.register(DovePower(mythos, context, state))
        mythos.powers.register(RowPower(mythos, state))
        mythos.powers.register(SetTaskPower(mythos, context))
        mythos.powers.register(UnguentPower(mythos, context, state))
        mythos.powers.register(CharmPower(mythos, context, state))
        mythos.powers.register(BetrayPower(mythos, state))

        context.registerListener(voyage)

        // The voyage is a LIST, and lists can be added to. Talos on the beach. The Sirens. The Harpies.
        mythos.extensions.consume<Landfall>(Landfall.POINT) { landfall ->
            voyage.landfalls += landfall
            context.logger.info("A leg of the voyage was added by another addon: ${landfall.name}")
        }

        listOf(
            Landfall("lemnos", "Lemnos", "An island of women who killed all their husbands. They are being extremely hospitable and nobody is relaxed.", 10),
            Landfall("harpies", "Salmydessus", "There is a blind old prophet here and something keeps stealing his dinner.", 20),
            Landfall(
                "symplegades", "The Clashing Rocks",
                "Two cliffs that come together on anything that goes between them. There is no third option and no way round.",
                30, distance = 900.0,
            ),
            Landfall(
                "colchis", "Colchis", "The end of the world. There is a king here, and a fleece, and a dragon, and a daughter.",
                40, distance = 1200.0,
            ) { crew ->
                // The grove, the tree, the fleece and the thing under it.
                crew.firstOrNull()?.let { voyage.hangTheFleece(it.location.clone().add(20.0, 0.0, 20.0)) }
                crew.forEach { it.sendMessage(mm("<dark_gray><i>The fleece is on a tree, twenty blocks that way. The thing under the tree does not sleep.")) }
            },
            Landfall("iolcus", "Home", "Nine years, and a sheepskin, and a woman who gave up everything to be standing on this deck.", 50, distance = 1500.0),
        ).forEach { voyage.landfalls += it }

        // Orpheus was on the Argo. He is somebody else's role, in somebody else's jar, and he out-sang
        // the Sirens — so his lament works on more than the dead.
        context.schedulers.globalDelayed(1) {
            mythos.roles.extend("orpheus") { it.copy(powers = (it.powers + "row").distinct()) }
        }

        // Fifty seats, and the crowd is the point.
        context.schedulers.globalRepeating(200, 200) {
            if (mythos.eras.currentId() != ArgoContent.ERA) return@globalRepeating
            if (mythos.eras.isComplete(ArgoContent.ERA, "the_crew")) return@globalRepeating
            val signed = mythos.roles.holders("argonaut").size
            if (signed >= mythos.dev.threshold(20)) {
                mythos.eras.complete(ArgoContent.ERA, "the_crew", "fifty of them signed on to a boat that did not exist yet")
            }
        }

        context.logger.info("The Argo. Fifty oars, and one person on board who can say no.")
    }
}
