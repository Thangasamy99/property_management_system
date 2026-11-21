package com.pcc.finance.util;

import java.math.BigDecimal;

/** English number-to-words for receipt amounts (XAF, whole francs). */
public final class NumberToWords {

    private static final String[] ONES = {
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen"};
    private static final String[] TENS = {
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
    private static final String[] SCALES = {"", "thousand", "million", "billion", "trillion"};

    private NumberToWords() {}

    public static String xaf(BigDecimal amount) {
        if (amount == null) return "";
        long value = amount.longValue();
        if (value == 0) return "Zero francs CFA";
        StringBuilder sb = new StringBuilder();
        int scale = 0;
        long remaining = value;
        while (remaining > 0) {
            int chunk = (int) (remaining % 1000);
            if (chunk > 0) {
                String words = threeDigits(chunk) + (SCALES[scale].isEmpty() ? "" : " " + SCALES[scale]);
                sb.insert(0, words + " ");
            }
            remaining /= 1000;
            scale++;
        }
        String result = sb.toString().trim();
        return Character.toUpperCase(result.charAt(0)) + result.substring(1) + " francs CFA";
    }

    private static String threeDigits(int n) {
        StringBuilder sb = new StringBuilder();
        if (n >= 100) {
            sb.append(ONES[n / 100]).append(" hundred");
            n %= 100;
            if (n > 0) sb.append(" ");
        }
        if (n >= 20) {
            sb.append(TENS[n / 10]);
            if (n % 10 > 0) sb.append("-").append(ONES[n % 10]);
        } else if (n > 0) {
            sb.append(ONES[n]);
        }
        return sb.toString();
    }
}
