package org.magiclib.kotlin

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.IntervalUtil
import org.magiclib.kotlin.TestMagicFleetBuilder.testMagicFleetBuilder

object MagicKotlinModPlugin {

    fun onGameLoad(newGame: Boolean) {
//        if (Global.getSettings().isDevMode
//            && Global.getSector().playerPerson.nameString.equals("ML_Test", ignoreCase = true)
//        ) {
//            testMagicFleetBuilder()
//        }
    }
}