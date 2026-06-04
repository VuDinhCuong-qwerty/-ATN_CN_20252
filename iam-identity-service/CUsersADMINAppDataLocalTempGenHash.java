import at.favre.lib.crypto.bcrypt.BCrypt;
public class GenHash {
    public static void main(String[] a) {
        System.out.println(BCrypt.withDefaults().hashToString(10, "1111".toCharArray()));
    }
}
