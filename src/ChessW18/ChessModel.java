package ChessW18;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Stack;

/**********************************************************************
 * * Holds all of the methods that are required to determine if pieces
 * are able to move, if the game is over, what moves a piece is allowed
 * to execute, and a method that allows for an undo function.
 *
 * @author Allison and George
 * @version 3/28/2018
 *********************************************************************/
public class ChessModel implements IChessModel {

    /** Holds the game model board. */
    private IChessPiece[][] board = new IChessPiece[8][8];

    /** Holds the player of the current turn. */
    private Player currentPlayer;

    /** Holds the captures of players. */
    private ArrayList<IChessPiece> whiteCaptures, blackCaptures;

    /** The stack that holds moves that have been executed.*/
    private Stack<Move> moveStack;

    /** A stack that holds moves that resulted in a capture.*/
    private Stack<Move> captureMoveStack;

    /** A message that shold be displayed to the screen.*/
    private String message;

    /*******************************************************************
    /** holds all the methods for reading and writing game files */
    private GameFileHandler handler;

    /** holds the sequence of moves in standard chess formation */
    private String gameData;

    /******************************************************************
     * Constructor for ChessModel that initializes arrays
     * and places starting pieces
     ******************************************************************/
    public ChessModel() {
        moveStack = new Stack<>();
        whiteCaptures = new ArrayList<>();
        blackCaptures = new ArrayList<>();
        placeStartingPieces();

        handler = new GameFileHandler(this);
        gameData = "";
    }

    /*******************************************************************
     * Places all of the pieces required for a standard game of chess.
     ******************************************************************/
    private void placeStartingPieces() {
        //white player starts
        currentPlayer = Player.WHITE;

        //placing the pawns on the board
        for (int column = 0; column < board.length; column++) {
            board[1][column] = new Pawn(Player.BLACK);
            board[6][column] = new Pawn(Player.WHITE);
        }
        //placing the rest of the pieces
        board[0][0] = new Rook(Player.BLACK);
        board[0][1] = new Knight(Player.BLACK);
        board[0][2] = new Bishop(Player.BLACK);
        board[0][3] = new Queen(Player.BLACK);
        board[0][4] = new King(Player.BLACK);
        board[0][5] = new Bishop(Player.BLACK);
        board[0][6] = new Knight(Player.BLACK);
        board[0][7] = new Rook(Player.BLACK);

        board[7][0] = new Rook(Player.WHITE);
        board[7][1] = new Knight(Player.WHITE);
        board[7][2] = new Bishop(Player.WHITE);
        board[7][3] = new Queen(Player.WHITE);
        board[7][4] = new King(Player.WHITE);
        board[7][5] = new Bishop(Player.WHITE);
        board[7][6] = new Knight(Player.WHITE);
        board[7][7] = new Rook(Player.WHITE);
    }

    /******************************************************************
     * Checks for if either player has run out of moves.
     *
     * @return True if the game has ended in a stalemate.
     *****************************************************************/
    @Override
    public boolean isComplete() {
        //if a player is in check and cannot get out of check
        //the game is complete
        return ((inCheck(Player.WHITE) &&
                movesToEscapeCheck(Player.WHITE).isEmpty()) ||
                (inCheck(Player.BLACK) &&
                        movesToEscapeCheck(Player.BLACK).isEmpty()));
    }

    /*****************************************************************
     * Accepts a move and returns false based on whether the move will
     * put them in check or if isValidMove for the specific piece is
     * valid
     *
     * @param move a {@link Move} object describing the move to be made
     * @return true if the move is valid, false otherwise
     *****************************************************************/
    @Override
    public boolean isValidMove(Move move) { //overloaded

        //check if the move is valid
        if (!board[move.oldRow][move.oldColumn].
                isValidMove(move, board))
            return false;

        IChessPiece temp = null; //to store a piece to be captured
        if (board[move.newRow][move.newColumn] != null &&
                board[move.newRow][move.newColumn].player() !=
                        board[move.oldRow][move.oldColumn].player())
            temp = board[move.newRow][move.newColumn];

        //placing the move that needs to be validated on the board
        board[move.newRow][move.newColumn] =
                board[move.oldRow][move.oldColumn];
        if (move.wasEnPassant())
            board[move.oldRow][move.newColumn] = null;
        board[move.oldRow][move.oldColumn] = null;

        //if the move placed ends up putting player in check
        //then return false
        if (inCheck(currentPlayer)){

            //putting piece back to old location
            board[move.oldRow][move.oldColumn] =
                    board[move.newRow][move.newColumn];
            if (temp != null)
                //replacing captured piece
                board[move.newRow][move.newColumn] = temp;
            else
                board[move.newRow][move.newColumn] = null;
            return false;
        } else {

            //putting pieces back
            board[move.oldRow][move.oldColumn] =
                    board[move.newRow][move.newColumn];
            if (temp != null)
                board[move.newRow][move.newColumn] = temp;
            else
                board[move.newRow][move.newColumn] = null;
            return true;//move is okay if it didn't put player in check
        }

    }

