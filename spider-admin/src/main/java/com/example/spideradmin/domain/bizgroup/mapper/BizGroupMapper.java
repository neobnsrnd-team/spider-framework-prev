package com.example.spideradmin.domain.bizgroup.mapper;

import com.example.spideradmin.domain.bizgroup.dto.BizGroupResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BizGroupMapper {

    List<BizGroupResponse> findAll();
}
