package com.usststore.item.service;

import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.item.mapper.SpecGroupMapper;
import com.usststore.item.mapper.SpecParamMapper;
import com.usststore.item.mapper.SpecificationMapper;
import com.usststore.item.pojo.SpecGroup;
import com.usststore.item.pojo.SpecParam;
import com.usststore.item.pojo.Specification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;
    @Autowired
    private SpecParamMapper specParamMapper;

    /*public Specification queryById(Long id) {

        Specification specification = specificationMapper.selectByPrimaryKey(id);

        if (specification==null){
            throw new UsException(ExceptionEnum.SPECIFICATION_NOT_FOUND);
        }

        return specification;

    }*/

    public List<SpecGroup> queryGroupsByCid(Long cid) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        List<SpecGroup> specGroups = specGroupMapper.select(specGroup);
        if (CollectionUtils.isEmpty(specGroups)){
            throw new UsException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return specGroups;
    }

    public List<SpecParam> queryParamsList(Long gid, Long cid, Boolean searching) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setSearching(searching);
        List<SpecParam> paramList = specParamMapper.select(specParam);
        if (CollectionUtils.isEmpty(paramList)){
            throw new UsException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        return paramList;
    }

    public List<SpecGroup> queryGroupsAndParamsByCid(Long cid) {
        //查规格组
        List<SpecGroup> specGroups = queryGroupsByCid(cid);
        //查当前分类下的参数
        List<SpecParam> specParams = queryParamsList(null, cid, null);
        //把规格参数变为map，key为规格组的id，值为组下的所有参数
        Map<Long,List<SpecParam>> paramsMap=new HashMap<>();
        for (SpecParam specParam : specParams) {
            if (!paramsMap.containsKey(specParam.getGroupId())){
                paramsMap.put(specParam.getGroupId(),new ArrayList<>());
            }
            paramsMap.get(specParam.getGroupId()).add(specParam);
        }
        //填充param到group中
        for (SpecGroup specGroup : specGroups) {
            specGroup.setParams(paramsMap.get(specGroup.getId()));
        }
        return specGroups;
    }
}
