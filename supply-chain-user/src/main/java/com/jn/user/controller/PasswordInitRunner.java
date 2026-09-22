package com.jn.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jn.user.entity.SysUser;
import com.jn.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordInitRunner implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 查找 admin 用户
        SysUser admin = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin")
        );

        if (admin != null) {
            // 使用 Spring Security 配置的 BCrypt 编码器，生成真正的 123456 密文并更新
            String real123456Hash = passwordEncoder.encode("123456");
            admin.setPassword(real123456Hash);
            userMapper.updateById(admin);

            System.out.println("==================================================");
            System.out.println("======> 初始化成功：admin 密码已强制重置为 123456");
            System.out.println("======> 数据库更新后的密文: " + real123456Hash);
            System.out.println("==================================================");
        }
    }
}
