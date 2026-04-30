package com.example.spideradmin.domain.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuUpdateRequest {

    private String priorMenuId;

    @NotNull(message = "정렬순서는 필수입니다")
    @Min(value = 0, message = "정렬순서는 0 이상이어야 합니다")
    @Max(value = 999, message = "정렬순서는 999 이하여야 합니다")
    private Integer sortOrder;

    @NotBlank(message = "메뉴명은 필수입니다")
    @Size(max = 100, message = "메뉴명은 100자 이내여야 합니다")
    private String menuName;

    @NotBlank(message = "메뉴URL은 필수입니다")
    @Size(max = 200, message = "메뉴URL은 200자 이내여야 합니다")
    private String menuUrl;

    @Size(max = 50, message = "메뉴이미지는 50자 이내여야 합니다")
    private String menuImage;

    @NotBlank(message = "표시여부는 필수입니다")
    @Size(max = 1, message = "표시여부는 1자여야 합니다")
    private String displayYn;

    @NotBlank(message = "사용여부는 필수입니다")
    @Size(max = 1, message = "사용여부는 1자여야 합니다")
    private String useYn;

    @Size(max = 70, message = "웹앱ID는 70자 이내여야 합니다")
    private String webAppId;
}