    /******************************************************************
     * Moves a piece from a previous location to a new location if the
     * move is valid
     *
     * @param move {@link Move} object describing the move to be made
     *****************************************************************/
    @Override
    public void move(Move move) {
        if (isValidMove(move)) {
            //check if move is to an occupied location and
            //if the occupying piece belongs to the opponent
            //if so, set that piece to be captured
            if (board[move.newRow][move.newColumn] != null &&
                    board[move.newRow][move.newColumn].player() ==
                            board[move.oldRow][move.oldColumn].
                                    opponent()) {
                move.setCapturedPiece(
                        board[move.newRow][move.newColumn]);

                //deals with en passant
            } else if (board[move.oldRow][move.newColumn] != null &&
                    //piece moving is a pawn
                    board[move.oldRow][move.oldColumn].
                            type().equals("Pawn") &&
                    //piece passanted is pawn
                    board[move.oldRow][move.newColumn].
                            type().equals("Pawn") &&
                    board[move.oldRow][move.newColumn].player().
                            equals(board[move.oldRow][move.oldColumn].
                                    opponent())) {
                ((Pawn) board[move.oldRow][move.newColumn]).
                        setAbleToBePassanted(false);
                move.setWasEnPassant(true);
                //sets captured piece to row above/below moving pawn
                move.setCapturedPiece(
                        board[move.oldRow][move.newColumn]);
            }

            //Checks for a if a castle was performed.
            else if (board[move.oldRow][move.oldColumn].type().
                    equals("King") &&
                     board[move.newRow][move.newColumn] == null)
                moveCastle(move);

            //if there is a piece to be captured
            if (move.getCapturedPiece() != null)
                //add to list of respective player's captures
                if (currentPlayer.equals(Player.BLACK))
                    blackCaptures.add(move.getCapturedPiece());
                else
                    whiteCaptures.add(move.getCapturedPiece());



            //transferring piece from old square to new square
            board[move.newRow][move.newColumn] =
                    board[move.oldRow][move.oldColumn];

            if (move.wasEnPassant()) //removing pawn if en passanted
                board[move.oldRow][move.newColumn] = null;

            //removing piece from old square
            board[move.oldRow][move.oldColumn] = null;

            //if the King or Rook is moved, can't castle anymore
            IChessPiece temp = board[move.newRow][move.newColumn];
            if (temp.type().equals("King"))
                ((King) temp).canCastle = false;
            if (temp.type().equals("Rook"))
                ((Rook) temp).canCastle = false;
            if (temp.type().equals("Pawn"))
                ((Pawn) temp).setFirstTurn(false);

            //if a pawn reaches the other side of the board
            if (board[move.newRow][move.newColumn].type().
                    equals("Pawn") &&
                    ((board[move.newRow][move.newColumn].player() ==
                            Player.WHITE && move.newRow == 0) ||
                    (board[move.newRow][move.newColumn].player() ==
                            Player.BLACK && move.newRow == 7))) {

                //for loading games, a dialog shouldn't appear
                //during loading
                if (move.getPromotion() == null) {
                    //dialog to promote pawn
                    String[] options = {"Queen", "Rook",
                            "Bishop", "Knight"};
                    int n = JOptionPane.showOptionDialog(null,
                            "What rank would you like " +
                                    "to promote this pawn to?",
                            "Promotion", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null,
                            options, options[0]);
                    if (n == 0) {
                        board[move.newRow][move.newColumn] =
                                new Queen(currentPlayer);
                    } else if (n == 1) {
                        board[move.newRow][move.newColumn] =
                                new Rook(currentPlayer);
                    } else if (n == 2) {
                        board[move.newRow][move.newColumn] =
                                new Bishop(currentPlayer);
                    } else if (n == 3) {
                        board[move.newRow][move.newColumn] =
                                new Knight(currentPlayer);
                    }
                    move.setPromotion(
                            board[move.newRow][move.newColumn]);
                } else
                    board[move.newRow][move.newColumn] =
                            move.getPromotion();
            }

            if (board[move.newRow][move.newColumn].type().
                    equals("Pawn") &&
                    ((currentPlayer == Player.WHITE &&
                            move.newRow == move.oldRow - 2) ||
                            (currentPlayer == Player.BLACK &&
                                    move.newRow == move.oldRow + 2)))
                ((Pawn) board[move.newRow][move.newColumn]).
                        setAbleToBePassanted(true);
            else
                for (int row = 0; row < board.length; row++)
                    for (int col = 0; col < board.length; col++)
                        if (board[row][col] != null &&
                                board[row][col].type().equals("Pawn"))
                            ((Pawn) board[row][col]).
                                    setAbleToBePassanted(false);

            //adding move to the stack for undoing later
            moveStack.push(move);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /******************************************************************
     * Will move the king and rook into the proper pieces if a castle
     * is allowed to happen. This method checks for if it is allowed to
     * happen.
     *
     * @param move The move that needs to be executed.
     *
     * @author George
     * @version 3/26/18
     *****************************************************************/
    private void moveCastle(Move move) {
        boolean valid = true;

        //Looks to see if the space between the king and rook is clear
        if(move.oldColumn < move.newColumn)
            for(int i = move.oldColumn+1; i < 7 && valid; i++)
                if(board[move.newRow][i] != null)
                    valid = false;
        else if(move.oldColumn > move.newColumn)
            for(i = move.oldColumn-1; i > 0 && valid; i--)
                if(board[move.newRow][i] != null)
                    valid = false;
        if (inCheck(currentPlayer))
            valid = false;

        //If the path was clear
        if(valid) {
            if(board[move.newRow][move.newColumn - 1] != null)
                if(board[move.newRow][move.newColumn - 1].
                        type().equals("Rook")) {
                    board[move.newRow][move.newColumn + 1] =
                            board[move.newRow][move.newColumn - 1];
                    board[move.newRow][move.newColumn - 1] =
                            null;
                    move.setWasCastle(true);
                }
            if(board[move.newRow][move.newColumn + 1] != null)
                if(board[move.newRow][move.newColumn + 1].
                        type().equals("Rook")) {
                board[move.newRow][move.newColumn -1] =
                        board[move.newRow][move.newColumn +1];
                board[move.newRow][move.newColumn +1] =  null;
                move.setWasCastle(true);
            }
        }
    }

    /******************************************************************
     * Finds player p's king's location and determines whether possible
     * move for the opponent's pieces contains the king's location
     *
     * @author Allison
     * @param  p {@link Move} the Player to verify if in check
     * @return true if an opponent's piece can capture player p's king
     *****************************************************************/
    @Override
    public boolean inCheck(Player p) {
        int kingRow = 0;
        int kingColumn = 0;
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board.length; col++) {

                if (board[row][col] != null &&
                        board[row][col].player().equals(p) &&
                        board[row][col].type().equals("King")) {
                    kingRow = row;
                    kingColumn = col;
                    //avoid useless lines of code after finding the king
                    break;
                }
            }
        }
        for (int row = 0; row < board.length; row++)
            for (int col = 0; col < board.length; col++) {

                IChessPiece temp = board[row][col];

                //if piece exists and belongs to opponent
                if (temp != null && !temp.player().equals(p)) {

                    //find all valid moves for opponent
                    ArrayList<Move> moves = legalMoves(row, col);
                    for (Move move : moves) {

                        //check if valid moves includes capturing
                        // the king
                        if (move.newRow == kingRow &&
                                move.newColumn == kingColumn)

                            return true;
                    }
                }
            }
        return false;
    }

