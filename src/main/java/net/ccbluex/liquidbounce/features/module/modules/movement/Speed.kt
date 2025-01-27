/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spartan.SpartanYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre.SpectreBHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre.SpectreLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre.SpectreOnGround
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

object Speed : Module("Speed", ModuleCategory.MOVEMENT) {

    private val speedModes = arrayOf( // NCP
            NCPBHop(),
            NCPFHop(),
            SNCPBHop(),
            NCPHop(),
            YPort(),
            YPort2(),
            NCPYPort(),
            Boost(),
            Frame(),
            MiJump(),
            OnGround(),
            // AAC
            AACBHop(),
            AAC2BHop(),
            AAC3BHop(),
            AAC4BHop(),
            AAC5BHop(),
            AAC6BHop(),
            AAC7BHop(),
            AACHop3313(),
            AACHop350(),
            AACHop438(),
            AACLowHop(),
            AACLowHop2(),
            AACLowHop3(),
            AACGround(),
            AACGround2(),
            AACYPort(),
            AACYPort2(),
            AACPort(),
            OldAACBHop(),
            // Spartan
            SpartanYPort(),
            // Spectre
            SpectreLowHop(),
            SpectreBHop(),
            SpectreOnGround(),
            TeleportCubeCraft(),
            // Server
            HiveHop(),
            HypixelHop(),
            Mineplex(),
            MineplexGround(),
            // Other
            Matrix(),
            SlowHop(),
            CustomSpeed(),
            Legit()
    )

    val mode by object : ListValue("Mode", modes, "NCPBHop") {
        override fun onChange(oldValue: String, newValue: String): String {
            if (state)
                onDisable()

            return super.onChange(oldValue, newValue)
        }

        override fun onChanged(oldValue: String, newValue: String) {
            if (state)
                onEnable()
        }
    }
    val customSpeed by FloatValue("CustomSpeed", 1.6f, 0.2f..2f) { mode == "Custom" }
    val customY by FloatValue("CustomY", 0f, 0f..4f) { mode == "Custom" }
    val customTimer by FloatValue("CustomTimer", 1f, 0.1f..2f) { mode == "Custom" }
    val customStrafe by BoolValue("CustomStrafe", true) { mode == "Custom" }
    val resetXZ by BoolValue("CustomResetXZ", false) { mode == "Custom" }
    val resetY by BoolValue("CustomResetY", false) { mode == "Custom" }

    val portMax = FloatValue("AAC-PortLength", 1f, 1f..20f) { mode == "AACPort" }
    val aacGroundTimer by FloatValue("AACGround-Timer", 3f, 1.1f..10f) { mode in arrayOf("AACGround", "AACGround2") }
    val cubecraftPortLength by FloatValue("CubeCraft-PortLength", 1f, 0.1f..2f) { mode == "TeleportCubeCraft" }
    val mineplexGroundSpeed by FloatValue("MineplexGround-Speed", 0.5f, 0.1f..1f) { mode == "Mineplex" }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isSneaking)
            return

        if (isMoving) {
            thePlayer.isSprinting = true
        }

        modeModule?.onUpdate()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isSneaking || event.eventState != EventState.PRE)
            return

        if (isMoving)
            thePlayer.isSprinting = true

        modeModule?.onMotion()
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.thePlayer.isSneaking)
            return

        modeModule?.onMove(event)
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (mc.thePlayer.isSneaking)
            return

        modeModule?.onTick()
    }

    override fun onEnable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f

        modeModule?.onEnable()
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f

        modeModule?.onDisable()
    }

    override val tag
        get() = mode

    private val modeModule
        get() = speedModes.find { it.modeName == mode }

    private val modes
        get() = speedModes.map { it.modeName }.toTypedArray()
}
