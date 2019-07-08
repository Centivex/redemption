@file:JvmName("GlobalUtils")
@file:JvmMultifileClass

package com.dcostap.engine.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.*
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.viewport.Viewport
import com.dcostap.engine.utils.ui.ExtLabel
import com.dcostap.engine.utils.ui.ExtTable
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisSlider
import com.dcostap.Engine
import com.dcostap.printDebug
import ktx.actors.onChange
import ktx.collections.*
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.reflect.KClass

/** Substitute of kotlin's .let; call after a variable to execute block of code (only when the variable isn't null)
 * with local non-nullable copy of the variable: this avoids the "variable could have been modified" complaint from the compiler
 *
 * Just like with .let, inside of the block reference reference the variable with **it**
 *
 * Equivalent using .let: **variable?.let {}**; with this: **variable.ifNotNull {}** */
inline fun <T : Any?> T?.ifNotNull(f: (it: T) -> Unit): Unit? {
    return if (this != null) f(this) else null
}

/** Automatically frees object when finished */
inline fun <B> pool(clazz: Class<B>, action: (B) -> Unit) {
    val value = Pools.obtain(clazz)
    action(value)
    Pools.free(value)
}

fun <T> Class<T>.createInstance(): T {
    return this.getDeclaredConstructor().newInstance()
}

inline fun <T1: Any, T2: Any> ifNotNull(p1: T1?, p2: T2?, block: (T1, T2)->Unit): Unit? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}

inline fun <T1: Any, T2: Any, T3: Any> ifNotNull(p1: T1?, p2: T2?, p3: T3?, block: (T1, T2, T3)->Unit): Unit? {
    return if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null
}

inline fun <T : Any> T?.ifNull(f: () -> Unit): Unit? {
    return if (this == null) f() else null
}

/** If any is null */
inline fun <T1: Any, T2: Any> ifNull(p1: T1?, p2: T2?, block: ()->Unit): Unit? {
    return if (p1 == null || p2 == null) block() else null
}

/** If any is null */
inline fun <T1: Any, T2: Any, T3: Any> ifNull(p1: T1?, p2: T2?, p3: T3?, block: ()->Unit): Unit? {
    return if (p1 == null || p2 == null || p3 == null) block() else null
}

fun Rectangle.fixNegatives() {
    if (this.width < 0) {
        this.x += width
        this.width = -width
    }

    if (this.height < 0) {
        this.y += height
        this.height = -height
    }
}

fun Rectangle.middleX() = x + width / 2f

fun Rectangle.middleY() = y + height / 2f

fun <T : Actor> Cell<T>.padTopBottom(pad: Float): Cell<T> {
    this.padTop(pad)
    return this.padBottom(pad)
}

fun Preferences.putBase64String(key: String, value: String) {
    this.putString(key, Base64.encode(value.toByteArray()))
}

fun Preferences.getBase64String(key: String, defValue: String? = null): String? {
    val value = this.getString(key, defValue)
    if (value == defValue) return value
    return String(Base64.decode(value))
}

fun Actor.modifyToCatchInput() {
    this.touchable = Touchable.enabled

    this.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {

        }
    })
}

fun Actor.hasActionOfType(type: KClass<out Action>): Boolean {
    for (action in actions) if (type.isInstance(action)) return true
    return false
}

fun Actor.addAction(vararg actions: Action) {
    addAction(Actions.sequence(*actions))
}

fun Actor.setPosition(pos: Vector2) {
    this.setPosition(pos.x, pos.y)
}

fun Actor.setPosition(pos: Vector2, alignment: Int) {
    this.setPosition(pos.x, pos.y, alignment)
}

inline val Int.float: Float
    get() = this.toFloat()

inline val Double.float: Float
    get() = this.toFloat()

fun pixelsToUnits(pixels: Number): Float = pixels.toFloat() / Engine.PPM
fun unitsToPixels(units: Number): Float = units.toFloat() * Engine.PPM

inline val Number.unitsToPixels: Float
    get() = MathUtils.floor(this.toFloat() * Engine.PPM).toFloat()

inline val Number.pixelsToUnits: Float
    get() = this.toFloat() / Engine.PPM

fun Exception.getFullString(): String {
    val sw = StringWriter()
    this.printStackTrace(PrintWriter(sw))
    val exceptionAsString = sw.toString()
    return exceptionAsString
}

