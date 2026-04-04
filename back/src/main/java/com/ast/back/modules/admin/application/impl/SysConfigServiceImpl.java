package com.ast.back.modules.admin.application.impl;

import com.ast.back.modules.admin.persistence.entity.SysConfig;
import com.ast.back.modules.admin.persistence.mapper.SysConfigMapper;
import com.ast.back.modules.admin.application.SysConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {
}

