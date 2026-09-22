package com.jn.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.trade.entity.MfgResistor;
import com.jn.trade.mapper.MfgResistorMapper;
import com.jn.trade.service.MfgResistorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MfgResistorServiceImpl extends ServiceImpl<MfgResistorMapper, MfgResistor> implements MfgResistorService {
}
