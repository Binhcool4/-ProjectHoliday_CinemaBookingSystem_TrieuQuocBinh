package springboot.scbs;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("====== BCRYPT GENERATOR ======");
        System.out.println("mk admin (123456): " + encoder.encode("123456"));
        System.out.println("mk staff (123456): " + encoder.encode("123456"));
        System.out.println("mk khach (123456): " + encoder.encode("123456"));
        System.out.println("==============================");
    }
}