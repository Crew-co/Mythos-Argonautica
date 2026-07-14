package net.crewco.mythos.argo

/**
 * **A place the Argo stops.**
 *
 * The voyage is a list of landfalls, and this addon ships five — but the whole point of the poem is
 * that it is a *list*, and lists can be added to. Lemnos. The Harpies. The Clashing Rocks. Talos, on
 * the way home. Any jar can post another one and it becomes a leg of the voyage, in order, with the
 * whole crew aboard.
 *
 * ```kotlin
 * mythos.extensions.contribute(Landfall.POINT, Landfall(
 *     id = "talos",
 *     name = "Talos",
 *     brief = "There is a bronze man on that beach and he is throwing rocks at the ship.",
 *     order = 60,
 *     onArrival = { crew -> /* spawn him */ },
 * ))
 * ```
 */
data class Landfall(
    val id: String,
    val name: String,
    /** Read out to the whole crew as the ship comes in. */
    val brief: String,
    /** Where in the voyage. The built-in legs are 10, 20, 30, 40, 50. */
    val order: Int,
    /** How far from the last stop, in blocks. The sea is big. */
    val distance: Double = 600.0,
    /** Called on the global region with everyone aboard, once the ship arrives. */
    val onArrival: (List<org.bukkit.entity.Player>) -> Unit = {},
) {
    companion object {
        const val POINT = "argo:landfalls"
    }
}
