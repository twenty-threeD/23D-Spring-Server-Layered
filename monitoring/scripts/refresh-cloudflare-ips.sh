#!/usr/bin/env bash
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
    while read -r cidr; do
        [ -n "$cidr" ] && echo "set_real_ip_from $cidr;"
    done <<< "$V4"
    while read -r cidr; do
        [ -n "$cidr" ] && echo "set_real_ip_from $cidr;"
    done <<< "$V6"
    echo "real_ip_header CF-Connecting-IP;"
} > "$SNIPPET_DIR/cloudflare-realip.conf"

{
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
