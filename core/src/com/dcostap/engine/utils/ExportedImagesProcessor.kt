package com.dcostap.engine.utils

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.dcostap.printDebug
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.math.BigInteger
import java.util.*

/** Created by Darius on 02-Mar-19. */
class ExportedImagesProcessor {
    companion object {
        var exportAtlasFolder = "atlas"
        var imagesOrigin = "../../workingAssets/atlas"
        var hashFileName = "textureHash.txt"
        val ignoreBlankRegions = false

        /** Will use texturePacker to pack all images, only if files have changed or atlas output files are not created
         * Loads settings from pack.json files in folders
         * Gradle task texturePacker does the same packing, Android launcher won't pack so use the task if needed
         *
         * Images starting with _ are deleted automatically
         *
         * Images which include _w#_h#_[_n#] in the name will be considered a tileset
         *  . The image will be sliced up to image regions following a grid of values w (width) and h (height)
         *  . Resulting regions will be saved as new png files
         *  . Original image will be deleted when finished
         *  . By default fully transparent regions might be ignored [ignoreBlankRegions]. If you want to include transparent regions, use
         *  optional _n# parameter to declare the number of regions that should be created. Created images will include fully
         *  transparent regions but only n number of regions will be sliced
         *  . Regions are counted starting from top-left going right then 1 new row bottom until finished.
         *  If image border isn't of size w / h it will be ignored
         * */
        fun processExportedImages() {
            if (Gdx.app.type != Application.ApplicationType.Desktop) return
            printDebug("_____\nChecking for image changes...")
            var initTime = System.currentTimeMillis()

            val isModified = TexturePacker.isModified(imagesOrigin, exportAtlasFolder, "atlas", TexturePacker.Settings())

            printDebug("Finished; time elapsed: " + java.lang.Double.toString((System.currentTimeMillis() - initTime) / 1000.toDouble()) + " s\n_____\n")

            if (isModified) {
                initTime = System.currentTimeMillis()
                printDebug("_____\nPacking images...")

                // delete images starting with _
                fun invalidFile(file: File) = (file.name.endsWith(".png") && file.name.startsWith("_"))

                val tilesetRegex = "(.*)_w(\\d+)_h(\\d+)(_n(\\d+))?".toRegex()
                fun tilesetFile(file: File) = (file.name.endsWith(".png") && file.nameWithoutExtension.matches(tilesetRegex))

                for (file in File(imagesOrigin).walk()) {
                    if (file.isFile) {
                        if (invalidFile(file)) {
                            printDebug("Deleting ${file.name}")
                            file.delete()
                        } else if (tilesetFile(file)) {
                            printDebug("Detected tileSet image: ${file.name}")
                            val find = tilesetRegex.find(file.nameWithoutExtension)
                            find!!
                            val newName = find.groupValues.get(1)
                            val width = find.groupValues.get(2).toInt()
                            val height = find.groupValues.get(3).toInt()

                            val group5 = find.groupValues.get(5)
                            val optionalNumberOfFrames = if (group5 == "") null else group5.toInt()

                            // slice the texture into new textures from region of width, height
                            val image = Pixmap(FileHandle(file))
                            var x = 0
                            var y = 0
                            var i = 0
                            while (true) {
                                val newImage = Pixmap(width, height, Pixmap.Format.RGBA8888)
                                var isFullyTransparent = true
                                val b: Byte = 0
                                for (xx in 0 until width) {
                                    for (yy in 0 until height) {
                                        val pixel = image.getPixel(x + xx, y + yy)
                                        newImage.drawPixel(xx, yy, pixel)
                                        if (isFullyTransparent && BigInteger.valueOf(pixel.toLong()).toByteArray().last() != b) {
                                            isFullyTransparent = false
                                        }
                                    }
                                }

                                if (!ignoreBlankRegions || (!isFullyTransparent || optionalNumberOfFrames != null)) {
                                    val finalName = newName + "_$i.png"
                                    printDebug("  created new image #$i: $finalName")
                                    val parentPath = file.path.replace(file.name, "")
                                    PixmapIO.writePNG(FileHandle(parentPath + "/" + finalName), newImage)
                                } else {
                                    printDebug("  (!) tile #$i is fully transparent, won't create new image")
                                }
                                
                                newImage.dispose()

                                i++

                                if (optionalNumberOfFrames != null && i >= optionalNumberOfFrames) {
                                    printDebug("  (!) reached the specified maximum number " +
                                            "of frames: #$optionalNumberOfFrames. Stopping now")
                                    break
                                }

                                x += width
                                if (x + width > image.width) {
                                    x = 0
                                    y += height
                                    if (y + height > image.height) {
                                        break
                                    }
                                }
                            }

                            image.dispose()
                            file.delete()
                        }
                    }
                }

//                fun mergeImagesHere(folder: File) {
//                    val files = folder.listFiles()
//
//                    Arrays.sort(files) {o1, o2 ->
//                        o1.lastModified().compareTo(o2.lastModified())
//                    }
//
//                    val finalFiles = files.filter { it.name.endsWith(".png") }
//
//                    val newImage = Pixmap(width, height, Pixmap.Format.RGBA8888)
//
//                }
//
//                for (file in File(imagesOrigin).walk()) {
//                    if (file.isDirectory) mergeImagesHere(file)
//                }

                printDebug()

                TexturePacker.process(imagesOrigin, "atlas", "atlas")

                printDebug("Finished; time elapsed: " + java.lang.Double.toString((System.currentTimeMillis() - initTime) / 1000.toDouble()) + " s\n_____\n")
            }
        }
    }
}