#!/usr/bin/env bash
set -euo pipefail

api_base="${API_BASE_URL:-http://127.0.0.1:8080/api}"
admin_user="${SMOKE_USERNAME:-admin}"
: "${SMOKE_PASSWORD:=admin123}"
response_file="$(mktemp)"
upload_dir="$(mktemp -d)"
upload_file="$upload_dir/smoke.txt"
image_file="$upload_dir/smoke.png"
token=""
text_file_id=""
image_file_id=""

cleanup() {
    if [[ -n "$token" ]]; then
        [[ -z "$text_file_id" ]] || curl -sS -X DELETE -H "Authorization: Bearer $token" "$api_base/files/$text_file_id" >/dev/null || true
        [[ -z "$image_file_id" ]] || curl -sS -X DELETE -H "Authorization: Bearer $token" "$api_base/files/$image_file_id" >/dev/null || true
        curl -sS -X POST -H "Authorization: Bearer $token" "$api_base/auth/logout" >/dev/null || true
    fi
    rm -f "$response_file" "$upload_file" "$image_file"
    rmdir "$upload_dir"
}
trap cleanup EXIT
printf 'Alpha Vue smoke test\n' > "$upload_file"
printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=' | base64 -d > "$image_file"

request() {
    local step="$1"
    local expected="$2"
    shift 2
    local actual
    actual="$(curl -sS -o "$response_file" -w '%{http_code}' "$@")"
    if [[ "$actual" != "$expected" ]]; then
        printf '%s: expected HTTP %s but received %s\n' "$step" "$expected" "$actual" >&2
        jq '{code, message, traceId}' "$response_file" >&2 2>/dev/null || true
        exit 1
    fi
}

request_image() {
    local url="$1"
    local result
    result="$(curl -sS -o "$response_file" -w '%{http_code} %{content_type}' "$url")"
    if [[ "$result" != "200 image/png" ]]; then
        printf 'image-preview: expected HTTP 200 image/png but received %s\n' "$result" >&2
        exit 1
    fi
}

request health 200 "${api_base%/api}/actuator/health"
jq -e '.status == "UP"' "$response_file" >/dev/null
request unauthorized-profile 401 "$api_base/auth/profile"

request captcha 200 "$api_base/auth/captcha"
if [[ "$(jq -r '.data.enabled' "$response_file")" == "true" ]]; then
    printf 'Smoke test requires CAPTCHA_ENABLED=false for non-interactive login.\n' >&2
    exit 1
fi

request login 200 -H 'Content-Type: application/json' -d "$(jq -nc --arg u "$admin_user" --arg p "$SMOKE_PASSWORD" '{username:$u,password:$p,clientId:"pc-admin"}')" "$api_base/auth/login"
token="$(jq -er '.data.token' "$response_file")"
auth_header="Authorization: Bearer $token"

request profile 200 -H "$auth_header" "$api_base/auth/profile"
jq -e --arg u "$admin_user" '.data.username == $u' "$response_file" >/dev/null
request users 200 -H "$auth_header" "$api_base/system/users?page=1&size=10"
jq -e '.data.records | type == "array"' "$response_file" >/dev/null
request upload 200 -H "$auth_header" -F "file=@${upload_file};type=text/plain" "$api_base/files/upload"
text_file_id="$(jq -er '.data.id' "$response_file")"
request image-upload 200 -H "$auth_header" -F "file=@${image_file};type=image/png" "$api_base/files/upload"
image_file_id="$(jq -er '.data.id' "$response_file")"
image_public_url="$(jq -er '.data.publicUrl' "$response_file")"
if [[ "$image_public_url" == /* ]]; then
    image_public_url="${api_base%/api}${image_public_url}"
fi
request_image "$image_public_url"

request file-list-after-upload 200 -H "$auth_header" "$api_base/files?page=1&size=100"
jq -e --arg textId "$text_file_id" --arg imageId "$image_file_id" '
    [.data.records[].id | tostring] as $ids
    | ($ids | index($textId)) != null and ($ids | index($imageId)) != null
' "$response_file" >/dev/null

deleted_text_id="$text_file_id"
deleted_image_id="$image_file_id"
request delete-text 200 -X DELETE -H "$auth_header" "$api_base/files/$text_file_id"
text_file_id=""
request delete-image 200 -X DELETE -H "$auth_header" "$api_base/files/$image_file_id"
image_file_id=""
request file-list-after-delete 200 -H "$auth_header" "$api_base/files?page=1&size=100"
jq -e --arg textId "$deleted_text_id" --arg imageId "$deleted_image_id" '
    [.data.records[].id | tostring] as $ids
    | ($ids | index($textId)) == null and ($ids | index($imageId)) == null
' "$response_file" >/dev/null
request logout 200 -X POST -H "$auth_header" "$api_base/auth/logout"
request expired-profile 401 -H "$auth_header" "$api_base/auth/profile"
token=""

printf 'Alpha Vue smoke test passed.\n'