fun JsonValue.addChildValue(name: String = "", value: Any) {
    when (value) {
        is Boolean -> addChild(name, JsonValue(value))
        is String -> addChild(name, JsonValue(value))
        is Int -> addChild(name, JsonValue(value.toLong()))
        is Long -> addChild(name, JsonValue(value))
        is Float -> addChild(name, JsonValue(value.toDouble()))
        is Double -> addChild(name, JsonValue(value))
        is JsonValue -> addChild(name, value)
        else -> throw RuntimeException("Tried to add value: $value (type: ${value.javaClass}) to json; but value has type not supported ")
    }
}

fun JsonValue.getChildValue(name: String = "", defaultValue: Any): Any {
    return when (defaultValue) {
        is Boolean -> getBoolean(name, defaultValue)
        is String -> getString(name, defaultValue)
        is Int -> getInt(name, defaultValue)
        is Long -> getLong(name, defaultValue)
        is Float -> getFloat(name, defaultValue)
        is Double -> getDouble(name, defaultValue)
        is JsonValue -> getChild(name)
        else -> throw RuntimeException("Tried to get value with name $name. Not found")
    }
}

fun <T : Actor> Cell<T>.padLeftRight(pad: Float): Cell<T> {
    this.padLeft(pad)
    return this.padRight(pad)
}

fun Table.padTopBottom(pad: Float): Table {
    this.padTop(pad)
    return this.padBottom(pad)
}

fun Table.padLeftRight(pad: Float): Table {
    this.padLeft(pad)
    return this.padRight(pad)
}

val outlineShader = ShaderProgram(
        "uniform mat4 u_projTrans;\n" +
        "\n" +
        "attribute vec4 a_position;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "attribute vec4 a_color;\n" +
        "\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoord;\n" +
        "\n" +
        "uniform vec2 u_viewportInverse;\n" +
        "\n" +
        "void main() {\n" +
        "    gl_Position = u_projTrans * a_position;\n" +
        "    v_texCoord = a_texCoord0;\n" +
        "    v_color = a_color;\n" +
        "\n" +
        "}", "#ifdef GL_ES\n" +
        "precision mediump float;\n" +
        "precision mediump int;\n" +
        "#endif\n" +
        "\n" +
        "uniform sampler2D u_texture;\n" +
        "\n" +
        "// The inverse of the viewport dimensions along X and Y\n" +
        "uniform vec2 u_viewportInverse;\n" +
        "\n" +
        "// Color of the outline\n" +
        "uniform vec3 u_color;\n" +
        "\n" +
        "// Thickness of the outline\n" +
        "uniform float u_offset;\n" +
        "\n" +
        "// Step to check for neighbors\n" +
        "uniform float u_step;\n" +
        "\n" +
        "// U and V of sprite\n" +
        "uniform vec2 u_u;\n" +
        "uniform vec2 u_v;\n" +
        "\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoord;\n" +
        "\n" +
        "#define ALPHA_VALUE_BORDER 0.5\n" +
        "\n" +
        "void main() {\n" +
        "   vec2 T = v_texCoord.xy;\n" +
        "\n" +
        "   float alpha = 0.0;\n" +
        "   bool allin = true;\n" +
        "   for( float ix = -u_offset; ix < u_offset; ix += u_step )\n" +
        "   {\n" +
        "      for( float iy = -u_offset; iy < u_offset; iy += u_step )\n" +
        "      {\n" +
        "            vec2 samplePos = T + vec2(ix, iy) * u_viewportInverse;\n" +
        "\n" +
        "            samplePos.x = clamp(samplePos.x, u_u.x, u_u.y);\n" +
        "            samplePos.y = clamp(samplePos.y, u_v.x, u_v.y);\n" +
        "\n" +
        "            float newAlpha = texture2D(u_texture, samplePos).a;\n" +
        "            allin = allin && newAlpha > ALPHA_VALUE_BORDER;\n" +
        "            if (newAlpha > ALPHA_VALUE_BORDER && newAlpha >= alpha)\n" +
        "            {\n" +
        "               alpha = newAlpha;\n" +
        "            }\n" +
        "      }\n" +
        "   }\n" +
        "   if (allin)\n" +
        "   {\n" +
        "      alpha = 0.0;\n" +
        "   }\n" +
        "\n" +
        "   gl_FragColor = vec4(u_color,alpha);\n" +
        "}"
)

val tintShader = ShaderProgram("attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec2 v_texCoords;\n" +
        "varying vec4 v_color;\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "    v_color = a_color;\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position =  u_projTrans * a_position;\n" +
        "}", "#ifdef GL_ES\n" +
        "#define LOWP lowp\n" +
        "precision mediump float;\n" +
        "#else\n" +
        "#define LOWP\n" +
        "#endif\n" +
        "\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec4 u_emissive;\n" +
        "\n" +
        "void main() {\n" +
        "    gl_FragColor = v_color * texture2D(u_texture, v_texCoords) + u_emissive;\n" +
        "}")

