package com.sdumagicode.backend.util;

import com.sdumagicode.backend.entity.chat.ValuationStandard;
import com.sdumagicode.backend.mapper.ValuationStandardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValuationStandardHolder implements ApplicationListener<ContextRefreshedEvent> {
    private static List<ValuationStandard> standardsList;

    @Autowired
    private ValuationStandardMapper valuationStandardMapper;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        standardsList = valuationStandardMapper.selectValuationList();
    }

    public static List<ValuationStandard> getStandards() {
        if (standardsList == null)
            System.out.println("null standardsList");
        return standardsList;
    }

    public void refreshStandards() {
        standardsList = valuationStandardMapper.selectValuationList();
    }
}