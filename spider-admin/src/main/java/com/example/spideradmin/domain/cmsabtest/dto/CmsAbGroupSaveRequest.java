package com.example.spideradmin.domain.cmsabtest.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 그룹 ID와 구성 페이지 목록을 함께 저장할 때 사용하는 요청 DTO. */
public class CmsAbGroupSaveRequest {

    private String groupId;

    private List<CmsAbPageWeightRequest> pages = new ArrayList<>();
}
