/**
 * @file cms-asset-category.ts
 * @description
 *   이미지 자산 비즈니스 카테고리 관련 상수.
 *   카테고리 코드/라벨 자체는 FWK_CODE 테이블의 CODE_GROUP_ID='CMS00001' 행을 단일 출처로 한다.
 *   본 파일에는 그룹 ID 와 업로드 기본값(코드값)만 정의한다.
 */

/** FWK_CODE 의 카테고리 코드 그룹 ID */
export const CMS_ASSET_CATEGORY_GROUP_ID = 'CMS00001';

/**
 * 업로드 시 카테고리를 지정하지 않으면 사용할 기본 코드값.
 * 이 코드값(COMMON)은 FWK_CODE 에 항상 존재하는 것을 전제로 한다.
 */
export const CMS_ASSET_DEFAULT_CATEGORY = 'COMMON';
