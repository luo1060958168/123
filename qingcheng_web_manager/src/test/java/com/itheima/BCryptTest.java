package com.itheima;

public class BCryptTest {
    public static void main(String[] args) {
        String gensalt = BCrypt.gensalt();// 盐
        System.out.println(gensalt);
        String hashpw = BCrypt.hashpw("123456", gensalt);
        System.out.println(hashpw);
        boolean checkpw = BCrypt.checkpw("232", "$2a$10$oyfDEZkzU6i8Bgo0GStXtuHqVK2XYNuvoPWQjdUTBOAT4k4SHx/9C");
        System.out.println(checkpw);
    }
}
