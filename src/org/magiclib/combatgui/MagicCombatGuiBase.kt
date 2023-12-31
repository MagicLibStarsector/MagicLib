package org.magiclib.combatgui

import com.fs.starfarer.api.Global
import org.lazywizard.lazylib.ui.FontException
import org.lazywizard.lazylib.ui.LazyFont
import org.magiclib.combatgui.buttongroups.*
import org.magiclib.combatgui.buttons.MagicCombatActionButton
import org.magiclib.combatgui.buttons.MagicCombatButtonAction
import org.magiclib.combatgui.buttons.MagicCombatButtonInfo
import org.magiclib.combatgui.buttons.MagicCombatHoverTooltip
import java.awt.Color

/**
 * The base class you need to extend/inherit from to create a GUI.
 *
 * Call the constructor of this class in your constructor (via super) and pass it a guiLayout object.
 * You can use the `defaultGuiLayout` by passing nothing if you want to get started quickly.
 *
 * Override [getTitleString] to set a display title.
 *
 * Call [MagicCombatGuiBase.addButton] and/or [addButtonGroup] in your constructor to define what the GUI does.
 *
 * Call this classes [advance] and [render] in a BaseEveryFrame(Combat)Script advance/render methods.
 *
 * It makes sense to create a new GUI object when a hotkey is pressed.
 *
 * To get started quickly, you can use the [SampleMagicCombatGuiLauncher].
 *
 * Example implementation:
 * 
 * ```java
 * public class ExampleCombatGui extends GuiBase {
 *     // GUI setup work is done in constructor
 *     public ExampleCombatGui(){
 *         super(
 *                 new MagicCombatGuiLayout(0.05f, 0.8f, 100f, 20f, 0.5f,
 *                 Color.WHITE,5f, 0.4f, 0.2f, 25f,
 *                 "graphics/fonts/insignia15LTaa.fnt", 0.4f, 0.4f)
 *         );
 *
 *         addButton(new MyButtonAction(), // MyButtonAction is a class you need to write that implements [ButtonAction]
 *           "MyButton", // title of the button, i.e. text displayed on the button
 *           "my tooltip text", // text to display when user hovers over the button
 *           false // if the button should be disabled (usually false)
 *         );
 *
 *         addButtonGroup(
 *                 new MagicCombatMyButtonGroupAction(), // A class you need to write that implements [ButtonGroupAction]
 *                 new MagicCombatCreateSimpleButtons( // you can also write your own class that implements [CreateButtonsAction]
 *                         Arrays.asList("button1", "button2"),
 *                         null,
 *                         null
 *                 ),
 *                 null,
 *                 "Example button group"
 *         );
 *     }
 *
 *     @Nullable
 *     @Override
 *     protected String getTitleString() {
 *         return "This is an example title";
 *     }
 *
 *     @Nullable
 *     @Override
 *     protected String getMessageString() {
 *         return "This is a sample message";
 *     }
 * }
 * ```
 *
 * @author Jannes
 * @since 1.3.0
 */
open class MagicCombatGuiBase @JvmOverloads constructor(private val guiLayout: MagicCombatGuiLayout = createDefaultMagicCombatGuiLayout()) {

    companion object {
        /**
         * Best guess GUI layout, feel free to pass this to [MagicCombatGuiBase] to get started quickly.
         * In the long term, you probably want to create your own [MagicCombatGuiLayout].
         *
         * @author Jannes
         * @since 1.3.0
         */
        @JvmStatic
        fun createDefaultMagicCombatGuiLayout() = MagicCombatGuiLayout(
            0.05f, 0.8f, 100f, 20f, 0.5f, Color.WHITE,
            5f, 0.4f, 0.2f, 25f, "graphics/fonts/insignia15LTaa.fnt", 0.4f, 0.4f
        )
    }

    private val gSettings = Global.getSettings()

