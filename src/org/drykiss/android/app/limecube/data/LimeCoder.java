
package org.drykiss.android.app.limecube.data;

public class LimeCoder {
    private static final String SYMBOL_NAME = "\\_NAME";

    private static final String SYMBOL_FIRSTNAME = "\\_FIRSTNAME";
    private static final String SYMBOL_LASTNAME = "\\_LASTNAME";

    private static final String[] SYMBOL_NAME_TYPE = {
            "\\_LASTONENAME", "\\_LASTTWONAME", "\\_LASTTHREENAME"
    };
    private static final String[] SYMBOL_NAME_REVERSE_TYPE = {
            "\\_FIRSTONENAME", "\\_FIRSTTWONAME", "\\_FIRSTTHREENAME"
    };

    private static final String SYMBOL_YA = "\\_YA";
    private static final String SYMBOL_DI = "야";
    private static final String SYMBOL_DK = "아";
    private static final String[] SYMBOL_NAME_SYMBOL_SUFFIX = {
            "...", "..", ",,,", ",,", ",", ".", "?"
    };

    private static final String[] SYMBOL_NAME_SUFFIX = {
            "야", "아", "형", "형님", "누나", "누님", "오빠", "언니", "아저씨", "선생님", "씨", "군", "양", "사원", "사원님", "원장님", "선임님",
            "부장님", "과장님", "차장님", "기사님", "기자님", "대표님", "사장님", "사모님"
    };

    static public String encode(String text, String name) {
        text = text.replace("\\_", "\\__");
        String[] tokens = text.split(" ");
        String encoded = "";
        for (String token : tokens) {
            String encodedToken = encodeToken(token, name, isKoreanName(name));
            encoded += encodedToken + " ";
        }
        encoded = encoded.substring(0, encoded.length() - 1);
        return encoded;
    }

    static private boolean isKoreanName(String name) {
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (c < 44032 || c > 55203) {
                return false;
            }
        }
        return true;
    }

    static private String getFirstName(String name) {
        final String[] token = name.split(" ");
        return token[0];
    }

    static private String getLastName(String name) {
        final String[] token = name.split(" ");
        final int tokenSize = token.length;
        return token[tokenSize - 1];
    }

    static private String encodeToken(String token, String name, boolean isKoreanName) {
        final String original = token;
        boolean yaExist = false;

        for (String suffix : SYMBOL_NAME_SYMBOL_SUFFIX) {
            if (token.endsWith(suffix)) {
                token = token.substring(0, token.length() - suffix.length());
                break;
            }
        }

        for (String suffix : SYMBOL_NAME_SUFFIX) {
            if (token.endsWith(suffix)) {
                token = token.substring(0, token.length() - suffix.length());
                if (isKoreanName && (suffix.equals(SYMBOL_DI) || suffix.equals(SYMBOL_DK))) {
                    yaExist = true;
                }
                break;
            }
        }

        if (token.equals(name)) {
            return replaceName(original, name, SYMBOL_NAME, yaExist);
        }

        if (isKoreanName) {
            for (int i = 1; i < SYMBOL_NAME_TYPE.length + 1; i++) {
                String splitName = name.substring(name.length() - i, name.length());
                if (token.equals(splitName)) {
                    return replaceName(original, splitName, SYMBOL_NAME_TYPE[i - 1], yaExist);
                }
                splitName = name.substring(0, i);
                if (token.equals(splitName)) {
                    return replaceName(original, splitName, SYMBOL_NAME_REVERSE_TYPE[i - 1],
                            yaExist);
                }
            }
        } else {
            final String firstName = getFirstName(name);
            if (token.equals(firstName)) {
                return original.replace(firstName, SYMBOL_FIRSTNAME);
            }
            final String lastName = getLastName(name);
            if (token.equals(lastName)) {
                return original.replace(lastName, SYMBOL_LASTNAME);
            }
        }
        return original;
    }

    static private String replaceName(String original, String name, String nameSymbol,
            boolean yaExist) {
        String encoded = original.replace(name, nameSymbol);
        if (yaExist) {
            int nameSymbolIndex = encoded.indexOf(nameSymbol);
            String tmp = encoded.substring(0, nameSymbolIndex + nameSymbol.length());
            String tmp2 = encoded.substring(nameSymbolIndex + nameSymbol.length() + 1,
                    encoded.length());
            encoded = tmp + SYMBOL_YA + tmp2;
        }
        return encoded;
    }

    static public String decode(String text, String name) {
        text = text.replace(SYMBOL_NAME, name);

        final boolean isKoreanName = isKoreanName(name);
        final String firstName = getFirstName(name);
        final String lastName = getLastName(name);

        for (int i = 1; i < SYMBOL_NAME_TYPE.length + 1; i++) {
            String splitName;
            if (isKoreanName) {
                splitName = name.substring(name.length() - i, name.length());
            } else {
                splitName = firstName;
            }
            text = text.replace(SYMBOL_NAME_TYPE[i - 1], splitName);

            if (isKoreanName) {
                splitName = name.substring(0, i);
            } else {
                splitName = lastName;
            }
            text = text.replace(SYMBOL_NAME_REVERSE_TYPE[i - 1], splitName);
        }

        String decoded = "";

        int lastPosition = 0;
        while (true) {
            int position = text.indexOf(SYMBOL_YA, lastPosition);
            if (position < 0) {
                decoded += text.substring(lastPosition, text.length());
                break;
            }
            String token = text.substring(lastPosition, position);
            lastPosition = position + SYMBOL_YA.length();
            char lastChar = token.charAt(token.length() - 1);
            decoded += token;
            decoded += hasSupportWord(lastChar) ? SYMBOL_DK : SYMBOL_DI;
        }

        decoded = decoded.replace("\\__", "\\_");
        return decoded;
    }

    static private boolean hasSupportWord(char c) {
        if (c < 44032 || c > 55203) {
            return false;
        }
        if (((c - 44032) % (21 * 28)) % 28 == 0) {
            return false;
        }
        return true;
    }
}
