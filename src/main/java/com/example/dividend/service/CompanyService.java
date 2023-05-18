package com.example.dividend.service;

import com.example.dividend.exception.impl.NoCompanyException;
import com.example.dividend.model.Company;
import com.example.dividend.model.ScrapResult;
import com.example.dividend.persist.CompanyRepository;
import com.example.dividend.persist.DividendRepository;
import com.example.dividend.persist.entity.CompanyEntity;
import com.example.dividend.persist.entity.DividendEntity;
import com.example.dividend.scraper.Scraper;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class CompanyService {

    private Trie trie;
    private final Scraper yahooFinanceScraper;
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;


    public Company save(String ticker) {
        boolean exists = this.companyRepository.existsByTicker(ticker);
        if (exists) {
            throw new RuntimeException("already exists ticker -> " + ticker);
        }
        return this.storeCompanyAndDividend(ticker);
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return this.companyRepository.findAll(pageable);
    }

    private Company storeCompanyAndDividend(String ticker) {
        //ticker 를 기준으로 회사를 스크래핑
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);
        if (ObjectUtils.isEmpty(company)) {
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }

        //해당회사가 존재 할 경우, 회사의 배당금 정보를 스크래핑
        ScrapResult scrapResult = this.yahooFinanceScraper.scrap(company);

        //스크래핑 결과
        CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntityList = scrapResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());

        this.dividendRepository.saveAll(dividendEntityList);

        return company;
    }

    public List<String> getCompantNamesByKeyword(String keyword) {
        Pageable limit = PageRequest.of(0, 10);
        Page<CompanyEntity> companyEntities = this.companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);
        return companyEntities.stream()
                .map(e -> e.getName())
                .collect(Collectors.toList());
    }

    //추가하기
    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    //조회하기
    public List<String> autocomplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream() //.limit(10) 자동완성 수량 제한 걸기
                .collect(Collectors.toList());
    }

    private void deleteAutocompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        var company = this.companyRepository.findByTicker(ticker)
                .orElseThrow(NoCompanyException::new);


        this.dividendRepository.deleteAllByCompanyId(company.getId());
        this.companyRepository.delete(company);


        this.deleteAutocompleteKeyword(company.getName());
        return null;
    }
}
