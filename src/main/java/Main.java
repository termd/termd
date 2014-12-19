/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Main {

  public static void main(String[] args) throws Exception {

    StringBuffer l = new StringBuffer();
    for (int i = 0;i < 200;i++) {
      l.append("*");
    }

//    System.out.print(l);
//    System.out.print("GHIJKL");
    System.out.write(27);
    System.out.write(7);
    System.out.print("abc");
    System.out.write(27);
    System.out.write(8);
    System.out.print("def");
//    System.out.print("\033[100C");

    System.in.read();

  }

}
