public class ChessGUI {

 public static void main(String[] args) {
  JFrame frame = new JFRame("Chess Game");
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

  ChessPanel panel = new ChessPanel();
  frame.getContentPane().add(panel);

  frame.pack();
  frame.setVisible(true);
 }
}
