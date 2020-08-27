public class ClassConstantsExample {
    public static final int RECT_SIZE = 5;
    public static void main(String[] args) {
        for(int i = 0; i < RECT_SIZE; i++) {
            for(int j = 0; j < RECT_SIZE * 2; j++) {
                System.out.print("*");
            }
            System.out.println();
        }
    }
}