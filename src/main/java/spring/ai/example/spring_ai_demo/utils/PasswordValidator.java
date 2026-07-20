package spring.ai.example.spring_ai_demo.utils;

import java.util.regex.Pattern;

/**
 * 密码强度校验工具（企业级）
 *
 * 规则：
 * - 长度8-32位
 * - 必须包含大写字母、小写字母、数字中的至少两种
 * - 不能包含连续3个及以上相同字符
 * - 不能包含常见弱密码
 */
public class PasswordValidator {

    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern REPEATED_CHARS = Pattern.compile("(.)\\1{2,}");

    private static final String[] WEAK_PASSWORDS = {
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "master", "dragon", "login", "admin",
            "welcome", "shadow", "sunshine", "trustno1", "iloveyou"
    };

    private PasswordValidator() {
    }

    /**
     * 校验密码强度
     *
     * @param password 待校验密码
     * @return 错误消息，null表示通过
     */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }

        if (password.length() < 8) {
            return "密码长度不能少于8位";
        }
        if (password.length() > 32) {
            return "密码长度不能超过32位";
        }

        // 检查弱密码
        String lowerPwd = password.toLowerCase();
        for (String weak : WEAK_PASSWORDS) {
            if (lowerPwd.contains(weak)) {
                return "密码过于简单，请勿使用常见弱密码";
            }
        }

        // 计算字符类型数量
        int typeCount = 0;
        if (UPPER_CASE.matcher(password).find()) typeCount++;
        if (LOWER_CASE.matcher(password).find()) typeCount++;
        if (DIGIT.matcher(password).find()) typeCount++;
        if (SPECIAL_CHAR.matcher(password).find()) typeCount++;

        if (typeCount < 2) {
            return "密码必须包含大写字母、小写字母、数字、特殊字符中的至少两种";
        }

        // 检查连续重复字符
        if (REPEATED_CHARS.matcher(password).find()) {
            return "密码不能包含3个及以上连续重复字符";
        }

        return null; // 校验通过
    }

    /**
     * 获取密码强度等级（供前端展示）
     *
     * @return 1=弱, 2=中, 3=强, 4=极强
     */
    public static int getStrengthLevel(String password) {
        if (password == null || password.length() < 8) return 1;

        int score = 0;
        if (password.length() >= 12) score++;
        if (UPPER_CASE.matcher(password).find()) score++;
        if (LOWER_CASE.matcher(password).find()) score++;
        if (DIGIT.matcher(password).find()) score++;
        if (SPECIAL_CHAR.matcher(password).find()) score++;
        if (password.length() >= 16) score++;

        if (score <= 2) return 1;
        if (score <= 3) return 2;
        if (score <= 4) return 3;
        return 4;
    }
}
