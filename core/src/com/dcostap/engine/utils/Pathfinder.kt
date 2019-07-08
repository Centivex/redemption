package com.dcostap.engine.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.dcostap.Engine
import com.dcostap.engine.map.EntityTiledMap
import com.dcostap.engine.map.MapCell
import com.dcostap.printDebug

/**
 * Created by Darius on 21/08/2017.
 */

abstract class MapCellValidator {
    abstract fun isMapCellValid(mapCell: MapCell): Boolean

    open fun isMapCellValidRawCoords(x:Int, y: Int, map: EntityTiledMap): Boolean {
        return map.isInsideMap(x.toFloat(), y.toFloat()) && isMapCellValid(map.getMapCell(x, y))
    }
}

object Pathfinder {
    fun defaultValidator(): MapCellValidator = object : MapCellValidator() {
        override fun isMapCellValid(mapCell: MapCell): Boolean {
            return !mapCell.isSolid
        }
    }

    /** Max number of cells checked before giving up searching. -1 to never give up, so maximum cells checked = number of cells in map.
     * This can avoid looping through the entire map when the destination is unreachable,
     * but it might cause the algorithm to not find a complex but successful path */
    var maximumTries = 1500

    var allowDiagonals = false

    fun findPath(start: MapCell, goalCell: MapCell, validator: MapCellValidator = defaultValidator(), ignoreInvalidCells: Boolean = false): Array<MapCell>? {
        var ignoreInvalidCells = ignoreInvalidCells
        if (!validator.isMapCellValid(start)) ignoreInvalidCells = true //throw RuntimeException("Pathfinding fatal error: start position is invalid")
        if (!validator.isMapCellValid(goalCell)) ignoreInvalidCells = true

        // todo: right now the flood fill optimization is not compatible with diagonals
        if (!allowDiagonals && start.map.doFloodFillForSolidCellsPathfinding
                && start.floodRegion == goalCell.floodRegion
                && start.pathfindingFloodIndex != goalCell.pathfindingFloodIndex
                && start.pathfindingFloodIndex != -1L && goalCell.pathfindingFloodIndex != -1L) {
            ignoreInvalidCells = true
        }

        val affectedMapCells = Array<MapCell>() // to reset them after

        val openList = Array<MapCell>() // the list containing to-be inspected MapCells
        val closedList = Array<MapCell>() // the list containing inspected MapCells

        openList.add(start)
        affectedMapCells.add(start)

        // we initialize start here, other MapMapCells are initialized in MapCell code,
        // when first interacted with in getCellNeighbors
        start.node.g = 0f
        start.node.f = start.node.g + getHeuristic(start, goalCell)
        start.node.cameFrom = null

        fun constructPath(): Array<MapCell> {
            var mapCell: MapCell? = goalCell

            // going through the parents (cameFrom) from the end mapCell, you get the final path
            var path = Array<MapCell>()

            while (mapCell != null) {
                path.add(mapCell)
                mapCell = mapCell.node.cameFrom

                // skip first node
                if (mapCell != null && mapCell.node.cameFrom == null) break
            }

            path.reverse()

            if (ignoreInvalidCells) {
                val actualPath = Array<MapCell>()
                val oldPath = path
                path = actualPath

                var startValid = validator.isMapCellValid(start)

                for (cell in oldPath) {
                    // skip the rest of the path when you hit the first invalid cell
                    // this might happen when ignoring invalid cells, and allows you to construct the closest path even if you can't reach the goal
                    if (!validator.isMapCellValid(cell)) {
                        if (startValid) break
                    } else startValid = true
                    path.add(cell)
                }
            }

            clearMapCells(affectedMapCells)

            return path
        }

        // get the MapCell with the lowest f value
        var count = 0
        while (openList.size != 0 && count < maximumTries) {
            count++

            var lowestF = java.lang.Float.POSITIVE_INFINITY
            var current: MapCell? = null
            for (n in openList) {
                if (n.node.f < lowestF) {
                    lowestF = n.node.f
                    current = n
                }
            }

            if (current == null) throw RuntimeException("Pathfinding fatal error: current is null")

            // if goalCell is found
            if (current === goalCell) {
                printDebug("SEARCH OF PATH FINISHED, count was $count")

                return constructPath()
            }

            openList.removeValue(current, true)
            closedList.add(current)

            // get all neighbor MapCells of current MapCell
            // this is done with the method of the class MapCell
            // and it's there where invalid MapCells get ignored (solids / walls through diagonals...)
            val neigh = getCellNeighbors(current, goalCell, validator, ignoreInvalidCells)

            // if getCellNeighbors found the goal, return the path without checking for other neighbors
            if (neigh.size == 1 && neigh.get(0) === goalCell) {
                if (count > 50)
                    printDebug("SEARCH OF PATH FINISHED, count was $count")

                for (n in neigh) {
                    affectedMapCells.add(n)
                }

                neigh.get(0).node.cameFrom = current
                return constructPath()
            }

            for (neighbor in neigh) {
                affectedMapCells.add(neighbor)

                // if it's inside a closed list, ignore the MapCell
                if (!closedList.contains(neighbor, true)) {

                    // if it wasn't in an open list already, add it now
                    if (!openList.contains(neighbor, true)) {
                        openList.add(neighbor)
                    }

                    // get the g value of the neighbor MapCell from the current
                    // you either do this with a MapCell that is in the open list (so that it already has a value)
                    // in that case you compare the values and if this path is better, update that MapCell with the
                    // new value and new parent (cameFrom)
                    // if you do this with a MapCell that wasn't in the open list, you do the same, but since the MapCell
                    // has the default g value (infinite), the new g is always updated as better
                    var g2: Float
                    if (neighbor.x != current.x && neighbor.y != current.y) {
                        g2 = current.node.g + 14.14f // diagonal cost
                    } else {
                        g2 = current.node.g + 10 // not diagonal
                    }

                    if (ignoreInvalidCells && !validator.isMapCellValid(current)) g2 *= 1.7f

                    if (g2 < neighbor.node.g) { // path is better
                        neighbor.node.cameFrom = current
                        neighbor.node.g = g2
                        neighbor.node.f = neighbor.node.g + getHeuristic(neighbor, goalCell)
                    }
                }
            }
        }

        clearMapCells(affectedMapCells)
        printDebug("SEARCH OF PATH FAILED!! count is $count")
        if (ignoreInvalidCells) return Array()

        // pathfinding failed, try again ignoring invalid cells to get the closest path before hitting invalid cells
        return findPath(start, goalCell, validator, true)
    }

