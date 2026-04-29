import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { pageRoutes, modalRoutes } from '@/routes'
import { AuthProvider, useAuth } from '@/contexts/AuthContext'
import { useSessionActivity } from '@/hooks/useSessionActivity'
import { PATHS } from '@/constants/paths'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime:            1000 * 60 * 5,
      retry:                2,
      refetchOnWindowFocus: false,
    },
  },
})

/**
 * Route 기반 모달 패턴.
 *
 * navigate('/card/menu', { state: { background: location } }) 로 이동하면
 * - background 위치에 기존 페이지를 유지한 채
 * - modalRoutes 에 등록된 컴포넌트(ModalSlideOver 포함)를 오버레이로 렌더링한다.
 *
 * 새 모달이 필요하면 routes.tsx 의 modalRoutes 에만 추가하면 된다.
 */
function AppRoutes() {
  const location   = useLocation()
  const background = (location.state as { background?: Location })?.background
  const { isLoggedIn } = useAuth()

  // 클릭 이벤트 쓰로틀링 + 비활성 타임아웃으로 세션 만료 감지
  useSessionActivity()

  // 로그인 안 된 상태에서 로그인 페이지 및 공개 경로 외 접근 시 리다이렉트
  // /preview/* 는 Admin iframe 미리보기용 공개 경로이므로 인증 불필요
  // /react/viewer/* 는 승인된 React 컴포넌트 뷰어로 인증 없이 접근 가능
  const isPublicPath =
    location.pathname === PATHS.LOGIN ||
    location.pathname.startsWith('/preview') ||
    location.pathname.startsWith('/react/viewer') ||
    location.pathname.startsWith('/react-cms/viewer')
  if (!isLoggedIn && !isPublicPath) {
    return <Navigate to={PATHS.LOGIN} replace />
  }

  return (
    <>
      <Routes location={background ?? location}>
        <Route path="/" element={<Navigate to="/login" replace />} />
        {pageRoutes.map(({ path, element }) => (
          <Route key={path} path={path} element={element} />
        ))}
      </Routes>

      {background && (
        <Routes>
          {modalRoutes.map(({ path, element }) => (
            <Route key={path} path={path} element={element} />
          ))}
        </Routes>
      )}
    </>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}

export default App
