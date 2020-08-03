package com.usststore.item.web;

import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.item.pojo.SpecGroup;
import com.usststore.item.pojo.SpecParam;
import com.usststore.item.pojo.Specification;
import com.usststore.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    private SpecificationService specificationService;

    /**
     * 根据分类id查询规格组
     * @param cid
     * @return
     */
    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> queryGroupsByCid(@PathVariable("cid")Long cid){
        List<SpecGroup> groups=specificationService.queryGroupsByCid(cid);
        return ResponseEntity.ok(groups);
    }

    /**
     * 查询SpecParam集合
     * @param gid
     * @param cid
     * @return
     */
    @GetMapping("params")
    public ResponseEntity<List<SpecParam>> queryParamsList(
            @RequestParam(value = "gid",required = false)Long gid,
            @RequestParam(value = "cid",required = false)Long cid,
            @RequestParam(value = "searching",required = false)Boolean searching){
        return ResponseEntity.ok(specificationService.queryParamsList(gid,cid,searching));
    }

    /**
     * 根据商品分类查询规格组及组内参数
     * @param cid
     * @return
     */
    @GetMapping("group")
    public  ResponseEntity<List<SpecGroup>> queryGroupsAndParamsByCid(@RequestParam("cid") Long cid){
        return ResponseEntity.ok(specificationService.queryGroupsAndParamsByCid(cid));
    }
}
