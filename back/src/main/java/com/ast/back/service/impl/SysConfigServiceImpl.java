package com.ast.back.service.impl;

import com.ast.back.entity.SysConfig;
import com.ast.back.mapper.SysConfigMapper;
import com.ast.back.service.SysConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {
}

