package com.achobeta.themis.domain.laws.service.impl;

import com.achobeta.themis.domain.laws.repo.ILawDataImportRepository;
import com.achobeta.themis.domain.laws.service.ILawDataImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawDataImportServiceImpl implements ILawDataImportService {
    
    private final ILawDataImportRepository lawDataImportRepository;
    
    @Override
    public int importLawDataFromDatabase() throws Exception {
        return lawDataImportRepository.importLawDataFromDatabase();
    }
}
