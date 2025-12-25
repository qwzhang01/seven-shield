/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 *  all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package io.github.qwzhang01.shield.shield;

import java.util.regex.Pattern;

/**
 * Comprehensive data masking algorithm implementation with support for
 * multiple data types.
 *
 * <p>This class provides production-ready masking algorithms for:</p>
 * <ul>
 *   <li>Phone numbers - Masks middle 4 digits (138****5678)</li>
 *   <li>ID cards - Masks middle 8 or 6 digits based on length</li>
 *   <li>Email addresses - Masks username except first and last char</li>
 *   <li>Chinese names - Keeps first and last character</li>
 *   <li>English names - Keeps first and last character</li>
 * </ul>
 *
 * <p>All masking methods include validation to ensure the input matches
 * expected patterns before applying masking. Invalid inputs are returned
 * unchanged.</p>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class RoutineCoverAlgo implements CoverAlgo {

    private static final String MASK_CHAR = "*";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d" +
            "{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{15" +
            "}|\\d{18}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w" +
            ".-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00" +
            "-\\u9fa5]+$");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("^[a-zA-Z" +
            "]+$");

    /**
     * Default masking implementation that returns a fixed mask string.
     * Override this method in subclasses for custom masking behavior.
     *
     * @param content the content to mask
     * @return a fixed mask string "*****"
     */
    @Override
    public String mask(String content) {
        return "*****";
    }

    /**
     * Masks phone numbers following Chinese mobile phone format.
     *
     * <p>Masking pattern: 138****5678 (keeps first 3 and last 4 digits)</p>
     * <p>Validation: Must match pattern ^1[3-9]\d{9}$ (11-digit Chinese
     * mobile)</p>
     *
     * @param phone the phone number to mask
     * @return masked phone number, or original if validation fails
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }

        phone = phone.trim();

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return phone;
        }

        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * Masks Chinese ID card numbers (15 or 18 digits).
     *
     * <p>Masking patterns:</p>
     * <ul>
     *   <li>18-digit: 110101********1234 (keeps first 6 and last 4)</li>
     *   <li>15-digit: 110101******123 (keeps first 6 and last 3)</li>
     * </ul>
     *
     * @param idCard the ID card number to mask
     * @return masked ID card, or original if validation fails
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return idCard;
        }

        idCard = idCard.trim();

        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return idCard;
        }

        if (idCard.length() == 18) {
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        } else if (idCard.length() == 15) {
            return idCard.substring(0, 6) + "******" + idCard.substring(12);
        }

        return idCard;
    }

    /**
     * Masks email addresses while preserving domain information.
     *
     * <p>Masking pattern: u***r@example.com (keeps first and last character
     * of username)</p>
     * <p>For usernames with 2 or fewer characters, only keeps first
     * character.</p>
     *
     * @param email the email address to mask
     * @return masked email, or original if validation fails
     */
    public String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }

        email = email.trim();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return username.charAt(0) + MASK_CHAR + domain;
        } else {
            StringBuilder masked = new StringBuilder();
            masked.append(username.charAt(0));
            for (int i = 1; i < username.length() - 1; i++) {
                masked.append(MASK_CHAR);
            }
            masked.append(username.charAt(username.length() - 1));
            masked.append(domain);
            return masked.toString();
        }
    }

    /**
     * Masks Chinese names while preserving cultural readability.
     *
     * <p>Masking patterns:</p>
     * <ul>
     *   <li>Single character: Not masked (e.g., "王" → "王")</li>
     *   <li>Two characters: "张三" → "张*"</li>
     *   <li>Three+ characters: "欧阳锋华" → "欧*锋*"</li>
     * </ul>
     *
     * @param name the Chinese name to mask
     * @return masked name, or original if validation fails or single character
     */
    public String maskChineseName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        name = name.trim();

        if (!CHINESE_PATTERN.matcher(name).matches()) {
            return name;
        }

        if (name.length() == 1) {
            return name;
        } else if (name.length() == 2) {
            return name.charAt(0) + MASK_CHAR;
        } else {
            StringBuilder masked = new StringBuilder();
            masked.append(name.charAt(0));
            for (int i = 1; i < name.length() - 1; i++) {
                masked.append(MASK_CHAR);
            }
            masked.append(name.charAt(name.length() - 1));
            return masked.toString();
        }
    }

    /**
     * Masks English names while preserving first and last characters.
     *
     * <p>Masking patterns:</p>
     * <ul>
     *   <li>1-2 characters: "Jo" → "J*"</li>
     *   <li>3+ characters: "John" → "J**n", "Smith" → "S***h"</li>
     * </ul>
     *
     * @param name the English name to mask (alphabetic characters only)
     * @return masked name, or original if validation fails
     */
    public String maskEnglishName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        name = name.trim();

        if (!ENGLISH_PATTERN.matcher(name).matches()) {
            return name;
        }

        if (name.length() <= 2) {
            return name.charAt(0) + MASK_CHAR;
        } else {
            StringBuilder masked = new StringBuilder();
            masked.append(name.charAt(0));
            for (int i = 1; i < name.length() - 1; i++) {
                masked.append(MASK_CHAR);
            }
            masked.append(name.charAt(name.length() - 1));
            return masked.toString();
        }
    }
}