val colorShader = ShaderProgram("attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec2 v_texCoords;\n" +
        "varying vec4 v_color;\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "    v_color = a_color;\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position =  u_projTrans * a_position;\n" +
        "}", "#ifdef GL_ES\n" +
        "#define LOWP lowp\n" +
        "precision mediump float;\n" +
        "#else\n" +
        "#define LOWP\n" +
        "#endif\n" +
        "\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec4 u_emissive;\n" +
        "\n" +
        "void main() {\n" +
        "    gl_FragColor = vec4(u_emissive.rgb, texture2D(u_texture, v_texCoords).a);\n" +
        "}")

val colorDummy = Color()

/** Using shaders, tints batch to the color specified. Color.BLACK means no tinting. */
inline fun Batch.tint(color: Color, f: () -> Unit) {
    flush()
    shader = tintShader
    colorDummy.set(color)
    colorDummy.a = 0f
    tintShader.setUniformf("u_emissive", colorDummy)
    f()
    flush()
    shader = null
}

/** Using shaders, replaces all non transparent pixels with the color specified. */
inline fun Batch.color(color: Color, f: () -> Unit) {
    flush()
    shader = colorShader
    colorDummy.set(color)
    colorDummy.a = 0f
    colorShader.setUniformf("u_emissive", colorDummy)
    f()
    flush()
    shader = null
}

/** Using shaders, replaces all non transparent pixels with the color specified. */
inline fun Batch.drawOutline(textureRegion: TextureRegion, outlineSize: Float, color: Color, f: () -> Unit) {
    val sprite = textureRegion
    flush()
    shader = outlineShader

    outlineShader.begin()
    outlineShader.setUniformf("u_viewportInverse", Vector2(1f / sprite.texture.width, 1f / sprite.texture.height))
    outlineShader.setUniformf("u_offset", outlineSize)
    outlineShader.setUniformf("u_step", Math.min(1f, 1f))
    outlineShader.setUniformf("u_color", Vector3(color.r, color.g, color.b))
    if(sprite.isFlipX)
        outlineShader.setUniformf("u_u", Vector2(sprite.u2, sprite.u))
    else
        outlineShader.setUniformf("u_u", Vector2(sprite.u, sprite.u2))
    outlineShader.setUniformf("u_v", Vector2(sprite.v, sprite.v2))
    outlineShader.end()

    f()
    flush()
    shader = null
}

/** Percentage of application's height, rounded to closest integer */
fun percentOfAppHeight(percent: Float) = Utils.percentage100Int(Gdx.graphics.height.toFloat(), percent).toFloat()
/** Percentage of application's width, rounded to closest integer */
fun percentOfAppWidth(percent: Float) = Utils.percentage100Int(Gdx.graphics.width.toFloat(), percent).toFloat()

/** @return In viewport units */
fun percentOfViewportWidth(percent: Number, viewport: Viewport): Float {
    return (viewport.worldWidth * percent.toFloat()) * (viewport.camera as OrthographicCamera).zoom
}
/** @return In viewport units */
fun percentOfViewportHeight(percent: Number, viewport: Viewport): Float {
    return (viewport.worldHeight * percent.toFloat()) * (viewport.camera as OrthographicCamera).zoom
}

/** Rectangle constructor with default parameters */
fun newRectangle(x: Number = 0f, y: Number = 0f, width: Number = 0f, height: Number = 0f): Rectangle {
    return Rectangle().also {it.x = x.toFloat(); it.y = y.toFloat(); it.width = width.toFloat(); it.height = height.toFloat()}
}

/** x & y are the origin in the middle */
fun newRectangleFromPoint(x: Number = 0f, y: Number = 0f, width: Number = 0f, height: Number = 0f): Rectangle {
    return Rectangle().also {it.x = x.toFloat() - width.toFloat() / 2f; it.y = y.toFloat() - height.toFloat() / 2f;
        it.width = width.toFloat(); it.height = height.toFloat()}
}

/** Translates input into Engine units (Engine.PPM) */
fun newRectanglePixels(xPixels: Number = 0f, yPixels: Number = 0f, widthPixels: Number = 0f, heightPixels: Number = 0f): Rectangle {
    return Rectangle().also {
        it.x = pixelsToUnits(xPixels.toFloat()); it.y = pixelsToUnits(yPixels.toFloat())
        it.width = pixelsToUnits(widthPixels.toFloat()); it.height = pixelsToUnits(heightPixels.toFloat())
    }
}

