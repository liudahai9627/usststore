package com.usststore.search.web;

import com.usststore.common.vo.PageResult;
import com.usststore.search.pojo.Goods;
import com.usststore.search.pojo.SearchRequest;
import com.usststore.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 依据searchRequest搜索商品
     * @param searchRequest
     * @return
     */
    @PostMapping("page")
    public ResponseEntity<PageResult<Goods>> search(@RequestBody SearchRequest searchRequest){
        System.out.println("========================================");
        System.out.println(searchRequest);
        System.out.println("========================================");
        return ResponseEntity.ok(searchService.search(searchRequest));
    }
}