    private val xSpacing = guiLayout.buttonWidthPx + guiLayout.paddingPx
    private val ySpacing = guiLayout.buttonHeightPx + guiLayout.paddingPx + guiLayout.textSpacingBufferPx
    private val xTooltip = guiLayout.xTooltipRel * gSettings.screenWidthPixels / gSettings.screenScaleMult
    private val yTooltip = guiLayout.yTooltipRel * gSettings.screenHeightPixels / gSettings.screenScaleMult
    private val xAnchor = guiLayout.xAnchorRel * gSettings.screenWidthPixels / gSettings.screenScaleMult
    private val yAnchor = guiLayout.yAnchorRel * gSettings.screenHeightPixels / gSettings.screenScaleMult
    private val xMessage = guiLayout.xMessageRel * gSettings.screenWidthPixels / gSettings.screenScaleMult
    private val yMessage = guiLayout.yMessageRel * gSettings.screenHeightPixels / gSettings.screenScaleMult
    val color = guiLayout.color

    protected var font: LazyFont? = null

    protected val standaloneButtons = mutableListOf<MagicCombatActionButton>()
    protected val buttonGroups = mutableListOf<MagicCombatDataButtonGroup>()

    init {
        try {
            font = LazyFont.loadFont("graphics/fonts/insignia15LTaa.fnt")
        } catch (e: FontException) {
            Global.getLogger(this.javaClass).error("Failed to load font, won't be displaying messages", e)
        }
    }

    /**
     * Override this returning a string representing your GUI title.
     * May change between frames.
     *
     * Example implementation:
     * ```java
     * @Override
     * protected String getTitleString() {
     *   return "This is an example title";
     * }
     * ```
     */
    protected open fun getTitleString(): String? {
        return ""
    }

    /**
     * Override this to display a message, feel free to return null.
     * May change between frames.
     *
     * Example implementation:
     * ```java
     * @Override
     * protected String getMessageString() {
     *   return "";
     * }
     * ```
     */
    protected open fun getMessageString(): String? {
        return ""
    }

    /**
     * This is the intended way of adding button groups.
     * Adds a new button group to the GUI. This library will take care of positioning based on grid layout.
     * all actions will be automatically executed when appropriate.
     *
     * Think of a button group as a row of buttons that can be clicked. Whenever a button gets clicked by the user,
     * it gets activated (i.e. visually highlighted). The user can click the button again to de-activate it.
     * Each button has data (e.g. a string, maybe its name?) associated to it. Whenever a button in this group
     * is clicked by the user, the action of the button group gets executed on the data of all active buttons.
     *
     * Example:
     * ```java
     * addButtonGroup(
     *                 new MagicCombatMyButtonGroupAction(), // A class you need to write that implements [ButtonGroupAction]
     *                 new MagicCombatCreateSimpleButtons( // you can also write your own class that implements [CreateButtonsAction]
     *                         Arrays.asList("button1", "button2"),
     *                         null,
     *                         null
     *                 ),
     *                 null,
     *                 "Example button group"
     *         );
     * ```
     * Note: Internally, this will create a new object that inherits from [MagicCombatDataButtonGroup] and implements the abstract functions.
     *       If you want to provide your own implementation for [MagicCombatDataButtonGroup], use `addCustomButtonGroup` instead
     *
     * @param action will be performed when one of the buttons gets clicked, can't pass null
     *               Implement a class that implements ButtonGroupAction, overriding the execute method
     *               A list of data corresponding to the data of all currently active buttons will be passed to this action
     * @param create will be performed when the button group gets added, create individual buttons in this action, can't pass null
     *               Use the pre-existing class CreateSimpleButtons if you don't want to do anything fancy
     * @param refresh will be called whenever something changes (e.g. any button gets clicked), feel free to pass null
     */
    protected fun addButtonGroup(
        action: MagicCombatButtonGroupAction,
        create: MagicCombatCreateButtonsAction,
        refresh: MagicCombatRefreshButtonsAction?,
        descriptionText: String
    ) {
        val group = object : MagicCombatDataButtonGroup(font, descriptionText, createButtonGroupLayout(buttonGroups.size)) {
            override fun createButtons() {
                create.createButtons(this)
            }

            override fun refresh() {
                refresh?.refreshButtons(this)
            }

            override fun executeAction(data: List<Any>, triggeringButtonData: Any?, deselectedButtonData: Any?) {
                action.execute(data, triggeringButtonData, deselectedButtonData)
            }

            override fun onHover() {
                action.onHover()
            }
        }
        group.createButtons()
        buttonGroups.add(group)
    }

