package com.dcostap.engine.utils.font_loaders.smart_font_generator;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.dcostap.DareEngineKt;

/** Heavily adapted from jrenner's SmartFontGenerator */
public class SmartFontGenerator {
	private static final String TAG = "SmartFontGenerator";
	public static String generatedFontDir = "fonts/generated";
	public static String jsonFontsInfoName = "generatedFonts.json";
	public static int pageSize = 1024; // size of atlas pages for font pngs
    private static JsonReader jsonReader = new JsonReader();

    /** Version is saved on each generated font's info. Change it to force a reload of fonts on startup
     * The code will erase all generated fonts from the folder, performing cleanup */
    public static String fontVersion = "1.0";
    public static boolean alwaysRegenerateFonts = false;

    /** If true fonts generated will be place relative to home folder. Use only when testing: this avoids from populating
     * the assets folder with the generated fonts (which would be packaged in the .jar or .apk)
     * <p>
     * For final desktop releases, set it to false (because it might be better that users don't have a
     * folder created on their home folder; if false fonts will be saved relative to the app's directory
     * <p>
     * Careful while testing though: this means all projects will share the same folder; if fonts happen to be the same
     * problems might occur (it shouldn't be any problems, but still...) Setting {@link #alwaysRegenerateFonts} to true
     * avoids any problem */
    public static boolean desktopDebugGenerateFontsOnHomeFolder = true;

    public static BitmapFont loadFontOrGenerateIt(String fontIdentifier,
                                                  FreeTypeFontGenerator generator, FreeTypeFontParameter parameter)
    {
        if (!alwaysRegenerateFonts) {
            boolean generateIt = false;
            try {
                JsonValue wholeFile = jsonReader.parse(getFileHandle(generatedFontDir + "/" + jsonFontsInfoName));

                if (!wholeFile.getString("version").equals(fontVersion)) {
                    DareEngineKt.printDebug("-!- Different version from global version; erasing all generated fonts");
                    getFileHandle(generatedFontDir).deleteDirectory();
                    generateIt = true;
                }
                if (wholeFile.getInt("resolutionWidth") != (Gdx.graphics.getWidth())
                        || wholeFile.getInt("resolutionHeight") != (Gdx.graphics.getHeight())) {
                    DareEngineKt.printDebug("Different resolution from last time; erasing all generated fonts");
                    getFileHandle(generatedFontDir).deleteDirectory();
                    generateIt = true;
                }

                FreeTypeFontParameter fileParameter = loadParameterFromJson(wholeFile.get(fontIdentifier));

                if (!areParametersEqual(fileParameter, parameter)) {
                    DareEngineKt.printDebug("--> .json file parameters are different; generating font");
                    generateIt = true;
                }
            } catch (Exception exception) {
                DareEngineKt.printDebug("--> couldn't load fonts .json file; generating font");
                generateIt = true;
            }

            // try to load the previously generated font
            if (!generateIt) {
                try {
                    BitmapFont font = new BitmapFont(getFileHandle(generatedFontDir + "/" + fontIdentifier + ".fnt"));
                    DareEngineKt.printDebug("--> font was already generated; font loaded");
                    return font;
                } catch (Exception exception) {
                    DareEngineKt.printDebug("--> Error while loading already generated font: " + fontIdentifier);
                    DareEngineKt.printDebug(exception.getMessage());
                }
            }
        } else {
            DareEngineKt.printDebug("WARNING: VARIABLE ON CLASS SmartFontGenerator.java IS SET TO ALWAYS GENERATE FONTS; " +
                    "skipping directly to generation...");
        }

        // generate the font

        DareEngineKt.printDebug("--> generating the font...");
        // add to the json file with info from generated fonts
        // parameter info
        JsonValue jsonValue = new JsonValue(JsonValue.ValueType.object);
        jsonValue.name = fontIdentifier;
        jsonValue.addChild("size", new JsonValue(parameter.size));
        jsonValue.addChild("borderWidth", new JsonValue(parameter.borderWidth));
        jsonValue.addChild("borderColor", new JsonValue(parameter.borderColor.toString()));
        jsonValue.addChild("color", new JsonValue(parameter.color.toString()));
        jsonValue.addChild("spaceY", new JsonValue(parameter.spaceY));

        JsonValue wholeFile;
        String file = generatedFontDir + "/" + jsonFontsInfoName;
        try {
            // already exists a .json file?
            wholeFile = jsonReader.parse(getFileHandle(file));
        } catch (Exception exception) {
            // create new .json
            wholeFile = new JsonValue(JsonValue.ValueType.object);
            wholeFile.name = "generatedFontsProperties";
        }

        // if properties info from the font already exist, delete it
        // todo: remove generated stuff from previous font; currently it is left stored (changing the fontVersion cleans it though)
        if (wholeFile.get(fontIdentifier) != null) {
            wholeFile.remove(fontIdentifier);
        }
        // add new properties of the font to the .json
        wholeFile.addChild(jsonValue);

        if (wholeFile.get("version") == null) {
            wholeFile.addChild("version", new JsonValue(fontVersion));
        } else {
            wholeFile.get("version").set(fontVersion);
        }
        if (wholeFile.get("resolutionWidth") == null) {
            wholeFile.addChild("resolutionWidth", new JsonValue(String.valueOf(Gdx.graphics.getWidth())));
        } else {
            wholeFile.get("resolutionWidth").set(String.valueOf(Gdx.graphics.getWidth()));
        }
        if (wholeFile.get("resolutionHeight") == null) {
            wholeFile.addChild("resolutionHeight", new JsonValue(String.valueOf(Gdx.graphics.getHeight())));
        } else {
            wholeFile.get("resolutionHeight").set(String.valueOf(Gdx.graphics.getHeight()));
        }

        // write .json
        FileHandle fileHandle = getFileHandle(file);
        fileHandle.writeString(wholeFile.toJson(JsonWriter.OutputType.minimal), false);

        DareEngineKt.printDebug("    -> saved new font info on .json file: " + jsonFontsInfoName);
        DareEngineKt.printDebug("    -> font generated");

        // generate it
        return generateFontWriteFiles(fontIdentifier, generator, parameter);
    }

