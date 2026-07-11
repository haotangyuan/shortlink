#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-redirect}"
SHORT_URI="${SHORT_URI:-}"
THREADS="${THREADS:-200}"
DURATION_SECONDS="${DURATION_SECONDS:-120}"
RAMP_SECONDS="${RAMP_SECONDS:-30}"
TARGET_RPS="${TARGET_RPS:-0}"
TARGET_RPM=1000000000000
[[ "${TARGET_RPS}" -gt 0 ]] && TARGET_RPM=$((TARGET_RPS * 60))
HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-8068}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)-${MODE}}"
RESULT_DIR="benchmark/results/${RUN_ID}"

if [[ "${MODE}" != "create" && "${MODE}" != "redirect" ]]; then
  echo "用法: $0 create|redirect" >&2
  exit 2
fi
if [[ "${MODE}" == "redirect" && -z "${SHORT_URI}" ]]; then
  echo "redirect 模式必须设置 SHORT_URI，例如 SHORT_URI=abc123 $0 redirect" >&2
  exit 2
fi
if ! curl --silent --output /dev/null "http://${HOST}:${PORT}/"; then
  echo "目标服务 http://${HOST}:${PORT} 不可访问，请先启动应用。" >&2
  exit 1
fi

mkdir -p "${RESULT_DIR}"
CREATE_THREADS=0
REDIRECT_THREADS=0
[[ "${MODE}" == "create" ]] && CREATE_THREADS="${THREADS}"
[[ "${MODE}" == "redirect" ]] && REDIRECT_THREADS="${THREADS}"

JMETER_ARGS=(
  -n -t benchmark/BenchmarkPlan.jmx
  -l "${RESULT_DIR}/samples.jtl"
  -e -o "${RESULT_DIR}/dashboard"
  -Jhost="${HOST}" -Jport="${PORT}"
  -Jhost.header="${HOST}:${PORT}"
  -Jcreate.threads="${CREATE_THREADS}" -Jredirect.threads="${REDIRECT_THREADS}"
  -Jduration.seconds="${DURATION_SECONDS}" -Jramp.seconds="${RAMP_SECONDS}"
  -Jtarget.rpm="${TARGET_RPM}"
  -Jshort.uri="${SHORT_URI}"
  -Jgid="${GID:-public}" -Jauth.header="${AUTH_HEADER:-}"
)

if command -v jmeter >/dev/null 2>&1; then
  jmeter "${JMETER_ARGS[@]}"
elif command -v docker >/dev/null 2>&1; then
  DOCKER_HOST_NAME="${HOST}"
  if [[ "${HOST}" == "127.0.0.1" || "${HOST}" == "localhost" ]]; then
    DOCKER_HOST_NAME="host.docker.internal"
  fi
  docker run --rm -v "${PWD}:/work" -w /work justb4/jmeter:5.5 \
    "${JMETER_ARGS[@]}" -Jhost="${DOCKER_HOST_NAME}"
else
  echo "未找到 JMeter 或 Docker，无法执行压测。" >&2
  exit 1
fi

if [[ ! -s "${RESULT_DIR}/samples.jtl" || ! -f "${RESULT_DIR}/dashboard/index.html" ]]; then
  echo "JMeter 未生成有效样本或 HTML 报告，压测失败：${RESULT_DIR}" >&2
  exit 1
fi

echo "压测完成：${RESULT_DIR}/dashboard/index.html"
