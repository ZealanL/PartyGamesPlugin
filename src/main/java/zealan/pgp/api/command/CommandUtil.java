package zealan.pgp.api.command;

public class CommandUtil {
    static void verifyName(String name) {
        for (char c : name.toCharArray())
            if ((!Character.isLetterOrDigit(c) || Character.isUpperCase(c)) && c != '_')
                throw new IllegalArgumentException("Command name must be entirely composed of lowercase letters/numbers and underscores");
    }
}
