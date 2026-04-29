package com.example.spider_admin.domain.bizgroup.mapper;

import com.example.spider_admin.domain.bizgroup.dto.BizGroupResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BizGroupMapper {

    List<BizGroupResponse> findAll();
}
