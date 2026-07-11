#!/usr/bin/env python3
"""生成固定请求率阶梯压测的 README 图表与摘要。"""

from __future__ import annotations

import json
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd


RATES = {
    "create": [25, 50, 100, 200, 400, 500, 550],
    "redirect": [100, 250, 500, 750, 900, 1000, 1100],
}
RESULT_ROOT = Path("benchmark/results")
OUTPUT = RESULT_ROOT / "2026-07-11-native-rate"
BLUE = "#2563EB"
ORANGE = "#F59E0B"
SLATE = "#64748B"


def load_resources(path: Path) -> pd.DataFrame:
    rows = []
    for line in path.read_text().splitlines():
        parts = line.split(",")
        if len(parts) < 10:
            continue
        row = {}
        for offset in (1, 4, 7):
            name = parts[offset].removeprefix("shortlink-")
            row[f"{name}CpuPct"] = float(parts[offset + 1].rstrip("%"))
            memory = parts[offset + 2].split(" / ")[0]
            value, unit = float(memory[:-3]), memory[-3:]
            row[f"{name}MemoryMiB"] = value * 1024 if unit == "GiB" else value
        rows.append(row)
    return pd.DataFrame(rows)


def load_results(scenario: str) -> list[dict]:
    results = []
    for rate in RATES[scenario]:
        run = RESULT_ROOT / f"rate-{scenario}-{rate}"
        total = json.loads((run / "dashboard/statistics.json").read_text())["Total"]
        resources = load_resources(run / "resources.csv")
        results.append({
            "targetRps": rate,
            "actualRps": total["throughput"],
            "samples": total["sampleCount"],
            "meanMs": total["meanResTime"],
            "p95Ms": total["pct2ResTime"],
            "p99Ms": total["pct3ResTime"],
            "errorPct": total["errorPct"],
            "appCpuPeakPct": resources["appCpuPct"].max(),
            "appMemoryPeakMiB": resources["appMemoryMiB"].max(),
            "mysqlCpuPeakPct": resources["mysqlCpuPct"].max(),
            "redisCpuPeakPct": resources["redisCpuPct"].max(),
        })
    return results


def configure_plotting() -> None:
    plt.rcParams.update({
        "font.family": "sans-serif",
        "font.sans-serif": ["Arial Unicode MS", "PingFang SC", "DejaVu Sans"],
        "axes.edgecolor": "#CBD5E1",
        "axes.labelcolor": "#475569",
        "axes.titlecolor": "#0F172A",
        "xtick.color": "#64748B",
        "ytick.color": "#64748B",
        "grid.color": "#E2E8F0",
        "figure.facecolor": "white",
        "axes.facecolor": "white",
    })


def save_performance_chart(frames: dict[str, pd.DataFrame]) -> None:
    fig, axes = plt.subplots(2, 3, figsize=(15, 8.4))
    for row, scenario in enumerate(("create", "redirect")):
        frame = frames[scenario]
        limit = 500 if scenario == "create" else 1000
        axes[row, 0].plot(frame.targetRps, frame.targetRps, color=SLATE, linestyle="--", label="target")
        axes[row, 0].plot(frame.targetRps, frame.actualRps, color=BLUE, marker="o", linewidth=2, label="actual")
        axes[row, 0].set_title(f"{scenario.title()} · throughput")
        axes[row, 0].set_ylabel("requests / second")
        axes[row, 0].legend(frameon=False)

        axes[row, 1].plot(frame.targetRps, frame.p95Ms, color=BLUE, marker="o", linewidth=2, label="P95")
        axes[row, 1].plot(frame.targetRps, frame.p99Ms, color=ORANGE, marker="s", linewidth=2, label="P99")
        axes[row, 1].set_title(f"{scenario.title()} · tail latency")
        axes[row, 1].set_ylabel("milliseconds")
        axes[row, 1].legend(frameon=False)

        axes[row, 2].plot(frame.targetRps, frame.errorPct, color=ORANGE, marker="o", linewidth=2)
        axes[row, 2].axvline(limit, color=SLATE, linestyle="--", linewidth=1.5)
        axes[row, 2].set_title(f"{scenario.title()} · assertion errors")
        axes[row, 2].set_ylabel("percent")
        for axis in axes[row]:
            axis.set_xlabel("target requests / second")
            axis.grid(alpha=0.8)
    fig.suptitle("ShortLink · controlled request-rate staircase", fontsize=16, fontweight="bold", x=0.04, ha="left")
    fig.text(0.04, 0.94, "Native ARM JMeter 5.6.3 · 100 workers · 10s ramp-up · per-user cookies", color=SLATE)
    fig.tight_layout(rect=(0, 0, 1, 0.92))
    fig.savefig(OUTPUT / "rate-staircase.png", dpi=180, bbox_inches="tight")
    plt.close(fig)


def save_resource_chart(frames: dict[str, pd.DataFrame]) -> None:
    fig, axes = plt.subplots(2, 2, figsize=(12, 8.2))
    for row, scenario in enumerate(("create", "redirect")):
        frame = frames[scenario]
        axes[row, 0].plot(frame.targetRps, frame.appCpuPeakPct, color=BLUE, marker="o", label="app")
        axes[row, 0].plot(frame.targetRps, frame.mysqlCpuPeakPct, color=ORANGE, marker="s", label="mysql")
        axes[row, 0].plot(frame.targetRps, frame.redisCpuPeakPct, color=SLATE, marker="^", label="redis")
        axes[row, 0].set_title(f"{scenario.title()} · observed peak CPU")
        axes[row, 0].set_ylabel("% of one CPU core")
        axes[row, 0].legend(frameon=False)
        axes[row, 1].plot(frame.targetRps, frame.appMemoryPeakMiB, color=BLUE, marker="o")
        axes[row, 1].set_title(f"{scenario.title()} · application peak memory")
        axes[row, 1].set_ylabel("MiB")
        for axis in axes[row]:
            axis.set_xlabel("target requests / second")
            axis.grid(alpha=0.8)
    fig.suptitle("Container resource observations", fontsize=16, fontweight="bold", x=0.06, ha="left")
    fig.tight_layout(rect=(0, 0, 1, 0.92))
    fig.savefig(OUTPUT / "rate-resources.png", dpi=180, bbox_inches="tight")
    plt.close(fig)


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    results = {scenario: load_results(scenario) for scenario in RATES}
    frames = {scenario: pd.DataFrame(values) for scenario, values in results.items()}
    save_performance_chart(frames)
    save_resource_chart(frames)
    (OUTPUT / "summary.json").write_text(json.dumps(results, indent=2) + "\n")


if __name__ == "__main__":
    configure_plotting()
    main()
