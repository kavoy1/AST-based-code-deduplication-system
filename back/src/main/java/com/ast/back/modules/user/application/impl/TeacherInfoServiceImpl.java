package com.ast.back.modules.user.application.impl;

import com.ast.back.modules.user.persistence.entity.TeacherInfo;
import com.ast.back.modules.user.persistence.mapper.TeacherInfoMapper;
import com.ast.back.modules.user.application.TeacherInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TeacherInfoServiceImpl extends ServiceImpl<TeacherInfoMapper, TeacherInfo> implements TeacherInfoService {
}