    /******************************************************************
     * Finds player p's king's location and moves pieces of the same
     * player it to see if that move will get it out of check. If it
     * does, adds it to an array and returns that array.
     *
     * @author Allison
     * @param p player that needs to escape check
     * @return the moves that get the player out of check, checkmate if
     *          this returns empty
     *****************************************************************/
    public ArrayList<Move> movesToEscapeCheck (Player p) {

        //array to hold escaping moves
        ArrayList<Move> moves = new ArrayList<>();

        //storing the king's location
        int kingRow = 0;
        int kingColumn = 0;

        //finding the king
        for (int row = 0; row < board.length; row++)
            for (int col = 0; col < board.length; col++)
                if (board[row][col] != null &&
                        board[row][col].player().equals(p) &&
                        board[row][col].type().equals("King")) {
                    kingRow = row;
                    kingColumn = col;
                    break; //stop after finding king
                }

        //check all current player pieces to see
        //if there's a move to get out of check
        for (int row = 0; row < board.length; row++)
            for (int col = 0; col < board.length; col++)
                if (board[row][col] != null &&
                        board[row][col].player() ==
                                board[kingRow][kingColumn].player()) {
                    ArrayList<Move> testMoves = legalMoves(row, col);
                    for (Move test : testMoves)
                        //isValidMove checks for putting in check
                        //so we can add this to the escaping moves
                        if (isValidMove(test))
                            moves.add(test);


                }
        return moves;
    }

