#!/bin/sh
# 앱이 붙을 백엔드 주소를 맞춘다. 인자가 없으면 맥의 현재 LAN IP(로컬 백엔드)로 간다.
# 교육장 Wi-Fi는 DHCP가 IP를 자주 갈아치우므로, 네트워크가 바뀌면 이걸 다시 돌린다.
#
#   sh scripts/set-dev-server-host.sh            # 로컬 백엔드 = 맥의 현재 IP:8080
#   sh scripts/set-dev-server-host.sh 운영        # 운영 서버 = api.passmate.kr
#   sh scripts/set-dev-server-host.sh 10.0.2.2:8080   # 호스트[:포트]를 직접 줄 수도 있다
#
# 스킴은 적지 않는다 — 앱이 호스트 종류로 정한다(로컬·사설망 http/ws, 그 밖 https/wss).
# 고치는 파일 2개는 전부 gitignore 대상이라 추적 파일은 건드리지 않는다.
#   iosApp/Configuration/Local.xcconfig  (iOS)
#   local.properties                     (안드로이드 · Desktop)
# 반영하려면 다시 빌드·설치해야 한다 — 주소는 빌드 시점에 구워진다.

set -e
cd "$(dirname "$0")/.."

PORT=8080
PROD_HOST=api.passmate.kr
HOST="$1"

if [ "$HOST" = "운영" ] || [ "$HOST" = "prod" ]; then
    HOST="$PROD_HOST"
elif [ -z "$HOST" ]; then
    IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)

    if [ -z "$IP" ]; then
        echo "LAN IP를 찾지 못했다. Wi-Fi에 붙어 있는지 확인하고, 안 되면 인자로 직접 넘겨라." >&2
        exit 1
    fi
    HOST="$IP:$PORT"
fi

# 앱의 isLocalDevHost와 같은 기준이다. 여기가 어긋나면 확인 요청만 엉뚱한 스킴으로 날아간다
case "${HOST%%:*}" in
    localhost|127.0.0.1|10.0.2.2|10.*|192.168.*|172.1[6-9].*|172.2[0-9].*|172.3[01].*)
        SCHEME=http ;;
    *)
        SCHEME=https ;;
esac

# Local.xcconfig에는 서명 팀(DEVELOPMENT_TEAM_ID)도 들어 있다. 통째로 덮어쓰면 그게 날아가
# iOS 빌드가 서명에서 깨지므로, 주소 줄만 갈아끼우고 나머지는 그대로 둔다
XCCONFIG=iosApp/Configuration/Local.xcconfig

if [ -f "$XCCONFIG" ] && grep -q '^PASSMATE_SERVER_HOST=' "$XCCONFIG"; then
    sed -i '' "s|^PASSMATE_SERVER_HOST=.*|PASSMATE_SERVER_HOST=$HOST|" "$XCCONFIG"
elif [ -f "$XCCONFIG" ]; then
    printf 'PASSMATE_SERVER_HOST=%s\n' "$HOST" >> "$XCCONFIG"
else
    printf '// 이 파일은 gitignore 대상이다. 맥마다 다른 설정만 둔다.\nPASSMATE_SERVER_HOST=%s\n' "$HOST" > "$XCCONFIG"
fi

if grep -q '^PASSMATE_SERVER_HOST=' local.properties 2>/dev/null; then
    sed -i '' "s|^PASSMATE_SERVER_HOST=.*|PASSMATE_SERVER_HOST=$HOST|" local.properties
else
    printf '\n# 백엔드 주소(호스트[:포트]). 비우면 안드로이드는 에뮬레이터 기본값 10.0.2.2:8080, Desktop은 localhost:8080\nPASSMATE_SERVER_HOST=%s\n' "$HOST" >> local.properties
fi

echo "서버 주소를 $SCHEME://$HOST 로 맞췄다 (iOS Local.xcconfig · 안드로이드/Desktop local.properties)"
printf '백엔드 응답 확인: '

if curl -s -o /dev/null -w '%{http_code}\n' --max-time 8 "$SCHEME://$HOST/v3/api-docs" | grep -q 200; then
    echo "200 — 맥에서는 닿는다. 폰에서도 $SCHEME://$HOST/v3/api-docs 가 열리는지 확인해라."
else
    echo "실패 — 서버가 떠 있는지 먼저 확인해라."
fi

echo "양쪽 앱을 다시 빌드·설치해야 반영된다."
