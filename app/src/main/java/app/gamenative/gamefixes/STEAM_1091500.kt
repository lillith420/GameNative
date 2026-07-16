package app.gamenative.gamefixes

import android.content.Context
import app.gamenative.data.GameSource
import com.winlator.container.Container
import com.winlator.core.envvars.EnvVars
import timber.log.Timber

/**
 * Cyberpunk 2077 (Steam, appId 1091500).
 *
 * Tuned for GPU-bound playback on Adreno 660 / Snapdragon 888 (e.g. Galaxy Z Fold 3).
 *
 * Each value is applied ONLY when the field is still at its stock default, so manual
 * tweaks you make in the container UI survive relaunches. Flip [FORCE] to true to
 * hard-apply every launch once you've finished experimenting.
 *
 * Two groups below:
 *   - "baked" values are high-confidence wins.
 *   - "experimental" values are worth benchmarking; remove any that regress.
 *
 * Component VERSIONS (newer VKD3D / Proton 11 arm64ec) are intentionally NOT set here.
 * The gamefix path does not DOWNLOAD components, so pointing at an uninstalled version
 * would break launch. Select them once in the container UI (that triggers the download);
 * see the commented block at the bottom to bake them in afterwards.
 */
val STEAM_Fix_1091500: KeyedGameFix = object : KeyedGameFix {
    override val gameSource: GameSource = GameSource.STEAM
    override val gameId: String = "1091500"

    private val FORCE = false

    override fun apply(
        context: Context,
        gameId: String,
        installPath: String,
        installPathWindows: String,
        container: Container,
    ): Boolean {
        return try {
            var changed = false

            // ---- Environment variables (respect user-set values unless FORCE) ----
            val bakedEnv = mapOf(
                "WINEFSYNC" to "1",                    // needs a kernel w/ fsync/futex_waitv, else no-op
                "MESA_SHADER_CACHE_MAX_SIZE" to "3072MB", // room for CP2077's huge shader set
            )
            val experimentalEnv = mapOf(
                // vkd3d shader-compile worker threads; sized to the X1+A78 big cluster. Try 2-6.
                "VKD3D_THREAD_COUNT" to "4",
                // frames-in-flight; lower = less latency. Raise to 3-4 if you see stalls.
                "VKD3D_SWAPCHAIN_LATENCY_FRAMES" to "2",
            )
            val envVars = EnvVars(container.envVars)
            for ((name, value) in bakedEnv + experimentalEnv) {
                if (FORCE || !envVars.has(name)) {
                    envVars.put(name, value)
                    changed = true
                }
            }
            if (changed) container.envVars = envVars.toString()

            // ---- Present mode: fifo (hard vsync) -> mailbox [baked] ----
            if ((FORCE || container.rendererPresentMode == "fifo") &&
                container.rendererPresentMode != "mailbox"
            ) {
                container.rendererPresentMode = "mailbox"
                changed = true
            }

            // ---- Big-core affinity: 4-7 = X1 + A78, 0-3 = A55 efficiency [baked] ----
            // If combat feels worse after tuning, revert this first.
            if (FORCE || container.getCPUList(false) == null) {
                container.setCPUList("4,5,6,7")
                changed = true
            }

            // ---- dxwrapper: VRAM pool + relaxed shader math [baked] ----
            run {
                var cfg = container.getDXWrapperConfig()
                val before = cfg
                if (FORCE || cfg.contains("videoMemorySize=2048")) {
                    cfg = cfg.replace(Regex("videoMemorySize=\\d+"), "videoMemorySize=4096")
                }
                if (FORCE || cfg.contains("strict_shader_math=1")) {
                    cfg = cfg.replace(Regex("strict_shader_math=\\d+"), "strict_shader_math=0")
                }
                if (cfg != before) {
                    container.setDXWrapperConfig(cfg)
                    changed = true
                }
            }

            // ---- Driver present-wait off [experimental] ----
            // Can smooth pacing on Turnip where VK_KHR_present_wait causes CPU stalls.
            run {
                var g = container.getGraphicsDriverConfig()
                val before = g
                if (FORCE || g.contains("disablePresentWait=0")) {
                    g = g.replace(Regex("disablePresentWait=\\d+"), "disablePresentWait=1")
                }
                if (g != before) {
                    container.setGraphicsDriverConfig(g)
                    changed = true
                }
            }

            // ---- Component versions: enable ONLY after installing them via the UI ----
            // if (container.getWineVersion() == "proton-9.0-arm64ec") {
            //     container.setWineVersion("proton-11.0-1-arm64ec-1"); changed = true
            // }
            // container.setDXWrapperConfig(
            //     container.getDXWrapperConfig().replace(Regex("vkd3dVersion=[^,]+"), "vkd3dVersion=3.0.1"),
            // )

            if (changed) {
                container.saveData()
                Timber.tag("GameFixes").i("Applied Cyberpunk 2077 (SD888/Adreno 660) tuning for $gameId")
            }
            true
        } catch (e: Exception) {
            Timber.tag("GameFixes").e(e, "Failed to apply Cyberpunk 2077 fix for $gameId")
            false
        }
    }
}
