package com.usststore.item.api;

import com.usststore.item.pojo.SpecGroup;
import com.usststore.item.pojo.SpecParam;
import com.usststore.item.pojo.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface SpecificationApi {

    @GetMapping("spec/params")
    List<SpecParam> queryParamsList(
            @RequestParam(value = "gid",required = false)Long gid,
            @RequestParam(value = "cid",required = false)Long cid,
            @RequestParam(value = "searching",required = false)Boolean searching);

    @GetMapping("spec/group")
    List<SpecGroup> queryGroupsAndParamsByCid(@RequestParam("cid") Long cid);
}
