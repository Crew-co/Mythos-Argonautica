package net.crewco.mythos.argo

import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/** Where the ship is, how far it's got, and whether she said yes. */
class ArgoState(private val file: File) {

    private val yaml = if (file.exists()) YamlConfiguration.loadConfiguration(file) else YamlConfiguration()

    @Volatile var argo: Location? = yaml.getLocation("argo")
    @Volatile var leg: Int = yaml.getInt("leg", 0)

    /** Rocks are apart. Sail NOW. */
    @Volatile var windowUntil: Long = 0

    /** She agreed to help. She did not have to. */
    @Volatile var medeaConsented: Boolean = yaml.getBoolean("consented", false)

    @Volatile var fleeceTaken: Boolean = yaml.getBoolean("fleece", false)

    @Synchronized
    fun save() {
        yaml.set("argo", argo)
        yaml.set("leg", leg)
        yaml.set("consented", medeaConsented)
        yaml.set("fleece", fleeceTaken)
        runCatching { yaml.save(file) }
    }

    fun clear() {
        argo = null
        leg = 0
        windowUntil = 0
        medeaConsented = false
        fleeceTaken = false
        save()
    }
}
