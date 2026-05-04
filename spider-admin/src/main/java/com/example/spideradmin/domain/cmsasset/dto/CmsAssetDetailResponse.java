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
public class CmsAssetDetailResponse {

    private String assetId;
    private String assetName;
    private String businessCategory;
    private String businessCategoryName;
    private String mimeType;
    private Long fileSize;
    private String assetPath;
    private String assetUrl;
    private String assetDesc;
    private String assetState;
    private String useYn;
    private String rejectedReason;
    private String createUserId;
    private String createUserName;
    private String lastModifierName;
    private String createDate;
    private String lastModifiedDtime;
}
