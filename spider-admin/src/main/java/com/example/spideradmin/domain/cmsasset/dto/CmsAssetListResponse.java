package com.example.spideradmin.domain.cmsasset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsAssetListResponse {

    private String assetId;
    private String assetName;
    private String businessCategory;
    private String businessCategoryName;
    private String mimeType;
    private Long fileSize;
    private String assetUrl;
    private String assetState;
    private String useYn;
    private String createUserId;
    private String createUserName;
    private String rejectedReason;
    private String createDate;
    private String lastModifiedDtime;
}