    /**
     * It is recommended to use [addButtonGroup] instead.
     * add a custom button group where you have to take care of positioning etc.
     * You will need to create a new class that inherits from [MagicCombatDataButton] group and pass an instance to this method.
     * Actions will be automatically executed when appropriate.
     */
    protected fun addCustomButtonGroup(buttonGroup: MagicCombatDataButtonGroup) {
        buttonGroup.createButtons()
        buttonGroups.add(buttonGroup)
    }

    /**
     * Add a new button to the GUI and let this library handle positioning.
     * A button in this context is the simplest GUI element. If a user clicks it, the passed action gets executed.
     *
     * Example:
     * ```java
     * addButton(new MyButtonAction(), // MyButtonAction is a class you need to write that implements [MagicCombatButtonAction]
     *           "MyButton", // title of the button, i.e. text displayed on the button
     *           "my tooltip text", // text to display when user hovers over the button
     *           false // if the button should be disabled (usually false)
     *           );
     * ```
     *
     * @param action will be executed when the button is click, feel free to pass null
     * @param txt display text AKA name of the button
     * @param tooltipTxt will be displayed when user hovers over button, feel free to pass an empty string
     */
    protected fun addButton(action: MagicCombatButtonAction?, txt: String, tooltipTxt: String, isDisabled: Boolean = false) {
        val btnInfo = createButtonInfo(standaloneButtons.size, txt, tooltipTxt)
        val btn = MagicCombatActionButton(action, btnInfo)
        btn.isDisabled = isDisabled
        standaloneButtons.add(btn)
    }

    /**
     * It is recommended to use [addButton] instead of this.
     * Adds a custom button where you have to take care of positioning etc.
     */
    protected fun addCustomButton(button: MagicCombatActionButton) {
        standaloneButtons.add(button)
    }

    /**
     * Returns layout that would be assigned to button group when using [addButtonGroup].
     *
     * @note Only relevant if you plan on using [addCustomButtonGroup]
     */
    protected fun createButtonGroupLayout(index: Int): MagicCombatButtonGroupLayout {
        return MagicCombatButtonGroupLayout(
            xAnchor, yAnchor - index * ySpacing, guiLayout.buttonWidthPx, guiLayout.buttonHeightPx,
            guiLayout.a, guiLayout.color, guiLayout.paddingPx, xTooltip, yTooltip
        )
    }

    /**
     * @returns button info that would be assigned to button when using [addButton]
     *
     * @note Only relevant if you plan on using [addCustomButton]
     */
    protected fun createButtonInfo(xIndex: Int, txt: String, tooltipTxt: String): MagicCombatButtonInfo {
        return MagicCombatButtonInfo(
            xAnchor + xIndex * xSpacing, yAnchor + ySpacing,
            guiLayout.buttonWidthPx, guiLayout.buttonHeightPx, guiLayout.a, txt, font, color, MagicCombatHoverTooltip(
                xTooltip, yTooltip, tooltipTxt
            )
        )
    }

    /**
     * Calls the refresh method of every button group.
     * Gets automatically called in [advance], feel free to call once at the end of your constructor call.
     */
    protected open fun refreshButtons() {
        buttonGroups.forEach {
            it.refresh()
        }
    }

    /**
     * Call this every frame in your e.g. [com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin].
     * Executes button logic, such as checking which button was clicked and executing actions when appropriate.
     */
    open fun advance() {
        var wasAction = false
        buttonGroups.forEach { wasAction = it.advance() || wasAction }
        standaloneButtons.forEach { wasAction = it.advance() || wasAction }
        if (wasAction) {
            refreshButtons()
        }
    }

    /**
     * Delete all buttons from button groups and re-create them with the given [MagicCombatCreateButtonsAction].
     */
    open fun reRenderButtonGroups() {
        buttonGroups.forEach {
            it.buttons.clear()
            it.resetGrid()
            it.createButtons()
            it.refresh()
        }
    }

    /**
     * Call this every frame in your e.g. [com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin].
     * Renders buttons, texts and tooltips.
     */
    open fun render() {
        buttonGroups.forEach { it.render() }
        standaloneButtons.forEach { it.render() }
        getTitleString()?.let { font?.createText(it, color) }?.draw(xAnchor, yAnchor + (2 * ySpacing))
        getMessageString()?.let { font?.createText(it, color) }?.draw(xMessage, yMessage)
    }
}