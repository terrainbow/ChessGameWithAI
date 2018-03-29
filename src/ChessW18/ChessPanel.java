package ChessW18;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;

import static java.lang.System.exit;

public class ChessPanel extends JPanel {

	private JMenuItem gameItem;

	/** button to quit the game */
	private JMenuItem quitItem;

	/** button to undo the last move */
    private JMenuItem undoMove;
    private JMenuItem timeItem;
    private JMenuItem startItem;
    private JMenuItem stopItem;
    private JCheckBoxMenuItem countUpItem;
    private JMenuItem setTimerItem;

    /** because extra credit is key */
    private JCheckBoxMenuItem colorBlind;

    /** holds the buttons that make up the game board */
	private JButton[][] board;

	/** holds the game engine */
	private ChessModel model;

	/** variable to hold the size of the board */
	private final int BOARDSIZE = 8;

	private JLabel outMessage;
    private String message;

    /** images to represent white player pieces */
	private ImageIcon wPawn, wRook, wBishop, wKnight, wKing, wQueen;

	/** images to represent black player pieces */
	private ImageIcon bPawn, bRook, bBishop, bKnight, bKing, bQueen;

	/** the move attempted when selecting buttons on the board */
	private Move move;

	/** pieceChoice is used to tell if the player is choosing a piece
	 * or a position. */
	private boolean pieceChosen;

	/** the current player */
	private Player currentPlayer;

    /** panels to hold images of each player's captures */
    JPanel whiteCapturePanel, blackCapturePanel;

    /** holds labels that will have an icon set to represent captures for each player */
    ArrayList<JLabel> whiteCaptures = new ArrayList<>();
    ArrayList<JLabel> blackCaptures = new ArrayList<>();

    /** if true, competitive rules will apply on the timer */
    private boolean competitiveTimer = false;

    /** timer to count up or down and time gameplay */
    private Timer timer;

    /** variables to hold the amount of time on the timer */
    private int hours, minutes, seconds;

    /** holds amount of time (seconds) left for each player */
    private int whiteTime, blackTime;

    GameFileHandler handler;

    public ChessPanel(JMenuItem pquitItem, JMenuItem pgameItem,
                      JCheckBoxMenuItem colorBlind, JMenuItem undoMove,
                      JMenuItem pTime, JMenuItem pStart, JMenuItem pStop,
                      JCheckBoxMenuItem pCountUp, JMenuItem pSetTimer) {

        model = new ChessModel();
        handler = new GameFileHandler(model);
		move = new Move();
		currentPlayer = model.currentPlayer();
		pieceChosen = false;
		board = new JButton[BOARDSIZE][BOARDSIZE];
        JPanel boardPanel = boardInit();
        displayBoard();

		gameItem = pgameItem;
		quitItem = pquitItem;
		timeItem = pTime;
		startItem = pStart;
		stopItem = pStop;
        countUpItem = pCountUp;
        setTimerItem = pSetTimer;
		this.undoMove = undoMove;
		this.colorBlind = colorBlind;

		startItem.setEnabled(true);

        timer = new Timer(1000, new TimerListener());
        timer.setActionCommand("Up");
        countUpItem.setSelected(true);

		quitItem.addActionListener(e -> exit(0));
		gameItem.addActionListener( e -> resetBoard());
        startItem.addActionListener(e -> timer.start());
        stopItem.addActionListener(e -> timer.stop());
        countUpItem.addActionListener(e -> {
            if (timer.getActionCommand().equals("Up"))
                timer.setActionCommand("Down");
            else
                timer.setActionCommand("Up");
        });
        setTimerItem.addActionListener(e -> {
            try {
                whiteTime = blackTime = seconds = 60 * Integer.parseInt(
                        JOptionPane.showInputDialog(null,
                                "Set the timer in minutes.", "Set Timer",
                                JOptionPane.QUESTION_MESSAGE));
            }
            catch(NumberFormatException event) {
                message = "Please enter a time of only minutes.";
            }
            fixTime();
            timeItem.setText(hours + ":" + minutes + ":" + seconds);
        });

		this.undoMove.addActionListener(e -> {
            model.undoLastMove(); //undo's previous move
            currentPlayer = model.currentPlayer(); //make sure player is switched
            displayBoard(); //set the icons
            updateCaptures();
        });
		this.colorBlind.addActionListener(e -> {
		    if (this.colorBlind.isSelected())
		        //setting the color for the grid of icons
		        //black and white is harsh on the eyes and
		        //doesn't show the pieces very well
		        for(int row = 0; row < BOARDSIZE; row++)
		            for(int col = 0; col < BOARDSIZE; col++)
		                if ((row % 2 == 1 && col % 2 == 1) ||
                        (row % 2 == 0 && col % 2 == 0)) {
		                    //color blind friendly white
                            board[row][col].setBackground(new Color(240, 228, 66));
                        } else {
		                    //color blind friendly black
                            board[row][col].setBackground(new Color(0, 114, 178));
                        }
		    else
                for (int row = 0; row < BOARDSIZE; row++)
                    for (int col = 0; col < BOARDSIZE; col++)
                        if ((row % 2 == 1 && col % 2 == 1) ||
                                (row % 2 == 0 && col % 2 == 0))
                            //light yellow to contrast white pieces
                            board[row][col].setBackground(new Color(246, 249, 182));
                        else
                            //dark green to contrast black pieces
                            board[row][col].setBackground(new Color(72, 109, 42));

            });
// time, start, stop, countUp, setTimer

		//setting the timer to start with a modest amount of time
		minutes = 30;

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        //panel with the chess board
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridwidth = 2;
        constraints.gridheight = 3;
        add(boardPanel, constraints);

        //panel for the captures
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.gridwidth = 1;
        constraints.gridheight = 2;
        add(createCapturesPanel(), constraints);

//        ArrayList<String> moveCodes = handler.separateMoves(
//                        handler.readGameFile("SampleGame"));
//        for (int i = 0; i < moveCodes.size(); i++) {
//            Move temp = handler.decodeMove(moveCodes.get(i));
//            model.move(temp);
//            model.switchPlayer();
//            currentPlayer = model.currentPlayer();
//        }
//        displayBoard();
//        updateCaptures();
    }