fun newRectangleFromRegion(region: TextureRegion, centerX: Boolean = false, centerY: Boolean = false): Rectangle {
    return newRectanglePixels(if (centerX) -region.regionWidth / 2f else 0f, if (centerY) -region.regionHeight / 2f else 0f,
            region.regionWidth, region.regionHeight)
}

fun IntArray.getRandom(): Int = get(Utils.randomInt(size))
fun FloatArray.getRandom(): Float = get(Utils.randomInt(size))
fun <T> Array<T>.getRandom(): T = get(Utils.randomInt(size))

fun Number.map(inputLow: Number, inputHigh: Number, outputLow: Number, outputHigh: Number): Float {
    return Utils.mapNumber(this, inputLow, inputHigh, outputLow, outputHigh)
}

/** Extension to add support for doubles in Preferences. From https://stackoverflow.com/a/45412036 */
fun Preferences.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

fun Preferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))

fun <Type> GdxArray<Type>.getOrDefault(index: Int, default: Type): Type {
    if (index >= this.size || index < 0) return default
    return this[index] ?: default
}

infix fun <T : Actor> Table.add(actor: T): Cell<T> {
    return this.add(actor)
}

fun Rectangle.pixelsToGameUnits(): Rectangle {
    // translate bounding box pixels to units
    x /= Engine.PPM
    y /= Engine.PPM

    // extra translation to units -> width and height
    width /= Engine.PPM
    height /= Engine.PPM

    return this
}

fun OrthographicCamera.moveToBottomLeft(x: Float, y: Float) {
    this.position.x = x + this.viewportWidth * this.zoom / 2f
    this.position.y = y + this.viewportHeight * this.zoom / 2f
}

fun OrthographicCamera.leftX(): Float {
    return position.x - viewportWidth * zoom / 2f
}

fun OrthographicCamera.rightX(): Float {
    return position.x + viewportWidth * zoom / 2f
}

fun OrthographicCamera.topY(): Float {
    return position.y + viewportHeight * zoom / 2f
}

fun OrthographicCamera.bottomY(): Float {
    return position.y - viewportHeight * zoom / 2f
}

fun OrthographicCamera.setLeftX(value: Float) {
    position.x = value + viewportWidth * zoom / 2f
}

fun OrthographicCamera.setRightX(value: Float) {
    position.x = value - viewportWidth * zoom / 2f
}

fun OrthographicCamera.setTopY(value: Float) {
    position.y = value - viewportHeight * zoom / 2f
}

fun OrthographicCamera.setBottomY(value: Float) {
    position.y = value + viewportHeight * zoom / 2f
}

/**
 * Automatically calls [Batch.begin] and [Batch.end].
 * @param action inlined. Executed after [Batch.begin] and before [Batch.end].
 */
inline fun <B : Batch> B.use(action: (B) -> Unit) {
    begin()
    action(this)
    end()
}

/**
 * Automatically calls [ShaderProgram.begin] and [ShaderProgram.end].
 * @param action inlined. Executed after [ShaderProgram.begin] and before [ShaderProgram.end].
 */
inline fun <S : ShaderProgram> S.use(action: (S) -> Unit) {
    begin()
    action(this)
    end()
}

inline fun <S : FrameBuffer> S.use(action: (S) -> Unit) {
    begin()
    action(this)
    end()
}

/** @return angle in degrees, from 0 to 360 */
fun Camera.getRotation(): Float {
    return (Math.atan2(up.x.toDouble(), up.y.toDouble()) * MathUtils.radiansToDegrees).toFloat() * -1f
}

/** @param degrees from 0 to 360 */
fun OrthographicCamera.setRotation(degrees: Float) {
    up.set(0f, 1f, 0f);
    direction.set(0f, 0f, -1f);
    rotate(-degrees)
}

