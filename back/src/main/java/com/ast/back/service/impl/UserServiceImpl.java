package com.ast.back.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.ast.back.common.BusinessException;
import com.ast.back.dto.LoginDTO;
import com.ast.back.dto.RegisterDTO;
import com.ast.back.entity.StudentInfo;
import com.ast.back.entity.TeacherInfo;
import com.ast.back.entity.User;
import com.ast.back.mapper.UserMapper;
import com.ast.back.service.StudentInfoService;
import com.ast.back.service.TeacherInfoService;
import com.ast.back.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private StudentInfoService studentInfoService;

    @Autowired
    private TeacherInfoService teacherInfoService;

    /**
     * 登录逻辑
     */
    @Override
    public User login(LoginDTO loginDTO) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));

        if (user == null) throw new BusinessException("用户不存在");
        if (!user.getPassword().equals(loginDTO.getPassword())) throw new BusinessException("密码错误");
        if (user.getStatus() == 0) throw new BusinessException("账号已禁用");

        return user;
    }

    /**
     * 注册逻辑
     */
    @Override
    public void register(RegisterDTO dto, jakarta.servlet.http.HttpSession session) {
        System.out.println(dto);
        User user = dto.getUser();
        // 1. 基本校验
        if (user == null || StrUtil.isBlank(user.getUsername())) throw new BusinessException("用户名不能为空");

        // 验证码校验
        String sessionCode = (String) session.getAttribute("emailCode");
        String sessionEmail = (String) session.getAttribute("emailTarget");
        Long sessionTime = (Long) session.getAttribute("emailTime");

        if (sessionCode == null || sessionEmail == null || sessionTime == null) {
            throw new BusinessException("请先获取验证码");
        }

        if (!sessionEmail.equals(user.getEmail())) {
            throw new BusinessException("获取验证码的邮箱与当前邮箱不一致");
        }

        if (System.currentTimeMillis() - sessionTime > 5 * 60 * 1000) {
            throw new BusinessException("验证码已过期，请重新获取");
        }

        if (!sessionCode.equals(dto.getEmailCode())) {
            throw new BusinessException("验证码错误");
        }

        // 验证通过，清除session中的验证码
        session.removeAttribute("emailCode");
        session.removeAttribute("emailTarget");
        session.removeAttribute("emailTime");

        // 2. 查重
        User exist = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (exist != null) throw new BusinessException("用户名已存在");

        // 3. 教师邀请码校验
        if ("TEACHER".equals(user.getRole()) && !"TEA888".equals(dto.getInviteCode())) {
            throw new BusinessException("邀请码错误");
        }

        // 4. 填充默认值并保存
        user.setCreateTime(LocalDateTime.now());
        user.setStatus(1);
        
        // 5. 生成唯一UID
        user.setUid(generateUniqueUid());
        
        this.save(user);

        // 6. 如果是学生，保存学生详细信息
        if ("STUDENT".equals(user.getRole()) && dto.getStudentInfo() != null) {
            StudentInfo info = dto.getStudentInfo();
            info.setUserId(user.getId());
            info.setCreateTime(LocalDateTime.now());
            // 简单校验学号
            if (StrUtil.isNotBlank(info.getStudentNumber())) {
                // 检查学号是否重复（可选）
                long count = studentInfoService.count(new LambdaQueryWrapper<StudentInfo>()
                        .eq(StudentInfo::getStudentNumber, info.getStudentNumber()));
                if (count > 0) {
                    throw new BusinessException("学号已存在");
                }
                studentInfoService.save(info);
            }
        }
        
        // 7. 如果是教师，保存教师详细信息
        if ("TEACHER".equals(user.getRole()) && dto.getTeacherInfo() != null) {
            TeacherInfo info = dto.getTeacherInfo();
            info.setUserId(user.getId());
            info.setCreateTime(LocalDateTime.now());
            
            if (StrUtil.isNotBlank(info.getTeacherNumber())) {
                // 检查教师编号是否重复
                long count = teacherInfoService.count(new LambdaQueryWrapper<TeacherInfo>()
                        .eq(TeacherInfo::getTeacherNumber, info.getTeacherNumber()));
                if (count > 0) {
                    throw new BusinessException("教师编号已存在");
                }
                teacherInfoService.save(info);
            }
        }
    }
    
    private String generateUniqueUid() {
        while (true) {
            String uid = RandomUtil.randomNumbers(10);
            User exist = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUid, uid));
            if (exist == null) {
                return uid;
            }
        }
    }

    /**
     * 找回并重置密码逻辑 (从 NoticeServiceImpl 挪过来的)
     */
    @Override
    public void resetPassword(String email, String newPassword) {
        // 在这里，this.getOne 操作的就是 User 实体
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));

        if (user == null) {
            throw new BusinessException("该邮箱未绑定任何账号");
        }

        user.setPassword(newPassword);
        // this.updateById 对应的也是 users 表
        this.updateById(user);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!user.getPassword().equals(oldPassword)) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(newPassword);
        this.updateById(user);
    }
}