    /**
     * sets minutes and hours correctly if seconds > 60 or seconds < -60
     */
    private void fixTime() {

        //if in negative time, set correct hours
        while (minutes < 0) {
            minutes += 60;
            hours--;
        }

        //if in negative time, set the correct minutes
        while (seconds < 0) {
            seconds += 60;
            minutes--;
        }

        //if there are too many seconds, add to minutes
        while (seconds >= 60) {
            seconds -= 60;
            minutes++;
        }

        //if there are too many minutes, add to hours
        while (minutes >= 60) {
            minutes -= 60;
            hours++;
        }
    }

    /**
     * Method to set the time to desired amount of seconds
     *
     * @param seconds
     */
    private void setTime(int seconds) {
        minutes = hours = 0;
        this.seconds = seconds;
        fixTime();
    }

    /**
     *
     */
    public class TimerListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (timer.getActionCommand().equals("Up")) {
                seconds++;
            } else if (timer.getActionCommand().equals("Down")) {
                    seconds--;
                    //if using the competitive timer, time should be taken away
                    //from the right player
                    if (competitiveTimer) {
                        if (currentPlayer == Player.WHITE)
                            whiteTime--;
                        else
                            blackTime--;
                    }
            }
            fixTime();

            //setting the label to take the form 0:00:00
            if (seconds < 0 || minutes < 0 || hours < 0)
                timeItem.setText(hours + ":" + minutes + ":" + seconds);
            else if (minutes >= 10 && seconds >= 10)
                timeItem.setText(hours + ":" + minutes + ":" + seconds);
            else if (minutes >= 10 && seconds < 10)
                timeItem.setText(hours + ":" + minutes + ":0" + seconds);
            else if (minutes < 10 && seconds >= 10)
                timeItem.setText(hours + ":0" + minutes + ":" + seconds);
            else
                timeItem.setText(hours + ":0" + minutes + ":0" + seconds);