object Utils {
    /** Clamps input values to input range, then maps them to the output range.
     *
     * inputLow must be lower than inputHigh, but outputLow can be bigger than outputHigh; map still will happen correctly  */
    @JvmStatic fun mapNumber(input: Number, inputLow: Number, inputHigh: Number, outputLow: Number, outputHigh: Number, interpolation: Interpolation = Interpolation.linear): Float {
        var thisOutputLow = outputLow.toFloat()
        var thisOutputHigh = outputHigh.toFloat()
        if (input.toFloat() < inputLow.toFloat()) return thisOutputLow
        if (input.toFloat() > inputHigh.toFloat()) return thisOutputHigh

        var switched = false
        if (thisOutputLow > thisOutputHigh) {
            val temp = thisOutputHigh
            thisOutputHigh = thisOutputLow
            thisOutputLow = temp
            switched = true
        }

        val endInput = interpolation.apply(inputLow.toFloat(), inputHigh.toFloat(),
                (input.toFloat() - inputLow.toFloat()) / (inputHigh.toFloat() - inputLow.toFloat())) // map input inside low / high to 0 till 1

        val scale = (thisOutputHigh - thisOutputLow) / (inputHigh.toFloat() - inputLow.toFloat())
        val value = (endInput.toFloat() - inputLow.toFloat()) * scale + thisOutputLow

        return if (switched) {
            thisOutputLow - value + thisOutputHigh
        } else
            value
    }

    val colors = GdxMap<String, Color>()

    init {
        colors.clear()
        colors.put("CLEAR", Color.CLEAR)
        colors.put("BLACK", Color.BLACK)

        colors.put("WHITE", Color.WHITE)
        colors.put("LIGHT_GRAY", Color.LIGHT_GRAY)
        colors.put("GRAY", Color.GRAY)
        colors.put("DARK_GRAY", Color.DARK_GRAY)

        colors.put("BLUE", Color.BLUE)
        colors.put("NAVY", Color.NAVY)
        colors.put("ROYAL", Color.ROYAL)
        colors.put("SLATE", Color.SLATE)
        colors.put("SKY", Color.SKY)
        colors.put("CYAN", Color.CYAN)
        colors.put("TEAL", Color.TEAL)

        colors.put("GREEN", Color.GREEN)
        colors.put("CHARTREUSE", Color.CHARTREUSE)
        colors.put("LIME", Color.LIME)
        colors.put("FOREST", Color.FOREST)
        colors.put("OLIVE", Color.OLIVE)

        colors.put("YELLOW", Color.YELLOW)
        colors.put("GOLD", Color.GOLD)
        colors.put("GOLDENROD", Color.GOLDENROD)
        colors.put("ORANGE", Color.ORANGE)

        colors.put("BROWN", Color.BROWN)
        colors.put("TAN", Color.TAN)
        colors.put("FIREBRICK", Color.FIREBRICK)

        colors.put("RED", Color.RED)
        colors.put("SCARLET", Color.SCARLET)
        colors.put("CORAL", Color.CORAL)
        colors.put("SALMON", Color.SALMON)
        colors.put("PINK", Color.PINK)
        colors.put("MAGENTA", Color.MAGENTA)

        colors.put("PURPLE", Color.PURPLE)
        colors.put("VIOLET", Color.VIOLET)
        colors.put("MAROON", Color.MAROON)
    }

    /** Like [map] but with a simple percentage as input values.
     * Point1 may be higher value than point2
     *
     * @param percentProgress from 0 to 1 */
    @JvmStatic fun lerp(point1: Number, point2: Number, percentProgress: Number, interpolation: Interpolation = Interpolation.linear): Float {
        return mapNumber(percentProgress, 0f, 1f, point1, point2, interpolation)
    }

    @JvmStatic fun clamp(value: Number, min: Number, max: Number): Float {
        val thisValue = value.toDouble()
        val thisMin = min.toDouble()
        val thisMax = max.toDouble()

        if (thisValue < thisMin) return thisMin.toFloat()
        return if (thisValue > thisMax) thisMax.toFloat() else thisValue.toFloat()
    }

    @JvmStatic fun clamp(value: Number, minAndMaxPosAndNeg: Number): Float {
        return if (minAndMaxPosAndNeg.toDouble() >= 0) clamp(value, -minAndMaxPosAndNeg.toDouble(), minAndMaxPosAndNeg)
        else clamp(value, minAndMaxPosAndNeg.toDouble(), -minAndMaxPosAndNeg.toDouble())
    }

    private val textureGroupsRegex = Regex("(.*)_(\\d+)")
    /** returns the index number if the textureName ends with _#. Returns -1 if no correct name format */
    fun textureNameIndex(textureName: String): Int {
        return textureGroupsRegex.find(textureName)?.groupValues?.get(2)?.toInt() ?: -1
    }

    fun textureNameWithoutIndex(textureName: String): String {
        return textureGroupsRegex.find(textureName)?.groupValues?.get(1) ?: textureName
    }

    //region NUMBER FORMAT
    private val magnitudes = arrayOf("K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc", "UDc", "DDc",
            "TDc", "QaD", "QiD", "SxD", "SpD", "OcD", "NoD", "Vi")

