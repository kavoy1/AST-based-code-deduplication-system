package com.ast.back.service.impl;

import com.ast.back.entity.Clazz;
import com.ast.back.entity.User;
import com.ast.back.mapper.ClassMapper;
import com.ast.back.service.ClassService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.ast.back.entity.ClassStudent;
import com.ast.back.entity.Notice;
import com.ast.back.entity.StudentInfo;
import com.ast.back.mapper.ClassStudentMapper;
import com.ast.back.mapper.UserMapper;
import com.ast.back.service.NoticeService;
import com.ast.back.service.StudentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Collections;

@Service
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Clazz> implements ClassService {

    @Autowired
    private ClassStudentMapper classStudentMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private StudentInfoService studentInfoService;

    @Override
    public List<Map<String, Object>> getStudentsByClassId(Integer classId, Long teacherId) {
        return baseMapper.selectStudentsByClassId(classId, teacherId);
    }

    @Override
    public boolean removeStudentFromClass(Integer classId, Long studentId, Long teacherId) {
        return baseMapper.deleteStudentFromClass(classId, studentId, teacherId) > 0;
    }

    @Override
    public boolean createNewClass(Clazz clazz) {
        // 1. 生成唯一邀请码
        String inviteCode = generateUniqueInviteCode();
        clazz.setInviteCode(inviteCode);
        
        // 2. 设置创建时间
        clazz.setCreateTime(LocalDateTime.now());
        
        // 3. 插入数据库
        return this.save(clazz);
    }

    @Override
    public IPage<Clazz> getClassesByTeacherId(Long teacherId, int page, int size) {
        Page<Clazz> p = new Page<>(page, size);
        return baseMapper.selectClassesWithStudentCount(p, teacherId);
    }

    @Override
    public Map<String, Object> getTeacherStats(Long teacherId) {
        // 1. 管理班级数
        long classCount = this.count(new LambdaQueryWrapper<Clazz>().eq(Clazz::getTeacherId, teacherId));

        // 2. 学生总数
        Long studentCount = baseMapper.countStudentsByTeacherId(teacherId);

        // 3. 发布作业 (暂无 Task 实体，模拟为 0)
        long homeworkCount = 0; 
        
        // 4. 高危预警 (暂无相关实体，模拟为 0)
        long warningCount = 0;
        
        // 5. 班级人数分布
        // 修正方案：使用一个新查询获取分布
        List<Map<String, Object>> distribution = baseMapper.selectClassStudentDistribution(teacherId);
        List<String> names = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        
        if (distribution != null) {
            for (Map<String, Object> item : distribution) {
                names.add((String) item.get("name"));
                counts.add((Long) item.get("student_count"));
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("classCount", classCount);
        stats.put("studentCount", studentCount != null ? studentCount : 0);
        stats.put("homeworkCount", homeworkCount);
        stats.put("warningCount", warningCount);
        stats.put("chartNames", names);
        stats.put("chartValues", counts);
        return stats;
    }

    @Override
    public List<Map<String, Object>> getPendingApplications(Long teacherId) {
        // 1. 获取老师的所有班级ID
        List<Clazz> classes = this.list(new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getTeacherId, teacherId)
                .select(Clazz::getId, Clazz::getClassName));
        
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Integer> classIds = classes.stream().map(Clazz::getId).collect(Collectors.toList());
        Map<Integer, String> classMap = classes.stream().collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));

        // 2. 查询待审批的申请 (status = 0)
        List<ClassStudent> applications = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .in(ClassStudent::getClassId, classIds)
                        .eq(ClassStudent::getStatus, 0)
        );
        
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 获取学生信息
        List<Long> studentIds = applications.stream().map(ClassStudent::getStudentId).distinct().collect(Collectors.toList());
        List<User> students = userMapper.selectBatchIds(studentIds);
        Map<Long, User> studentMap = students.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 获取详细信息
        List<StudentInfo> studentInfos = studentInfoService.list(new LambdaQueryWrapper<StudentInfo>().in(StudentInfo::getUserId, studentIds));
        Map<Long, StudentInfo> studentInfoMap = studentInfos.stream().collect(Collectors.toMap(StudentInfo::getUserId, s -> s));

        // 4. 组装数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassStudent app : applications) {
            User student = studentMap.get(app.getStudentId());
            if (student != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", app.getId()); // 申请记录ID
                map.put("studentId", student.getId());
                map.put("username", student.getUsername());
                map.put("nickname", student.getNickname());
                map.put("email", student.getEmail());
                map.put("role", student.getRole());
                
                // 填充学生详细信息
                StudentInfo info = studentInfoMap.get(student.getId());
                if (info != null) {
                    map.put("school", info.getSchool());
                    map.put("studentNumber", info.getStudentNumber());
                    map.put("college", info.getCollege());
                    map.put("adminClass", info.getClassName());
                } else {
                    map.put("school", "-");
                    map.put("studentNumber", "-");
                    map.put("college", "-");
                    map.put("adminClass", "-");
                }

                map.put("className", classMap.get(app.getClassId()));
                map.put("applyTime", app.getJoinTime());
                result.add(map);
            }
        }
        
        return result;
    }

    @Override
    public boolean approveApplication(Integer applicationId) {
        ClassStudent app = classStudentMapper.selectById(applicationId);
        if (app != null) {
            app.setStatus(1); // Approved
            int rows = classStudentMapper.updateById(app);
            if (rows > 0) {
                // 发送通知
                Clazz clazz = this.getById(app.getClassId());
                if (clazz != null) {
                    Notice notice = new Notice();
                    notice.setTitle("加入班级成功");
                    notice.setContent("恭喜您，您已成功加入班级：" + clazz.getClassName());
                    notice.setReceiverId(app.getStudentId());
                    notice.setIsRead(0);
                    notice.setType("SYSTEM");
                    notice.setCreateTime(LocalDateTime.now());
                    noticeService.save(notice);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean rejectApplication(Integer applicationId) {
        ClassStudent app = classStudentMapper.selectById(applicationId);
        if (app != null) {
            Clazz clazz = this.getById(app.getClassId());
            int rows = classStudentMapper.deleteById(applicationId);
            if (rows > 0) {
                // 发送通知
                if (clazz != null) {
                    Notice notice = new Notice();
                    notice.setTitle("加入班级申请被拒绝");
                    notice.setContent("很遗憾，您申请加入班级：" + clazz.getClassName() + " 的请求被拒绝。");
                    notice.setReceiverId(app.getStudentId());
                    notice.setIsRead(0);
                    notice.setType("SYSTEM");
                    notice.setCreateTime(LocalDateTime.now());
                    noticeService.save(notice);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteClass(Integer id) {
        // 可以在这里添加额外的逻辑，比如检查班级下是否有学生，或者级联删除学生关系
        // 目前简单实现为直接删除班级
        return this.removeById(id);
    }

    private String generateUniqueInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String code;
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();

            // 检查数据库中是否已存在该邀请码
            Long count = baseMapper.selectCount(new QueryWrapper<Clazz>().eq("invite_code", code));
            if (count == 0) break; // 不存在则跳出循环，可以使用
        }
        return code;
    }

    @Override
    public boolean inviteStudentToClass(Integer classId, Long studentId) {
        // 1. 检查是否已经在班级中
        ClassStudent exist = classStudentMapper.selectOne(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));
        
        if (exist != null) {
            if (exist.getStatus() == 0) {
                // 如果是申请中，直接改为已加入
                exist.setStatus(1);
                return classStudentMapper.updateById(exist) > 0;
            }
            return true; // 已经是成员
        }
        
        // 2. 创建新记录
        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(studentId);
        cs.setJoinTime(LocalDateTime.now());
        cs.setStatus(1); // 直接置为已加入
        
        return classStudentMapper.insert(cs) > 0;
    }

    @Override
    public List<Map<String, Object>> getStudentClasses(Long studentId) {
        // 查询学生加入的所有班级（包括审核中和已通过的）
        List<ClassStudent> relations = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getStudentId, studentId)
        );

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> classIds = relations.stream().map(ClassStudent::getClassId).collect(Collectors.toList());
        List<Clazz> classes = this.listByIds(classIds);
        Map<Integer, Clazz> classMap = classes.stream().collect(Collectors.toMap(Clazz::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassStudent relation : relations) {
            Clazz clazz = classMap.get(relation.getClassId());
            if (clazz != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", clazz.getId());
                map.put("className", clazz.getClassName());
                map.put("courseName", clazz.getCourseName());
                map.put("teacherId", clazz.getTeacherId());
                // 获取老师名字
                User teacher = userMapper.selectById(clazz.getTeacherId());
                map.put("teacherName", teacher != null ? (teacher.getNickname() != null ? teacher.getNickname() : teacher.getUsername()) : "未知");
                map.put("joinTime", relation.getJoinTime());
                map.put("status", relation.getStatus()); // 0:审核中, 1:已加入
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public String joinClass(Long studentId, String inviteCode) {
        // 1. 校验邀请码
        Clazz clazz = this.getOne(new LambdaQueryWrapper<Clazz>().eq(Clazz::getInviteCode, inviteCode));
        if (clazz == null) {
            return "邀请码无效";
        }

        // 2. 检查是否已经加入或申请过
        Long count = classStudentMapper.selectCount(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, clazz.getId())
                        .eq(ClassStudent::getStudentId, studentId)
        );
        
        if (count > 0) {
            return "您已在该班级或正在审核中";
        }

        // 3. 创建申请记录
        ClassStudent relation = new ClassStudent();
        relation.setClassId(clazz.getId());
        relation.setStudentId(studentId);
        relation.setJoinTime(LocalDateTime.now());
        relation.setStatus(0); // 0: 待审核
        
        classStudentMapper.insert(relation);
        
        return "success";
    }
}
