package com.example.dividend.scraper;

import com.example.dividend.model.Company;
import com.example.dividend.model.ScrapResult;

public interface Scraper {
    Company scrapCompanyByTicker(String ticker);
    ScrapResult scrap(Company company);
}