    private fun doTheFormat(number: Double, decimals: Int): String {
        return String.format(Locale.US, "%,." + Integer.toString(decimals) + "f", number)
    }

    /**
     * Adapted from https://stackoverflow.com/a/30688774. Transforms input to Double and adds magnitudes to it. Should cover
     * all range of values of Long but not all from Double
     */
    @JvmStatic fun formatNumber(number: Number, normalDecimals: Int = 0, includeMagnitudes: Boolean = true, magnitudeDecimals: Int = 2,
                                ignoreThousandsMagnitude: Boolean = false, ignoreMagnitudesUntil: Number = -1,
                     magnitudes: kotlin.Array<String> = Utils.magnitudes): String
    {
        if (!includeMagnitudes) return doTheFormat(number.toDouble(), normalDecimals)
        fun doesNotNeedMagnitude(number: Double, originalNumber: Boolean = true): Boolean {
            return number < 1000 || (number < 1_000_000 && ignoreThousandsMagnitude && originalNumber)
                    || (number < ignoreMagnitudesUntil.toDouble() && ignoreMagnitudesUntil != -1 && originalNumber)
        }

        var thisNumber = number.toDouble()

        if (thisNumber <= -9200000000000000000L) {
            return "-9.2E"
        }

        if (doesNotNeedMagnitude(thisNumber)) return doTheFormat(thisNumber, normalDecimals)

        var i = 0
        while (true) {
//            if (thisNumber < 10000 && thisNumber % 1000 >= 100)
//                return ret + doTheFormat(thisNumber / 1000, magnitudeDecimals) +
//                        ',' + doTheFormat(thisNumber % 1000 / 100 + magnitudes[i].toDouble(), magnitudeDecimals)
            thisNumber /= 1000.0
            if (doesNotNeedMagnitude(thisNumber, false))
                return doTheFormat(thisNumber, magnitudeDecimals) + magnitudes[i]
            i++
        }
    }
    //endregion

    fun solidColorDrawable(color: Color): Drawable {
        return solidColorDrawable(color.r, color.g, color.b, color.a)
    }

    fun solidColorDrawable(color: Color, a: Float): Drawable {
        return solidColorDrawable(color.r, color.g, color.b, a)
    }

    fun solidColorDrawable(r: Float, g: Float, b: Float, a: Float): Drawable {
        return object : BaseDrawable() {
            override fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float) {
                super.draw(batch, x, y, width, height)

                batch!!
                batch.setColor(r, g, b, a)
                batch.draw(Engine.pixelTexture, x, y, width, height)
                batch.color = Color.WHITE
            }
        }
    }

    /** Using VisUI, creates a slider which allows to change a value. Pass a function to easily update a variable with the slider's value
     * Uses ExtLabels with the default font */
    fun visUI_valueChangingSlider(valueName: String, minValue: Float = 0f, maxValue: Float = 1f, startingValue: Float = 0.5f,
                                  stepSize: Float = 0.01f, decimals: Int = 2, getSliderValue: (value: Float) -> Unit): ExtTable {
        var changed = false
        return ExtTable().also {
            it.pad(10f)
            it.add(ExtLabel(valueName)).center()

            it.row()

            val visSlider = VisSlider(minValue, maxValue, stepSize, false).also {
                it.value = startingValue
            }

            fun format(number: Number): String {
                return formatNumber(number, decimals, true, 3, true)
            }

            it.add(Table().also {
                it.add(ExtLabel(format(minValue)))

                it.add(visSlider)

                it.add(ExtLabel(format(maxValue)))
            })

            it.row()
            it.add(ExtLabel().also {
                it.textUpdateFunction = { visSlider.value.toString() }
            }).center()

            visSlider.onChange { getSliderValue(visSlider.value) }
        }
    }

    /** VisUI checkbox, but the text is in a [ExtLabel] */
    fun visUI_customCheckBox(text: String, checked: Boolean, textPadLeft: Float = 1f): Table {
        return Table().also {
            it.add(VisCheckBox("", checked).also {
                it.add(ExtLabel(text).also { it.setAlignment(Align.left) }).padLeft(textPadLeft)
            })
        }
    }

    // not working
