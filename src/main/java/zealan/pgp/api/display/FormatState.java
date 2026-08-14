package zealan.pgp.api.display;

import org.bukkit.ChatColor;

import java.util.HashSet;

public class FormatState {
    Character colorChar = null;
    HashSet<Character> styleChars = new HashSet<>();

    private FormatState() {}

    public static FormatState parse(String withFormat) {
        FormatState formatState = new FormatState();

        char[] charArray = withFormat.toCharArray();

        for (int i = 1; i < charArray.length; i++) {
            if (charArray[i-1] != '&' && charArray[i-1] != ChatColor.COLOR_CHAR)
                continue;

            char c = charArray[i];
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
                formatState.colorChar = c;
                formatState.styleChars.clear();
            } else if ("klmno".contains(String.valueOf(c))) {
                formatState.styleChars.add(c);
            } else if (c == 'r') {
                formatState.colorChar = null;
                formatState.styleChars.clear();
            }
        }

        return formatState;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.colorChar != null) {
            sb.append("&" + this.colorChar);
        }

        for (Character c : this.styleChars) {
            sb.append("&" + c);
        }

        return sb.toString().replace('&', ChatColor.COLOR_CHAR);
    }
}
