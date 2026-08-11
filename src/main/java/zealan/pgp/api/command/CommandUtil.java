package zealan.pgp.api.command;

public class CommandUtil {
    static void verifyName(String name) {
        for (char c : name.toCharArray())
            if ((!Character.isLowerCase(c) || !Character.isLetter(c)) && c != '_')
                throw new IllegalArgumentException("Command name must be entirely lowercase letters + underscores");
    }
}