    private static FreeTypeFontParameter loadParameterFromJson(JsonValue parameterJson) {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        parameter.size = parameterJson.getInt("size");
        parameter.borderWidth = parameterJson.getFloat("borderWidth");
        parameter.borderColor = Color.valueOf(parameterJson.getString("borderColor"));
        parameter.color = Color.valueOf(parameterJson.getString("color"));
        parameter.spaceY = parameterJson.getInt("spaceY");

        return parameter;
    }

    private static boolean areParametersEqual(FreeTypeFontParameter parameter1, FreeTypeFontParameter parameter2) {
        return (parameter1.color.equals(parameter2.color) && parameter1.borderColor.equals(parameter2.borderColor)
                && parameter1.borderWidth == parameter2.borderWidth && parameter1.size == parameter2.size
                && parameter1.spaceY == parameter2.spaceY);
    }

    /**
     * Convenience method for generating a font, and then writing the fnt and png files.
     * Writing a generated font to files allows the possibility of only generating the fonts when they are missing, otherwise
     * loading from a previously generated file.
     */
    private static BitmapFont generateFontWriteFiles(String fontIdentifier, FreeTypeFontGenerator generator, FreeTypeFontParameter parameter) {
        PixmapPacker packer = new PixmapPacker(pageSize , pageSize, Pixmap.Format.RGBA8888, 2, false, new PixmapPacker.SkylineStrategy());
        parameter.packer = packer;
        FreeTypeFontGenerator.FreeTypeBitmapFontData fontData = generator.generateData(parameter);
        Array<PixmapPacker.Page> pages = packer.getPages();
        Array<TextureRegion> texRegions = new Array<>();
        for (int i = 0; i < pages.size; i++) {
            PixmapPacker.Page p = pages.get(i);
            Texture tex = new Texture(
                    new PixmapTextureData(p.getPixmap(), p.getPixmap().getFormat(), false, false, true)) {
                @Override
                public void dispose() {
                    super.dispose();
                    getTextureData().consumePixmap().dispose();
                }
            };
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            texRegions.add(new TextureRegion(tex));
        }
        BitmapFont font = new BitmapFont(fontData, texRegions, false);
        saveFontToFile(font, parameter.size, fontIdentifier, packer);
        packer.dispose();
        return font;
    }

    private static void saveFontToFile(BitmapFont font, int fontSize, String fontIdentifier, PixmapPacker packer) {
        FileHandle fontFile = getFileHandle(generatedFontDir + "/" + fontIdentifier + ".fnt"); // .fnt path
        FileHandle pixmapDir = getFileHandle(generatedFontDir + "/" + fontIdentifier); // png dir path
        BitmapFontWriter.setOutputFormat(BitmapFontWriter.OutputFormat.Text);

        String[] pageRefs = BitmapFontWriter.writePixmaps(packer.getPages(), pixmapDir, fontIdentifier);
        Gdx.app.debug(TAG, String.format("Saving font [%s]: fontfile: %s, pixmapDir: %s\n", fontIdentifier, fontFile, pixmapDir));
        // here we must add the png folder to the page refs
        for (int i = 0; i < pageRefs.length; i++) {
            pageRefs[i] = fontIdentifier + "/" + pageRefs[i];
        }
        BitmapFontWriter.writeFont(font.getData(), pageRefs, fontFile, new BitmapFontWriter.FontInfo(fontIdentifier, fontSize), 1, 1);
    }

    public static FileHandle getFileHandle(String path) {
        if (Gdx.app.getType() == Application.ApplicationType.Desktop && desktopDebugGenerateFontsOnHomeFolder) {
            return Gdx.files.external("libgdx_generatedFonts" + "/" + path); // relative to the home folder; avoid from generating the fonts into
                                                                // the assets folder, which then would get exported to .apk, for example
        } else {
            return Gdx.files.local(path);
        }
    }
}