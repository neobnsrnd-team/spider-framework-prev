/**
 * @file src/app/maintenance/page.tsx
 * @description 긴급차단(IS_PUBLIC='N') 시 리다이렉트되는 서비스 점검 안내 페이지.
 *              middleware.ts 에서 /deployed/*.html 접근 차단 시 이 페이지로 redirect 된다.
 */

export default function MaintenancePage() {
    return (
        <html lang="ko">
            <head>
                <meta charSet="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>서비스 일시 중단</title>
                <style>{`
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background: #f5f6f8;
                        font-family: 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
                    }
                    .card {
                        text-align: center;
                        padding: 52px 40px;
                        background: #fff;
                        border-radius: 16px;
                        box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
                        max-width: 420px;
                        width: 90%;
                    }
                    .icon {
                        font-size: 52px;
                        margin-bottom: 20px;
                    }
                    h1 {
                        font-size: 20px;
                        font-weight: 700;
                        color: #1a1a1a;
                        margin-bottom: 14px;
                        letter-spacing: -0.3px;
                    }
                    p {
                        font-size: 14px;
                        color: #666;
                        line-height: 1.8;
                    }
                `}</style>
            </head>
            <body>
                <div className="card">
                    <div className="icon">🔧</div>
                    <h1>서비스 일시 중단 안내</h1>
                    <p>
                        현재 서비스 점검 중입니다.
                        <br />
                        잠시 후 다시 이용해 주세요.
                    </p>
                </div>
            </body>
        </html>
    );
}