//    fun hasEmptyConstructor(kotlinClass: KClass<out Any>): Boolean {
//        for (c in kotlinClass.constructors) {
//            if (c.parameters.size == 2) {
//                return true
//            }
//        }
//
//        return false
//    }

    private val dummyVector2 = Vector2()
    private val dummyVector3 = Vector3()

    @JvmStatic fun projectPosition(x: Number, y: Number, originViewport: Viewport? = null, endViewport: Viewport? = null, flipY: Boolean = true): Vector2 {
        val coords = dummyVector2
        val thisX = x.toFloat()
        val thisY = y.toFloat()
        coords.set(thisX, thisY)
        originViewport?.project(coords)
        endViewport.ifNotNull {
            if (flipY) coords.y = Gdx.graphics.height - coords.y // looks like you need to flip it in this case, probably because
                                                                    // the unproject requires "opengl-oriented y"
            it.unproject(coords)
        }

        if (originViewport?.screenWidth == 0) {
            printDebug("WARNING! - Projecting position of worldViewport with width 0. This will give unexpected results." +
                    "\nCheck you are not calling the method before resize() was called (viewport still has no size)")
        }

        return coords
    }

    @JvmStatic fun projectPosition(x: Number, y: Number, originCamera: Camera? = null, endCamera: Camera? = null, flipY: Boolean = true): Vector2 {
        val coords = dummyVector3
        val thisX = x.toFloat()
        val thisY = y.toFloat()
        coords.set(thisX, thisY, 0f)
        originCamera?.project(coords)
        endCamera.ifNotNull {
            if (flipY) coords.y = Gdx.graphics.height - coords.y // looks like you need to flip it in this case, probably because
            // the unproject requires "opengl-oriented y"
            it.unproject(coords)
        }

        if (originCamera?.viewportWidth == 0f) {
            printDebug("WARNING! - Projecting position of worldViewport with width 0. " +
                    "This will give unexpected results." +
                    "\nCheck you are not calling the method before resize() was called (viewport still has no size)")
        }

        dummyVector2.set(coords.x, coords.y)
        return dummyVector2
    }

    @JvmStatic fun clearScreen(r: Int = 58, g: Int = 68, b: Int = 102) {
        Gdx.gl.glClearColor(r / 255f, g / 255f, b / 255f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    fun scene2dActionShake(maximumMovingQuantity: Float, originalX: Float = 0f, originalY: Float = 0f): Action {
        fun random(): Float {
            return randomFloatRange(0f, maximumMovingQuantity, true)
        }
        fun shake(): Action {
            return Actions.sequence(
                    Actions.moveBy(random(), random(), 0.03f, Interpolation.pow3Out),
                    Actions.moveTo(originalX, originalY, 0.02f, Interpolation.pow3Out))
        }

        return Actions.sequence(shake(),shake(),shake(),shake())
    }

    @JvmStatic fun getDecimalPart(number: Float): Float {
        return number - number.toInt()
    }

    /** applies a percentage from 0 to 100 to the number */
    @JvmStatic fun percentage100(number: Number, percentage: Number): Float {
        return (number.toDouble() * (percentage.toDouble() / 100.0)).toFloat()
    }

    @JvmStatic fun percentage100Int(number: Number, percentage: Number): Int {
        return percentage100(number, percentage).toInt()
    }

    fun removeExtensionFromFilename(filename: String): String {
        val i = filename.lastIndexOf(".")
        return if (i >= 0)
            filename.substring(0, i)
        else
            filename
    }

    fun getFileName(filename: String): String {
        val paths = filename.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return paths[paths.size - 1]
    }

    private val rand = Random()

    /** Includes 0, doesn't include length (From 0 to length - 1)  */
    @JvmStatic fun randomInt(length: Int): Int {
        return rand.nextInt(length)
    }

    @JvmStatic fun <T> randomChoose(vararg stuff: T): T {
        return stuff.get(Utils.randomInt(stuff.size))
    }

    @JvmStatic fun randomChoose(vararg floats: Float): Float {
        return floats.get(Utils.randomInt(floats.size))
    }

    @JvmStatic fun randomChoose(vararg ints: Int): Int {
        return ints.get(Utils.randomInt(ints.size))
    }

    @JvmStatic fun randomFloat(): Float {
        return rand.nextFloat()
    }

    /** Returns 1 or -1 */
    @JvmStatic fun randomSign(): Int {
        return if (randomFloat() > 0.5) 1 else -1
    }

    @JvmStatic fun chance(percentOfTrue: Number): Boolean {
        var thisPercentageOfTrue = percentOfTrue.toFloat()
        thisPercentageOfTrue = clamp(thisPercentageOfTrue, 0f, 1f)
        return randomFloat() < thisPercentageOfTrue
    }

    @JvmStatic fun randomIntRange(minValue: Int, maxValue: Int): Int {
        val length = Math.abs(maxValue - minValue)
        val int = randomInt(length + 1)
        return minValue + int
    }

    @JvmStatic fun randomFloatRange(positiveMin: Float, positiveMax: Float, randomSign: Boolean): Float {
        if (positiveMax == positiveMin) return positiveMax
        if (positiveMax < positiveMin)
            throw IllegalArgumentException("Invalid input floats: " + positiveMax + " isn't" +
                    "bigger than " + positiveMin)

        return mapNumber(randomFloat(), 0f, 1f, positiveMin, positiveMax) * if (randomSign) if (chance(0.5f)) 1 else -1 else 1
    }

    @JvmStatic fun randomFloatRange(min: Float, max: Float): Float {
        return mapNumber(randomFloat(), 0f, 1f, min, max)
    }

    @JvmStatic fun angleMovementX(distance: Float, directionDegrees: Float): Float {
        var thisDirectionDegrees = directionDegrees
        if (thisDirectionDegrees < 0) {
            thisDirectionDegrees += 360f
        }
        return distance * Math.cos(Math.toRadians(thisDirectionDegrees.toDouble())).toFloat()
    }

    @JvmStatic fun angleMovementY(distance: Float, directionDegrees: Float): Float {
        var thisDirectionDegrees = directionDegrees
        if (thisDirectionDegrees < 0) {
            thisDirectionDegrees += 360f
        }
        return distance * Math.sin(Math.toRadians(thisDirectionDegrees.toDouble())).toFloat()
    }

    /**
     * Length (angular) of a shortest way between two angles.
     * It will be in range [-180, 180] (signed).
     */
    @JvmStatic fun getAngleDifferenceSigned(sourceAngle: Float, targetAngle: Float): Float {
        var thisSourceAngle = sourceAngle
        var thisTargetAngle = targetAngle
        thisSourceAngle = Math.toRadians(thisSourceAngle.toDouble()).toFloat()
        thisTargetAngle = Math.toRadians(thisTargetAngle.toDouble()).toFloat()
        return Math.toDegrees(Math.atan2(Math.sin((thisTargetAngle - thisSourceAngle).toDouble()), Math.cos((thisTargetAngle - thisSourceAngle).toDouble()))).toFloat()
    }

    @JvmStatic fun getAngleDifferenceNotSigned(sourceAngle: Float, targetAngle: Float): Float {
        return Math.abs(getAngleDifferenceSigned(sourceAngle, targetAngle))
    }

    @JvmStatic fun getAngleBetweenPoints(start: Vector2, end: Vector2): Float {
        return getAngleBetweenPoints(start.x, start.y, end.x, end.y)
    }

    @JvmStatic fun getAngleBetweenPoints(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        var angle = Math.toDegrees(Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
        if (angle < 0) angle += 360f
        return angle
    }

    @JvmStatic fun getDistanceBetweenPoints(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return Math.hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toFloat()
    }

    @JvmStatic fun getDistanceBetweenPoints(point1: Vector2, point2: Vector2): Float {
        return getDistanceBetweenPoints(point1.x, point1.y, point2.x, point2.y)
    }

    @JvmStatic fun getColorFrom255RGB(red: Int, green: Int, blue: Int, alpha: Float = 1f): Color {
        return Color(red / 255f, green / 255f, blue / 255f, alpha)
    }

    @JvmStatic fun getColorFrom255RGB(redGreenBlue: Int, alpha: Float = 1f): Color {
        return getColorFrom255RGB(redGreenBlue, redGreenBlue, redGreenBlue, alpha)
    }

    fun drawProgressBar(gameDrawer: GameDrawer, x: Float, y: Float, width: Float, height: Float,
                        value: Float, maxValue: Float, minValue: Float, background: Color, foreGround: Color) {
        gameDrawer.color = background
        gameDrawer.drawRectangle(x, y, width, height, true, 0f)

        gameDrawer.color = foreGround
        gameDrawer.drawRectangle(x, y, mapNumber(value, minValue, maxValue, 0f, width), height, true, 0f)

        gameDrawer.resetColor()
    }

    fun getClosestNumberInList(number: Float, list: FloatArray): Float {
        var ret = list[0]
        var diff = Math.abs(ret - number)
        for (i in 1 until list.size) {
            if (ret != list[i]) {
                val newDiff = Math.abs(list[i] - number)
                if (newDiff < diff) {
                    ret = list[i]
                    diff = newDiff
                }
            }
        }
        return ret
    }

    @JvmStatic fun growRectangle(rectangle: Rectangle, growth: Float): Rectangle {
        rectangle.x -= growth
        rectangle.y -= growth
        rectangle.width += growth * 2
        rectangle.height += growth * 2
        return rectangle
    }
}

