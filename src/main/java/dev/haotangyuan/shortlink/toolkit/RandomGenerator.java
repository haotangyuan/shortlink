package dev.haotangyuan.shortlink.toolkit;

/**
 * @author: haotangyuan
 */
public final class RandomGenerator {

    public static String generateRandom() {
        return generateRandom(6);
    }

    /**
     * 生成随机字符串
     * @param length 长度
     * @return 随机字符串
     */
    public static String generateRandom(int length) {
        StringBuilder sb = new StringBuilder();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }
}
