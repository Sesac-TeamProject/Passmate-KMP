#!/bin/sh
# 실기기 테스트용 백엔드 주소를 맥의 현재 LAN IP로 맞춘다.
# 교육장 Wi-Fi는 DHCP가 IP를 자주 갈아치우므로, 네트워크가 바뀌면 이걸 다시 돌린다.
#
#   sh scripts/set-dev-server-host.sh            # 현재 IP로 맞춘다
#   sh scripts/set-dev-server-host.sh 안드로이드  # 인자로 호스트:포트를 직접 줄 수도 있다
#
# 고치는 파일 2개는 전부 gitignore 대상이라 추적 파일은 건드리지 않는다.
#   iosApp/Configuration/Local.xcconfig  (iOS)
#   local.properties                     (안드로이드)
# 반영하려면 양쪽 다 다시 빌드·설치해야 한다 — 주소는 빌드 시점에 구워진다.

set -e
cd "$(dirname "$0")/.."

PORT=8080
HOST="$1"

if [ -z "$HOST" ]; then
    IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)

    if [ -z "$IP" ]; then
        echo "LAN IP를 찾지 못했다. Wi-Fi에 붙어 있는지 확인하고, 안 되면 인자로 직접 넘겨라." >&2
        exit 1
    fi
    HOST="$IP:$PORT"
fi

printf '// 이 파일은 gitignore 대상이다. 기기 테스트용 로컬 설정만 둔다.\nPASSMATE_SERVER_HOST=%s\n' "$HOST" > iosApp/Configuration/Local.xcconfig

if grep -q '^PASSMATE_SERVER_HOST=' local.properties 2>/dev/null; then
    sed -i '' "s|^PASSMATE_SERVER_HOST=.*|PASSMATE_SERVER_HOST=$HOST|" local.properties
else
    printf '\n# 실기기 테스트용 백엔드 주소(호스트:포트). 비우면 에뮬레이터 기본값 10.0.2.2:8080\nPASSMATE_SERVER_HOST=%s\n' "$HOST" >> local.properties
fi

echo "서버 주소를 $HOST 로 맞췄다 (iOS Local.xcconfig · 안드로이드 local.properties)"
printf '백엔드 응답 확인: '

if curl -s -o /dev/null -w '%{http_code}\n' --max-time 3 "http://$HOST/v3/api-docs" | grep -q 200; then
    echo "200 — 맥에서는 닿는다. 폰 브라우저로 http://$HOST/v3/api-docs 도 열리는지 확인해라."
else
    echo "실패 — 백엔드가 떠 있는지 먼저 확인해라."
fi

echo "양쪽 앱을 다시 빌드·설치해야 반영된다."
