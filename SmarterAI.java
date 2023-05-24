import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/*
 * SmarterAI class:
 * Defines an AI that will search randomly until it finds ships.
 * Then attempts to attack cells around the discovered location
 * to ruthlessly go after ships.
 */
public class SmarterAI extends BattleshipAI {
    private List<Position> shipHits;

    private final boolean debugAI = false;

    private boolean preferMovesFormingLine;

    private boolean maximiseAdjacentRandomisation;

    public SmarterAI(SelectionGrid playerGrid, boolean preferMovesFormingLine, boolean maximiseAdjacentRandomisation) {
        super(playerGrid);
        shipHits = new ArrayList<>();
        this.preferMovesFormingLine = preferMovesFormingLine;
        this.maximiseAdjacentRandomisation = maximiseAdjacentRandomisation;
        Collections.shuffle(validMoves);
    }

    /**
     * Resets the ships that have been hit and randomises the move order.
     */
    @Override
    public void reset() {
        super.reset();
        shipHits.clear();
        Collections.shuffle(validMoves);
    }

    /**
     * Selects an appropriate move depending on whether any ships were currently hit and not yet destroyed.
     * The AI will choose an attack adjacent to known ship hit locations if a ship has been foumd, otherwise
     * it will select the next random move.
     */
    @Override
    public Position selectMove() {
        if(debugAI) System.out.println("\nBEGIN TURN===========");
        Position selectedMove;
        // If a ship has been hit, but not destroyed
        if(shipHits.size() > 0) {
            if(preferMovesFormingLine) {
                selectedMove = getSmarterAttack();
            } else {
                selectedMove = getSmartAttack();
            }
        } else {
            if(maximiseAdjacentRandomisation) {
                selectedMove = findMostOpenPosition();
            } else {
                // Use a random move
                selectedMove = validMoves.get(0);
            }
        }
        updateShipHits(selectedMove);
        validMoves.remove(selectedMove);
        if(debugAI) {
            System.out.println("Selected Move: " + selectedMove);
            System.out.println("END TURN===========");
        }
        return selectedMove;
    }

    private Position getSmartAttack() {
        List<Position> suggestedMoves = getAdjacentSmartMoves();
        Collections.shuffle(suggestedMoves);
        return  suggestedMoves.get(0);
    }

    /**
     * Gets a list of moves adjacent to shipHits and chooses one based on
     * whether it forms a line of at least two elements with adjacent ship hits.
     * If no optimal guess is found a random adjacent move is selected.
     */
    private Position getSmarterAttack() {
        List<Position> suggestedMoves = getAdjacentSmartMoves();
        for(Position possibleOptimalMove : suggestedMoves) {
            if(atLeastTwoHitsInDirection(possibleOptimalMove,Position.LEFT)) return possibleOptimalMove;
            if(atLeastTwoHitsInDirection(possibleOptimalMove,Position.RIGHT)) return possibleOptimalMove;
            if(atLeastTwoHitsInDirection(possibleOptimalMove,Position.DOWN)) return possibleOptimalMove;
            if(atLeastTwoHitsInDirection(possibleOptimalMove,Position.UP)) return possibleOptimalMove;
        }
        // No optimal choice found, just randomise the move.
        Collections.shuffle(suggestedMoves);
        return  suggestedMoves.get(0);
    }

    private Position findMostOpenPosition() {
        Position position = validMoves.get(0);;
        int highestNotAttacked = -1;
        for(int i = 0; i < validMoves.size(); i++) {
            int testCount = getAdjacentNotAttackedCount(validMoves.get(i));
            if(testCount == 4) { // Maximum found, just return immediately
                return validMoves.get(i);
            } else if(testCount > highestNotAttacked) {
                highestNotAttacked = testCount;
                position = validMoves.get(i);
            }
        }
        return position;
    }
    private int getAdjacentNotAttackedCount(Position position) {
        List<Position> adjacentCells = getAdjacentCells(position);
        int notAttackedCount = 0;
        for(Position adjacentCell : adjacentCells) {
            if(!playerGrid.getMarkerAtPosition(adjacentCell).isMarked()) {
                notAttackedCount++;
            }
        }
        return notAttackedCount;
    }

