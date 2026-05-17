package com.eight87.shutterboy.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * cold-start-perf Phase F.2 — record the cold-boot critical path.
 *
 * Launches `com.eight87.shutterboy/.MainActivity` from a cold start, waits
 * for the Photos timeline to become "stable" (the Photos tab title from
 * `res/values/strings.xml` is on screen), and lets the macrobench plugin
 * capture which classes/methods were touched. The plugin merges the
 * resulting profile into `app/src/main/baseline-prof.txt` via the app
 * module's `baselineProfile { mergeIntoMain = true }` block.
 *
 * Stable-anchor choice: `By.text("Photos")` matches the Photos tab title
 * (Phase 0.8-locked `photos_tab_title` string), which is the first text
 * widget rendered after the timeline mounts. We wait up to 5 s; that's
 * comfortably above the AVD's measured median of ~1056 ms (`main.md`
 * L.6) and tolerates first-launch DB scan slowness on a real device.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.text("Photos")), 5_000L)
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.eight87.shutterboy"
    }
}
