package com.achobeta.themis.domain.laws.service.impl;

import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.repo.ILawCategoryRepository;
import com.achobeta.themis.domain.laws.service.ILawCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawCategoryService implements ILawCategoryService {

    private final ILawCategoryRepository lawCategoryRepository;

    @Override
    public List<LawCategory> queryLawCategoryList() {
        return lawCategoryRepository.listLawCategories();
    }
}
