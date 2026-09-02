#!/usr/bin/env bash
# Cloudflare 엣지 IP 대역을 받아 nginx 스니펫 2개를 생성한다.
#
#   /etc/nginx/snippets/cloudflare-realip.conf : set_real_ip_from — 진짜 IP 복원
#   /etc/nginx/conf.d/cloudflare-geo.conf      : $is_cloudflare — 엣지 판정
#
# geo 지시어는 http 컨텍스트에만 올 수 있어 conf.d 로 나눠 넣는다.
# (nginx.conf 가 conf.d/*.conf 를 http 블록 안에서 include 한다)
#
# Cloudflare가 대역을 추가하는 경우가 있으므로 분기에 한 번쯤 돌린다.
#   sudo ./refresh-cloudflare-ips.sh && sudo nginx -t && sudo nginx -s reload
set -euo pipefail

SNIPPET_DIR="${1:-/etc/nginx/snippets}"
CONFD_DIR="${2:-/etc/nginx/conf.d}"
V4=$(curl -fsS --max-time 15 https://www.cloudflare.com/ips-v4)
V6=$(curl -fsS --max-time 15 https://www.cloudflare.com/ips-v6)

if [ -z "$V4" ] || [ -z "$V6" ]; then
    echo "Cloudflare IP 목록을 받지 못했습니다. 기존 파일을 유지합니다." >&2
    exit 1
fi

mkdir -p "$SNIPPET_DIR" "$CONFD_DIR"

{
    echo "# 자동 생성됨 - refresh-cloudflare-ips.sh ($(date -u +%Y-%m-%dT%H:%M:%SZ))"
    echo "# 직접 수정하지 말 것."
    while read -r cidr; do
        [ -n "$cidr" ] && echo "set_real_ip_from $cidr;"
    done <<< "$V4"
    while read -r cidr; do
        [ -n "$cidr" ] && echo "set_real_ip_from $cidr;"
    done <<< "$V6"
    echo "real_ip_header CF-Connecting-IP;"
} > "$SNIPPET_DIR/cloudflare-realip.conf"

{
    echo "# 자동 생성됨 - refresh-cloudflare-ips.sh ($(date -u +%Y-%m-%dT%H:%M:%SZ))"
    echo "#"
    echo "# realip 모듈이 \$remote_addr 를 실제 클라이언트 IP로 치환하므로"
    echo "# allow/deny 로는 엣지 판정을 할 수 없다."
    echo "# 치환 전 주소인 \$realip_remote_addr 를 기준으로 본다."
    echo "geo \$realip_remote_addr \$is_cloudflare {"
    echo "    default 0;"
    while read -r cidr; do
        [ -n "$cidr" ] && echo "    $cidr 1;"
    done <<< "$V4"
    while read -r cidr; do
        [ -n "$cidr" ] && echo "    $cidr 1;"
    done <<< "$V6"
    echo "}"
} > "$CONFD_DIR/cloudflare-geo.conf"

echo "생성 완료:"
echo "  $SNIPPET_DIR/cloudflare-realip.conf"
echo "  $CONFD_DIR/cloudflare-geo.conf"