            if (competitiveTimer) {
                if (whiteTime == 0) {
                    //TODO: game over
                } else if (blackTime == 0) {

                }
            }

        }
    }

    /**
     *
     * @return the panel that holds each player's captures
     */
    private JPanel createCapturesPanel() {

        whiteCapturePanel = new JPanel();
        whiteCapturePanel.setLayout(new GridLayout(4, 4, 7, 7));
        whiteCapturePanel.setBorder(new LineBorder(Color.white, 7));
        whiteCapturePanel.setBackground(new Color(189, 221, 175));

        blackCapturePanel = new JPanel();
        blackCapturePanel.setLayout(new GridLayout(4, 4, 7, 7));
        blackCapturePanel.setBorder(new LineBorder(Color.black, 7));
        blackCapturePanel.setBackground(new Color(189, 221, 175));

        JPanel captures = new JPanel();
        captures.setLayout(new GridLayout(2, 1));
        captures.add(whiteCapturePanel);
        captures.add(blackCapturePanel);
        captures.setPreferredSize(new Dimension(400, 700));

        return captures;
    }

    /**
     * Adds to the panel of captures if a piece is taken
     *
     */
    private void updateCaptures() {

        //adds the last element of the model.blackCaptures array if
        //its size is bigger than the blackCaptures JLabel array
	    if (model.getBlackCaptures().size() > blackCaptures.size())  {
            for (int i = model.getBlackCaptures().size() - blackCaptures.size(); i > 0; i--) {
                JLabel label = new JLabel();
                label.setIcon(getPieceIcon(Player.WHITE,
                        model.getBlackCaptures().get(model.getBlackCaptures().size() - i).type()));
                blackCaptures.add(label);
                blackCapturePanel.add(label);
            }

	        //or remove if needed
        } else if (model.getBlackCaptures().size() < blackCaptures.size()) {
	        blackCapturePanel.remove(blackCaptures.size() - 1);
	        blackCaptures.remove(blackCaptures.size() - 1);
        }

        //adds to the white captures panel if needed
        if (model.getWhiteCaptures().size() > whiteCaptures.size()) {
            for (int i = model.getWhiteCaptures().size() - whiteCaptures.size(); i > 0; i--) {
                JLabel label = new JLabel();
                label.setIcon(getPieceIcon(Player.BLACK, model.getWhiteCaptures().get(
                        model.getWhiteCaptures().size() - i).type()));
                whiteCaptures.add(label);
                whiteCapturePanel.add(label);
            }

	        //or remove if needed
        } else if (model.getWhiteCaptures().size() < whiteCaptures.size()) {
	        whiteCapturePanel.remove(whiteCaptures.size() - 1);
	        whiteCaptures.remove(whiteCaptures.size() - 1);
        }
        blackCapturePanel.repaint();
	    whiteCapturePanel.repaint();
    }

	/**
	 * Sets up the board, a two dimensional matrix of JButtons,
	 * to contain an icon for a chess piece in each button. The
	 * icons are specific to each button.
	 *
	 * @author George
	 * @version 3/18
	 */
	private JPanel boardInit() {

	    JPanel boardPanel = new JPanel();

		wPawn = new ImageIcon("wPawn.png");
		wRook = new ImageIcon("wRook.png");
		wBishop = new ImageIcon("wBishop.png");
		wKnight = new ImageIcon("wKnight.png");
		wKing = new ImageIcon("wKing.png");
		wQueen = new ImageIcon("wQueen.png");

		bPawn = new ImageIcon("bPawn.png");
		bRook = new ImageIcon("bRook.png");
		bBishop = new ImageIcon("bBishop.png");
		bKnight = new ImageIcon("bKnight.png");
		bKing = new ImageIcon("bKing.png");
		bQueen = new ImageIcon("bQueen.png");

		ButtonListener listener = new ButtonListener();

//		setLayout(new GridBagLayout()); /** I'm setting this up so that
//		 the buttons will fall into place when created */
        boardPanel.setLayout(new GridLayout(BOARDSIZE, BOARDSIZE)); //this can be changed back, just temporary for seeing the grid
        boardPanel.setPreferredSize(new Dimension(700, 700));

		//Creating the grid of buttons
		for(int row = 0; row < BOARDSIZE; row++)
			for(int col = 0; col < BOARDSIZE; col++) {
		        board[row][col] = new JButton();
				board[row][col].addActionListener(listener);
				//making a grid. maybe it should have its own panel?
                if ((row % 2 == 1 && col % 2 == 1) ||
                        (row % 2 == 0 && col % 2 == 0))
                    board[row][col].setBackground(new Color(72, 109, 42));
                else
                    board[row][col].setBackground(new Color(246, 249, 182));
				boardPanel.add(board[row][col]);
			}

		outMessage = new JLabel("");
        return boardPanel;
	}


	/******************************************************************
	 * Method that updates the board by setting the correct icons.
	 *
	 * @author Allison
	 *****************************************************************/
	private void displayBoard() {
		for (int row = 0; row < BOARDSIZE; row++) {
			for (int col = 0; col < BOARDSIZE; col++) {
				IChessPiece temp = model.pieceAt(row,col); //variable to improve readability
				if (temp == null) { //if there is no piece, keep going on the loop
                    board[row][col].setIcon(null);
                    continue;
                }

                //set the icon to the correct piece and player
                board[row][col].setIcon(getPieceIcon(temp.player(), temp.type()));
			}
		}
//		outMessage.setText(message);
	}

	/***************************************************************
	 * Puts the board in the starting setup.
	 *
	 **************************************************************/
	private void resetBoard() {
//		board = new JButton[BOARDSIZE][BOARDSIZE];
		boardInit();
		displayBoard();
		repaint();
	}


    /**
     * Returns the icon that matches a given player and piece type
     *
     * @param player player that owns the piece
     * @param type type of piece to get an icon for
     * @return the icon that matches a player and piece type
     */
    private ImageIcon getPieceIcon(Player player, String type) {
        ImageIcon icon = new ImageIcon();
        switch (type) {
            case "Pawn":
                if (player == Player.BLACK)
                    icon = bPawn;
                else
                    icon = wPawn;
                break;
            case "Knight":
                if (player == Player.BLACK)
                    icon = bKnight;
                else
                    icon = wKnight;
                break;
            case "Bishop":
                if (player == Player.BLACK)
                    icon = bBishop;
                else
                    icon = wBishop;
                break;
            case "Rook":
                if (player == Player.BLACK)
                    icon = bRook;
                else
                    icon = wRook;
                break;
            case "King":
                if (player == Player.BLACK)
                    icon = bKing;
                else
                    icon = wKing;
                break;
            case "Queen":
                if (player == Player.BLACK)
                    icon = bQueen;
                else
                    icon = wQueen;

        }
        return icon;
    }


	/**
	 * Inner class that represents action listener for buttons
	 */
	private class ButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {
			/*
			 * We need to store the location of the first button, and
			 * then store the location of the second button.
			 * -If the player chooses the first button, this will be
			 * dealt with through the move in ChessModel. The player
			 * must choose another button.
			 * -If the player chooses a different button, then this will
			 * be stored and model.move is called. The turn will be
			 * switched in chessModel.
			 *
			 * */
            String game;
			for (int row = 0; row < board.length; row++)
				for (int col = 0; col < board.length; col++)
					if (event.getSource() == board[row][col]) {
			            boolean check = model.inCheck(currentPlayer);
						if(!pieceChosen &&
                                model.pieceAt(row, col) != null && //need to make sure user is selecting an actual piece
                                model.pieceAt(row, col).player().equals(currentPlayer)) {//prevents playing for opponent
						        move.oldRow = row;
                                move.oldColumn = col;
						    if (!check) {

                                ArrayList<Move> moves = model.filterLegalMoves(model.legalMoves(row, col));
                                for (Move move : moves)
                                    board[move.newRow][move.newColumn].setBorder(new LineBorder(Color.blue, 5));

                                //a border so you can see which piece is selected
                                board[row][col].setBorder(new LineBorder(Color.orange, 5));
                            } else { //player in check should only be able to move out of check

                                ArrayList<Move> moves = model.movesToEscapeCheck(currentPlayer);
                                if (model.isComplete()) { //if there are no moves to escape check, then checkmate
                                    System.out.println("GAME OVER");
                                    break;
                                }
                                for (Move move : moves) {//show border for moves that get player out of check
                                    board[move.newRow][move.newColumn].setBorder(new LineBorder(Color.green, 5));
                                    board[move.oldRow][move.oldColumn].setBorder(new LineBorder(Color.red, 2));
                                }
//                                board[row][col].setBorder(new LineBorder(Color.red, 5));

                            }
                                pieceChosen = true;
						} else if (pieceChosen){ //stores the second pressed button.
							move.newColumn = col;
							move.newRow = row;
                            for (int r = 0; r < board.length; r++)
                                for (int c = 0; c < board.length; c++)
                                    if (board[r][c].getBorder() != null)
                                        board[r][c].setBorder(null);

							//not sure if he wants an invalid move to throw an error
                            try {
                                if (!check) {

//                                    model.move(move);//The move method that is called here will check for validity.

                                    //this moves the pieces
                                    //also creates a string in standard chess pieces to represent the move
                                    if (model.isValidMove(move)) {
                                        game = handler.buildGameData(handler.moveAndGenerateCode(move));
                                        model.switchPlayer();
                                        currentPlayer = model.currentPlayer();
                                        move = new Move(); //to prevent null pointer errors from trying to move a piece that isn't there anymore
                                        model.setGameData(game);
//                                    System.out.println(game);
//                                    game = handler.removeLastMove(game);
//                                    System.out.println(game);
                                    }

                                } else {
                                    boolean moveWillEscape = false;
                                    move.newRow = row;
                                    move.newColumn = col;
                                    for (Move testMove :
                                            model.movesToEscapeCheck(currentPlayer))
                                        //check if the move the player is trying to make is an escaping one
                                        if (testMove.oldRow == move.oldRow &&
                                                testMove.oldColumn == move.oldColumn &&
                                                testMove.newRow == move.newRow &&
                                                testMove.newColumn == move.newColumn) {
                                            moveWillEscape = true;
                                        }

                                    if (moveWillEscape) {
                                        model.move(move);
                                        model.switchPlayer();
                                        currentPlayer = model.currentPlayer();
                                        move = new Move();
                                    }
                                }
                                //set the timer to the current player's time
                                if (competitiveTimer)
                                    setTime(currentPlayer == Player.WHITE ? whiteTime : blackTime);

//                                ChessAI ai = new ChessAI(Player.BLACK, model);
//                                Move aiMove = ai.scanDatabase();
//                                if (aiMove.oldRow == 0 && aiMove.oldColumn == 0 &&
//                                        aiMove.newRow == 0 && aiMove.newColumn == 0) {
//                                    System.out.println("AI Database Failed");

//                                }
//                                else {
//                                    game = handler.buildGameData(handler.moveAndGenerateCode(aiMove));
//                                    model.setGameData(game);
//                                    model.switchPlayer();
//                                    currentPlayer = model.currentPlayer();
//                                }
                            } catch (IllegalArgumentException e) {
                                message = "Illegal Move";
                            }

							pieceChosen = false;
                            displayBoard();
						}
						updateCaptures();
					}
		}
	}
}