    /*
     Tests if there are two adjacent ship hits in a direction from a test start point.
     */
    private boolean atLeastTwoHitsInDirection(Position start, Position direction) {
        Position testPosition = new Position(start);
        testPosition.add(direction);
        if(!shipHits.contains(testPosition)) return false;
        testPosition.add(direction);
        if(!shipHits.contains(testPosition)) return false;
        if(debugAI) System.out.println("Smarter match found AT: " + start + " TO: " + testPosition);
        return true;
    }

    private List<Position> getAdjacentSmartMoves() {
        List<Position> result = new ArrayList<>();
        for(Position shipHitPos : shipHits) {
            List<Position> adjacentPositions = getAdjacentCells(shipHitPos);
            for(Position adjacentPosition : adjacentPositions) {
                if(!result.contains(adjacentPosition) && validMoves.contains(adjacentPosition)) {
                    result.add(adjacentPosition);
                }
            }
        }
        if(debugAI) {
            printPositionList("Ship Hits: ", shipHits);
            printPositionList("Adjacent Smart Moves: ", result);
        }
        return result;
    }
    
    private void printPositionList(String messagePrefix, List<Position> data) {
        String result = "[";
        for(int i = 0; i < data.size(); i++) {
            result += data.get(i);
            if(i != data.size()-1) {
                result += ", ";
            }
        }
        result += "]";
        System.out.println(messagePrefix + " " + result);
    }

    private List<Position> getAdjacentCells(Position position) {
        List<Position> result = new ArrayList<>();
        if(position.x != 0) {
            Position left = new Position(position);
            left.add(Position.LEFT);
            result.add(left);
        }
        if(position.x != SelectionGrid.GRID_WIDTH-1) {
            Position right = new Position(position);
            right.add(Position.RIGHT);
            result.add(right);
        }
        if(position.y != 0) {
            Position up = new Position(position);
            up.add(Position.UP);
            result.add(up);
        }
        if(position.y != SelectionGrid.GRID_HEIGHT-1) {
            Position down = new Position(position);
            down.add(Position.DOWN);
            result.add(down);
        }
        return result;
    }

    /**
     * Tests if the position hits a ship. Then evaluates if the ship that is hit
     * would be destroyed. If it would be destroyed the data is all cleared for that
     * ship because it is no longer necessary to know about destroyed ships.
     */
    private void updateShipHits(Position testPosition) {
        Marker marker = playerGrid.getMarkerAtPosition(testPosition);
        if(marker.isShip()) {
            shipHits.add(testPosition);
            // Check to find if this was the last place to hit on the targeted ship
            List<Position> allPositionsOfLastShip = marker.getAssociatedShip().getOccupiedCoordinates();
            if(debugAI) printPositionList("Last Ship", allPositionsOfLastShip);
            boolean hitAllOfShip = containsAllPositions(allPositionsOfLastShip, shipHits);
            // If it was remove the ship data from history to now ignore it
            if(hitAllOfShip) {
                for(Position shipPosition : allPositionsOfLastShip) {
                    for(int i = 0; i < shipHits.size(); i++) {
                        if(shipHits.get(i).equals(shipPosition)) {
                            shipHits.remove(i);
                            if(debugAI) System.out.println("Removed " + shipPosition);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Tests if all the positions in positionsToSearch are in listToSearchIn.
     */
    private boolean containsAllPositions(List<Position> positionsToSearch, List<Position> listToSearchIn) {
        for(Position searchPosition : positionsToSearch) {
            boolean found = false;
            for(Position searchInPosition : listToSearchIn) {
                if(searchInPosition.equals(searchPosition)) {
                    found = true;
                    break;
                }
            }
            if(!found) return false;
        }
        return true;
    }
}
