-- ============================================================================
-- CMS A/B test columns for SPW_CMS_PAGE
-- ============================================================================

ALTER TABLE SPW_CMS_PAGE ADD (
    AB_GROUP_ID VARCHAR2(64) DEFAULT NULL,
    AB_WEIGHT   NUMBER(5, 2) DEFAULT NULL
);

COMMENT ON COLUMN SPW_CMS_PAGE.AB_GROUP_ID IS 'A/B test group identifier';
COMMENT ON COLUMN SPW_CMS_PAGE.AB_WEIGHT IS 'A/B test exposure weight';