//    /**
//     * Creates the timer panel and adds buttons to it
//     *
//     * @return the panel that holds the timer and its buttons
//     */
//    private JPanel createTimerPanel() {
//
//        JPanel timerPanel = new JPanel();
//        timerLabel = new JLabel("0:30:00");
//        timer = new Timer(1000, new TimerListener());
//        timer.setActionCommand("Down"); //timer starts counting down
//        timerPanel.add(timerLabel);
//
//        //to start the timer
//        JButton start = new JButton("Start");
//        start.addActionListener(e -> timer.start());
//        timerPanel.add(start);
//
//        //to stop the timer
//        JButton stop = new JButton("Stop");
//        stop.addActionListener(e -> timer.stop());
//        timerPanel.add(stop);
//
//        //to change whether timer counts up or down
//        JCheckBox countUp = new JCheckBox("Count Up");
////        countUp.setSelected(true);
//        countUp.addActionListener(e -> {
//            if (timer.getActionCommand().equals("Up"))
//                timer.setActionCommand("Down");
//            else
//                timer.setActionCommand("Up");
//        });
//        timerPanel.add(countUp);
//
//        //to set the timer
//        JButton setTimer = new JButton("Set Timer");
//        setTimer.addActionListener(e -> {
//            whiteTime = blackTime = seconds = 60 * Integer.parseInt(
//                    JOptionPane.showInputDialog(null,
//                            "Set the timer in minutes.", "Set Timer",
//                            JOptionPane.QUESTION_MESSAGE));
//            fixTime();
//            timerLabel.setText(hours + ":" + minutes + ":" + seconds);
//        });
//        timerPanel.add(setTimer);
//
//        //to change whether using competitive timer mode or not
//        JCheckBox useCompetitiveTimer = new JCheckBox("Competitive Timer");
//        useCompetitiveTimer.addActionListener(e -> {
//            competitiveTimer = !competitiveTimer;
//        });
//        timerPanel.add(useCompetitiveTimer);
//
//        return timerPanel;
//    }