package com.example.admin_demo.domain.reactcmsadmindeployment.service;

import com.example.admin_demo.domain.cmsdeployment.config.CmsDeployProperties;
import com.example.admin_demo.domain.reactcmsadmindeployment.config.ReactDeployLocalProperties;
import com.example.admin_demo.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployHistoryRequest;
import com.example.admin_demo.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployHistoryResponse;
import com.example.admin_demo.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployPageRequest;
import com.example.admin_demo.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployPageResponse;
import com.example.admin_demo.domain.reactcmsadmindeployment.mapper.ReactCmsAdminDeployMapper;
import com.example.admin_demo.global.dto.PageRequest;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.exception.InternalException;
import com.example.admin_demo.global.exception.NotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** React CMS Admin 배포 관리 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReactCmsAdminDeployService {

    private final ReactCmsAdminDeployMapper reactCmsAdminDeployMapper;
    private final CmsDeployProperties deployProperties;
    private final ReactDeployLocalProperties localProperties;

    /** 배포 대상 페이지 목록 조회 (PAGE_TYPE='REACT', APPROVE_STATE='APPROVED') */
    public PageResponse<ReactCmsAdminDeployPageResponse> findApprovedPageList(
            ReactCmsAdminDeployPageRequest req, PageRequest pageRequest) {
        long total = reactCmsAdminDeployMapper.countApprovedPageList(req);
        List<ReactCmsAdminDeployPageResponse> list =
                reactCmsAdminDeployMapper.findApprovedPageList(req, pageRequest.getOffset(), pageRequest.getEndRow());

        // 로컬 TypeScript 배포는 Vite 뷰어 경로, 그 외는 기존 HTML 정적 파일 경로 사용
        String protocol = deployProperties.getDeployedProtocol();
        String pathPrefix = deployProperties.getDeployedPathPrefix();
        String localInstanceId = localProperties.getInstanceId();
        list.forEach(item -> {
            if (item.getInstanceIp() == null || item.getInstancePort() == null) return;
            if (localInstanceId.equals(item.getInstanceId())) {
                // 로컬 TypeScript 배포: demo/front Vite 뷰어 라우트로 연결
                item.setDeployedUrl("http://" + item.getInstanceIp() + ":" + item.getInstancePort()
                        + "/react-cms/viewer/" + item.getPageId());
            } else {
                item.setDeployedUrl(protocol + "://" + item.getInstanceIp() + ":" + item.getInstancePort()
                        + pathPrefix + "/" + item.getPageId() + ".html");
            }
        });

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /** 배포 이력 목록 조회 (모달용, pageId 필터) */
    public PageResponse<ReactCmsAdminDeployHistoryResponse> findHistoryList(
            ReactCmsAdminDeployHistoryRequest req, PageRequest pageRequest) {
        long total = reactCmsAdminDeployMapper.countHistoryList(req);
        List<ReactCmsAdminDeployHistoryResponse> list =
                reactCmsAdminDeployMapper.findHistoryList(req, pageRequest.getOffset(), pageRequest.getEndRow());
        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /**
     * 배포 실행 — PAGE_DESC(사전 생성된 JSX)를 로컬 파일로 저장하고 FWK_CMS_FILE_SEND_HIS에 이력을 기록한다.
     *
     * <p>PAGE_TYPE='REACT', APPROVE_STATE='APPROVED' 사전 검증 후
     * COMPONENT_DIR/{pageId}.tsx, CONTAINER_DIR/{pageId}.tsx 두 파일을 생성한다.</p>
     */
    @Transactional
    public void push(String pageId, String userId) {
        if (reactCmsAdminDeployMapper.existsApprovedPage(pageId) == 0) {
            throw new NotFoundException("승인된 React 페이지를 찾을 수 없습니다. pageId=" + pageId);
        }

        String pageDesc = reactCmsAdminDeployMapper.findPageDescById(pageId);
        if (pageDesc == null || pageDesc.isBlank()) {
            throw new InternalException("배포할 코드가 없습니다. CMS 빌더에서 페이지를 저장한 뒤 다시 시도해주세요.");
        }

        long fileSize = writeLocalFiles(pageId, pageDesc);

        int nextVersion = reactCmsAdminDeployMapper.findMaxDeployVersion(pageId) + 1;
        String fileId = pageId + "_v" + nextVersion + ".tsx";
        String fileCrcValue = computeSha256Prefix(pageDesc);

        reactCmsAdminDeployMapper.insertDeployHistory(
                localProperties.getInstanceId(), fileId, fileSize, fileCrcValue, userId);

        log.info("React CMS 로컬 배포 완료: pageId={}, userId={}, fileId={}", pageId, userId, fileId);
    }

    /** 컴포넌트 파일과 컨테이너 파일을 로컬 디렉토리에 저장하고 컴포넌트 파일 크기(bytes)를 반환 */
    private long writeLocalFiles(String pageId, String pageDesc) {
        try {
            // 컴포넌트 파일: PAGE_DESC(generateJSX 결과) 그대로 저장
            Path componentDir = resolveDir(localProperties.getComponentDir());
            byte[] componentBytes = pageDesc.getBytes(StandardCharsets.UTF_8);
            Files.write(componentDir.resolve(pageId + ".tsx"), componentBytes);

            // 컨테이너 파일: demo/front 라우팅을 위한 re-export 래퍼
            Path containerDir = resolveDir(localProperties.getContainerDir());
            String containerCode = "// Auto-generated container for " + pageId + " — do not edit manually\n"
                    + "export { default } from \"@/reactcms/generated/" + pageId + "\";\n";
            Files.writeString(containerDir.resolve(pageId + ".tsx"), containerCode, StandardCharsets.UTF_8);

            return componentBytes.length;
        } catch (IOException e) {
            throw new InternalException("배포 파일 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /** 파일 내용의 SHA-256 해시 앞 16자리를 반환 (FILE_CRC_VALUE 용) */
    private String computeSha256Prefix(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "0000000000000000";
        }
    }

    /** 경로를 절대 경로로 변환하고 디렉토리가 없으면 생성 */
    private Path resolveDir(String dirPath) throws IOException {
        Path path = Paths.get(dirPath).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }
}
