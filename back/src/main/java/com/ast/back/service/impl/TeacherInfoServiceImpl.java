package com.ast.back.service.impl;

import com.ast.back.entity.TeacherInfo;
import com.ast.back.mapper.TeacherInfoMapper;
import com.ast.back.service.TeacherInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TeacherInfoServiceImpl extends ServiceImpl<TeacherInfoMapper, TeacherInfo> implements TeacherInfoService {
}