    private fun getCellNeighbors(cell: MapCell, goalCell: MapCell?, validator: MapCellValidator, ignoreInvalidCells: Boolean): Array<MapCell> {
        val neighbors = Array<MapCell>()

        for (x in -1..1) {
            for (y in -1..1) {
                if (x == 0 && y == 0)
                    continue

                val xx: Int = cell.x + x
                val yy: Int = cell.y + y

                if (cell.map.isInsideMap(xx.toFloat(), yy.toFloat())) {
                    val current = cell.map.getMapCell(xx, yy)

//                    // is current the goal node? return a list with only that node
//                    if (current === goalCell) {
//                        val l = Array<MapCell>()
//                        l.add(current)
//                        return l
//                    }

                    if (validator.isMapCellValid(current) || ignoreInvalidCells) {
                        var valid = true

                        // find diagonal neighbors and discard invalid ones
                        if (!allowDiagonals && xx != cell.x && yy != cell.y) {
                            // check if the diagonal block is not surrounded by 1 or 2 solids
                            if (xx < cell.x && yy > cell.y) {
                                if (!validator.isMapCellValidRawCoords(xx + 1, yy, cell.map) || !validator.isMapCellValidRawCoords(xx, yy - 1, cell.map))
                                    valid = false
                            } else if (xx > cell.x && yy > cell.y) {
                                if (!validator.isMapCellValidRawCoords(xx - 1, yy, cell.map) || !validator.isMapCellValidRawCoords(xx, yy - 1, cell.map))
                                    valid = false
                            } else if (xx < cell.x && yy < cell.y) {
                                if (!validator.isMapCellValidRawCoords(xx + 1, yy, cell.map) || !validator.isMapCellValidRawCoords(xx, yy + 1, cell.map))
                                    valid = false
                            } else if (xx > cell.x && yy < cell.y) {
                                if (!validator.isMapCellValidRawCoords(xx - 1, yy, cell.map) || !validator.isMapCellValidRawCoords(xx, yy + 1, cell.map))
                                    valid = false
                            }
                        }

                        if (valid) {
                            neighbors.add(current)

                            // put the values really high if they were not initialized yet
                            if (current.node.f == -1f) {
                                current.node.f = java.lang.Float.POSITIVE_INFINITY
                                current.node.g = java.lang.Float.POSITIVE_INFINITY
                            }
                        }
                    }
                }
            }
        }

        return neighbors
    }

    private fun getHeuristic(start: MapCell, finish: MapCell): Float {
        // more expensive than manhattan
        // but this takes into account diagonal movement
        val xDistance = Math.abs(start.x - finish.x).toFloat()
        val yDistance = Math.abs(start.y - finish.y).toFloat()
        return if (xDistance > yDistance) {
            14 * yDistance + 10 * (xDistance - yDistance)
        } else {
            14 * xDistance + 10 * (yDistance - xDistance)
        }
    }

    private fun clearMapCells(MapCells: Array<MapCell>) {
        for (mapCell in MapCells) {
            // flash affected cells
            if (Engine.DEBUG_PATHFINDING)
                mapCell.debugFlashingRect.flashColor(Color.GREEN, 1f)

            mapCell.node.reset()
        }
    }

    // returns 1 if not surrounded by solids / firstCollisionEntity is there
    // returns 0 if surrounded by solids but goal is walkable
    // returns -1 if surrounded and goal isn't walkable
//    private fun checkIfSurroundedBySolids(firstCollisionEntity: Entity?, startCell: MapCell, finishCell: MapCell): Int {
//        for (x in -1..1) {
//            for (y in -1..1) {
//                if (x == 0 && y == 0)
//                    continue
//
//                if (firstCollisionEntity!!.map.isInsideMap((startCell.x + x).toFloat(), (startCell.y + y).toFloat())) {
//                    val checkingCell = firstCollisionEntity.map.mapCells[startCell.x + x][startCell.y + y]
//                    if (checkingCell.isWalkable || checkingCell.tiledEntity === firstCollisionEntity) {
//                        return 1
//                    }
//                }
//            }
//        }
//
//        return if (finishCell.isWalkable) {
//            0
//        } else {
//            -1
//        }
//
//    }
}
