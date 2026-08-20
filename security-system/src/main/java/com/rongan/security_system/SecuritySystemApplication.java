package com.rongan.security_system;

import com.rongan.security_system.entity.User;
import com.rongan.security_system.entity.Device;
import com.rongan.security_system.repository.UserRepository;
import com.rongan.security_system.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class SecuritySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecuritySystemApplication.class, args);
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 项目启动时执行的初始化数据
     */
    @Bean
    public CommandLineRunner initData(UserRepository userRepository, DeviceRepository deviceRepository) {
        return args -> {
            // 如果用户表为空，则插入默认用户
            if (userRepository.count() == 0) {
                // 创建管理员
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setPhone("13800138000");
                admin.setRole("admin");
                userRepository.save(admin);

                // 创建普通用户
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setPhone("13900139000");
                user.setRole("user");
                userRepository.save(user);

                System.out.println("初始化用户：admin(管理员), user(普通用户)");
            }

            // 初始化设备
            if (deviceRepository.count() == 0) {
                Device cam1 = new Device();
                cam1.setName("大门摄像头");
                cam1.setType("camera");
                cam1.setLocation("大门");
                cam1.setStatus(1);
                cam1.setLastOnlineTime(java.time.LocalDateTime.now());
                deviceRepository.save(cam1);

                Device smoke1 = new Device();
                smoke1.setName("车间烟感");
                smoke1.setType("smoke");
                smoke1.setLocation("车间");
                smoke1.setStatus(1);
                smoke1.setLastOnlineTime(java.time.LocalDateTime.now());
                deviceRepository.save(smoke1);

                Device temp1 = new Device();
                temp1.setName("仓库温感");
                temp1.setType("temperature");
                temp1.setLocation("原料仓库");
                temp1.setStatus(1);
                temp1.setLastOnlineTime(java.time.LocalDateTime.now());
                deviceRepository.save(temp1);

                System.out.println("初始化设备：大门摄像头 (camera), 车间烟感 (smoke), 仓库温感 (temperature)");
            }
        };
    }
}
