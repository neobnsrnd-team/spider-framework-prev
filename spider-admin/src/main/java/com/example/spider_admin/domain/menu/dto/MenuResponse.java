package com.example.spider_admin.domain.menu.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuResponse {

    private String menuId;
    private String priorMenuId;
    private Integer sortOrder;
    private String menuName;
    private String menuUrl;
    private String menuImage;
    private String displayYn;
    private String useYn;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String webAppId;

    // 트리 구조를 위한 필드
    private String priorMenuName;
    private Integer level;
    private List<MenuResponse> children = new ArrayList<>();
    private Boolean hasChildren;

    private String tableType;
}
