-- =============================================================
-- Remove deprecated CMS user dashboard menu
-- =============================================================
-- Keeps the /cms/dashboard route and dashboard APIs available.
-- Removes only the sidebar/menu metadata for v3_cms_user_dashboard.

DELETE FROM FWK_USER_MENU
WHERE MENU_ID = 'v3_cms_user_dashboard';

DELETE FROM FWK_ROLE_MENU
WHERE MENU_ID = 'v3_cms_user_dashboard';

DELETE FROM FWK_MENU
WHERE MENU_ID = 'v3_cms_user_dashboard';

COMMIT;