    /******************************************************************
     * Creates moves from current location to all locations on the
     * board and tests to see if those moves are valid. If they are,
     * it adds them to an array and returns that array.
     *
     * @author Allison
     * @param currentRow row of the piece to be tested
     * @param currentCol column of the piece to be tested
     * @return valid moves for the piece at currentRow and currentCol
     *****************************************************************/
    public ArrayList<Move> legalMoves(int currentRow, int currentCol) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board.length; col++) {
                Move temp = new Move(currentRow, currentCol, row, col);
                if (board[currentRow][currentCol] != null &&
                        board[currentRow][currentCol].
                                isValidMove(temp, board))
                    possibleMoves.add(temp);
            }
        }
        return possibleMoves;
    }

    /*****************************************************************
     * Method that will take out moves that put a player in check.
     * Had to separate this method due to stack overflow errors.
     *
     * @author Allison
     * @param moves array of possible moves a piece can make, should
     *              be from legalMoves()
     * @return array of possible moves that will not put the player
     *         in check
     *****************************************************************/
    public ArrayList<Move> filterLegalMoves(ArrayList<Move> moves) {
        ArrayList<Move> betterMoves = new ArrayList<>();
        //take out moves that fail isValidMove(move)
        for (Move move: moves) {
            if (isValidMove(move))
                betterMoves.add(move);
        }
        return betterMoves;
    }

    /******************************************************************
     * Getter for the current player
     *
     * @return the player whose turn it is
     *****************************************************************/
    @Override
    public Player currentPlayer() {
        return currentPlayer;
    }

    /******************************************************************
     * Switches the current player
     *****************************************************************/
    public void switchPlayer() {
        if (currentPlayer().equals(Player.BLACK))
            currentPlayer = Player.WHITE;
        else
            currentPlayer = Player.BLACK;
    }

    /*****************************************************************
     * Getter for the piece at a specific location
     *
     * @param row the row of the piece to be returned
     * @param column the column of the piece to be returned
     * @return the IChessPiece at the given row and column
     *****************************************************************/
    public IChessPiece pieceAt(int row, int column) {
        return board[row][column];
    }

    /******************************************************************
     * Clears all data and ArrayLists and places pieces back into their
     * starting position
     *****************************************************************/
    public void reset() {
        //clear all data from the game
        blackCaptures.clear();
        whiteCaptures.clear();
        moveStack.clear();

        //remake the board
        board = new IChessPiece[8][8];
        placeStartingPieces();
        currentPlayer = Player.WHITE;
        handler = new GameFileHandler(this);
        gameData = "";
    }

    /******************************************************************
     * Reverts board back to the position before the most recent move
     * by popping the first element off the moveStack.
     *
     * @author Allison
     *****************************************************************/
    public void undoLastMove() {

        //if there were no moves made, exit the method
        if (moveStack.empty())
            return;

        //remove and return the previous move
        Move lastMove = moveStack.pop();

        //setting piece back to old location
        board[lastMove.oldRow][lastMove.oldColumn] =
                board[lastMove.newRow][lastMove.newColumn];

        //checking if the last move was a capture
        if (lastMove.getCapturedPiece() != null) {
            ArrayList<IChessPiece> captures;
            if (board[lastMove.oldRow][lastMove.oldColumn].
                    player().equals(Player.WHITE)) {
                captures = whiteCaptures;
            } else {
                captures = blackCaptures;
            }

            //set the old location to the captured piece
            if (captures.size() > 0) {
                //en passant requires different locations
                if (lastMove.wasEnPassant()) {
                    board[lastMove.oldRow][lastMove.newColumn] =
                            captures.get(captures.size() - 1);
                    board[lastMove.newRow][lastMove.newColumn] = null;
                } else //otherwise just put the captured piece back
                    board[lastMove.newRow][lastMove.newColumn] =
                            captures.get(captures.size() - 1);

                //piece is no longer captured after undo
                captures.remove(captures.size() - 1);
            }

            //undoing castling can be hard coded
        } else if (lastMove.wasCastle()) {
            if (lastMove.oldRow == 0 &&
                    lastMove.newColumn == 2) {
                board[0][4] = board[0][2];
                board[0][0] = board[0][3];
                board[0][2] = board[0][3] = null;
            } else if (lastMove.oldRow == 0 &&
                    lastMove.newColumn == 6) {
                board[0][4] = board[0][6];
                board[0][7] = board[0][5];
                board[0][6] = board[0][5] = null;
            } else if (lastMove.oldRow == 7 &&
                    lastMove.newColumn == 2) {
                board[7][4] = board[7][2];
                board[7][0] = board[7][3];
                board[7][2] = board[7][3] = null;
            } else {
                board[7][4] = board[7][6];
                board[7][7] = board[7][5];
                board[7][6] = board[7][5] = null;
            }
        } else //otherwise just set the previous location to null
            board[lastMove.newRow][lastMove.newColumn] = null;

        //if the King or Rook move was undone
        //they should now be able to castle
        IChessPiece temp = board[lastMove.oldRow][lastMove.oldColumn];
        if (temp.type().equals("King") &&
                lastMove.wasCastle())
            ((King) temp).canCastle = true;
        if (temp.type().equals("Rook") &&
                lastMove.wasCastle())
            ((Rook) temp).canCastle = true;
        if (temp.type().equals("Pawn") &&
                //if move was from starting position for black
                ((lastMove.oldRow == 1 &&
                        board[lastMove.oldRow][lastMove.oldColumn].
                                player().equals(Player.BLACK)) ||
                        //if move was from starting position for white
                        (lastMove.oldRow == 6 &&
                                board[lastMove.oldRow]
                                        [lastMove.oldColumn].
                                       player().equals(Player.WHITE))))
            ((Pawn) temp).setFirstTurn(true);
        //go back to previous player
        switchPlayer();

        //remove move from game data
        gameData = handler.removeLastMove();
    }


    /******************************************************************
     * Getter for the board
     *
     * @return the board that contains the chess pieces
     *****************************************************************/
    public IChessPiece[][] getBoard() {
        return board;
    }

    /******************************************************************
     * Getter for the list of pieces white has captured
     *
     * @return the list of pieces captured by white
     ******************************************************************/
    public ArrayList<IChessPiece> getWhiteCaptures() {
        return whiteCaptures;
    }

    /******************************************************************
     * Getter for the list of pieces black has captured
     *
     * @return the list of pieces captured by white
     *****************************************************************/
    public ArrayList<IChessPiece> getBlackCaptures() {
        return blackCaptures;
    }

    /******************************************************************
     * Setter for the string of moves in chess notation
     *
     * @param gameData the move sequence to use
     *****************************************************************/
    public void setGameData(String gameData) {
        this.gameData = gameData;
    }

    /******************************************************************
     * Getter for the string of move sequences
     *
     * @return String of moves in chess notation
     *****************************************************************/
    public String getGameData() {
        return gameData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /******************************************************************
     * Getter for the class that contains methods pertaining to reading
     * and writing files
     *
     * @return the instance of GameFileHandler used for this ChessModel
     *****************************************************************/
    public GameFileHandler getHandler() {
        return handler;
    }
}