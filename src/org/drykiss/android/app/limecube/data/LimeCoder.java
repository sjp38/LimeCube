
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

    private static final int SYMBOL_NAME_INT = 0;
    private static final int SYMBOL_FIRST_NAME_INT = 1;
    private static final int SYMBOL_LAST_NAME_INT = 2;
    private static final int SYMBOL_LASTONENAME_INT = 3;
    private static final int SYMBOL_LASTTWONAME_INT = 4;
    private static final int SYMBOL_LASTTHREENAME_INT = 5;
    private static final int SYMBOL_FIRSTONENAME_INT = 6;
    private static final int SYMBOL_FIRSTTWONAME_INT = 7;
    private static final int SYMBOL_FIRSTTHREENAME_INT = 8;

    private static final String SYMBOL_YA = "\\_YA";
    private static final String SYMBOL_DI = "야";
    private static final String SYMBOL_DK = "아";
    private static final String[] SYMBOL_NAME_SYMBOL_SUFFIX = {
            "...", "..", ",,,", ",,", ",", ".", "?"
    };

    private static final String[] SYMBOL_NAME_SUFFIX = {
            "야", "아", "이형님", "이형", "이누나", "이오빠", "이언니", "이", "형", "형님", "누나", "누님", "오빠", "언니",
            "아저씨",
            "씨", "군", "양", "사원", "사원님", "선임", "선임님", "책임님", "수석님", "선배", "선배님",
            "개발자", "개발자님", "디자이너", "디자이너님",
            "대리", "대리님", "부장", "부장님", "과장", "과장님", "차장", "차장님", "기사", "기사님", "기자", "기자님", "대표님",
            "사장님", "사모님",
            "교수", "교수님", "조교", "조교님", "박사", "박사님", "학부장", "학부장님",
            "선생", "선생님", "주임", "주임님", "교장", "교장 선생님", "장학사", "교육감", "교육감님", "교감", "교감 선생님",
            "원장님"
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
            for (int i = 0; i < SYMBOL_NAME_TYPE.length; i++) {
                String splitName = getSplitName(name, SYMBOL_LASTONENAME_INT + i);
                if (token.equals(splitName)) {
                    return replaceName(original, splitName, SYMBOL_NAME_TYPE[i], yaExist);
                }
                splitName = getSplitName(name, SYMBOL_FIRSTONENAME_INT + i);
                if (token.equals(splitName)) {
                    return replaceName(original, splitName, SYMBOL_NAME_REVERSE_TYPE[i],
                            yaExist);
                }
            }
        } else {
            final String firstName = getSplitName(name, SYMBOL_FIRST_NAME_INT);
            if (token.equals(firstName)) {
                return original.replace(firstName, SYMBOL_FIRSTNAME);
            }
            final String lastName = getSplitName(name, SYMBOL_LAST_NAME_INT);
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

    static private String getSplitName(String name, int nameType) {
        if (nameType == SYMBOL_NAME_INT) {
            return name;
        } else if (nameType == SYMBOL_FIRST_NAME_INT || nameType == SYMBOL_LAST_NAME_INT) {
            final String[] tokenByBlank = name.split(" ");
            return nameType == SYMBOL_FIRST_NAME_INT ? tokenByBlank[0]
                    : tokenByBlank[tokenByBlank.length - 1];
        }

        int splitStart = 0;
        int splitEnd = name.length();
        if (nameType >= SYMBOL_LASTONENAME_INT && nameType <= SYMBOL_LASTTHREENAME_INT) {
            splitStart = name.length() - (nameType - SYMBOL_LASTONENAME_INT + 1);
            if (splitStart < 0) {
                splitStart = 0;
            }
            splitEnd = name.length();
        } else if (nameType >= SYMBOL_FIRSTONENAME_INT && nameType <= SYMBOL_FIRSTTHREENAME_INT) {
            splitStart = 0;
            splitEnd = nameType - SYMBOL_FIRSTONENAME_INT + 1;
            if (splitEnd > name.length() - 1) {
                splitEnd = name.length();
            }
        }
        return name.substring(splitStart, splitEnd);
    }

    static public String decode(String text, String name) {
        text = text.replace(SYMBOL_NAME, name);

        final boolean isKoreanName = isKoreanName(name);
        final String firstName = getSplitName(name, SYMBOL_FIRST_NAME_INT);
        final String lastName = getSplitName(name, SYMBOL_LAST_NAME_INT);

        for (int i = 0; i < SYMBOL_NAME_TYPE.length; i++) {
            String splitName;
            if (isKoreanName) {
                splitName = getSplitName(name, SYMBOL_LASTONENAME_INT + i);
            } else {
                splitName = firstName;
            }
            text = text.replace(SYMBOL_NAME_TYPE[i], splitName);

            if (isKoreanName) {
                splitName = getSplitName(name, SYMBOL_FIRSTONENAME_INT + i);
            } else {
                splitName = lastName;
            }
            text = text.replace(SYMBOL_NAME_REVERSE_TYPE[i], splitName);
        }

        text = text.replace(SYMBOL_FIRSTNAME, firstName);
        text = text.replace(SYMBOL_LASTNAME, lastName);

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
