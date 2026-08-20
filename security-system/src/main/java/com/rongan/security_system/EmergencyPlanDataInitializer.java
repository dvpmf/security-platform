package com.rongan.security_system;

import com.rongan.security_system.entity.EmergencyPlan;
import com.rongan.security_system.repository.EmergencyPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应急预案数据初始化
 * 系统启动时自动添加预设的应急预案
 */
@Component
public class EmergencyPlanDataInitializer implements CommandLineRunner {

    @Autowired
    private EmergencyPlanRepository planRepository;

    @Override
    public void run(String... args) {
        if (planRepository.count() > 0) {
            System.out.println("[预案初始化] 已存在预案数据，跳过初始化");
            return;
        }

        System.out.println("========================================");
        System.out.println("[预案初始化] 开始添加预设应急预案...");

        try {
            EmergencyPlan plan1 = new EmergencyPlan();
            plan1.setAlertType("烟雾超标");
            plan1.setTitle("仓库烟雾超标应急处理预案");
            plan1.setSteps("1. 立即查看告警信息，确认发生位置\n2. 通知现场人员撤离至安全区域\n3. 检查消防喷淋系统是否正常\n4. 拨打消防电话119\n5. 在安全区域清点人数\n6. 记录处理过程");
            plan1.setNotifyRoles("admin,user");
            planRepository.save(plan1);

            EmergencyPlan plan2 = new EmergencyPlan();
            plan2.setAlertType("高温告警");
            plan2.setTitle("设备高温预警处理预案");
            plan2.setSteps("1. 查看告警设备位置和温度\n2. 检查空调或通风系统\n3. 降低设备负载\n4. 现场人员检查设备\n5. 必要时关闭设备电源\n6. 记录处理过程");
            plan2.setNotifyRoles("admin");
            planRepository.save(plan2);

            EmergencyPlan plan3 = new EmergencyPlan();
            plan3.setAlertType("设备异常");
            plan3.setTitle("传感器设备异常处理预案");
            plan3.setSteps("1. 查看告警详情\n2. 检查设备电源连接\n3. 远程尝试重启设备\n4. 通知维护人员检修\n5. 更换备用传感器\n6. 记录处理过程");
            plan3.setNotifyRoles("admin");
            planRepository.save(plan3);

            System.out.println("[预案初始化] ✅ 已添加 3 条预设应急预案");
        } catch (Exception e) {
            System.err.println("[预案初始化] ❌ 添加失败: " + e.getMessage());
        }

        System.out.println("========================================");
    }
}
