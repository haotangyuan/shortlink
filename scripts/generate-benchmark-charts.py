#!/usr/bin/env python3
"""从 JMeter 与 docker stats 结果生成 README 使用的静态图表。"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd


COLORS = {"create": "#2563EB", "redirect": "#F59E0B"}
LABELS = {"create": "Create · 600 concurrent", "redirect": "Redirect · 1000 concurrent"}


def load_jmeter(run_dir: Path) -> tuple[pd.DataFrame, dict]:
    samples = pd.read_csv(run_dir / "samples.jtl")
    samples["time"] = pd.to_datetime(samples["timeStamp"], unit="ms", utc=True)
    statistics = json.loads((run_dir / "dashboard/statistics.json").read_text())["Total"]
    return samples, statistics


def load_resources(path: Path) -> pd.DataFrame:
    rows = []
    for line in path.read_text().splitlines():
        parts = line.split(",")
        if len(parts) < 10:
            continue
        row = {"time": pd.to_datetime(parts[0], utc=True)}
        for offset in (1, 4, 7):
            name = parts[offset].removeprefix("shortlink-")
            row[f"{name}_cpu"] = float(parts[offset + 1].rstrip("%"))
            memory = parts[offset + 2].split(" / ")[0]
            value, unit = float(memory[:-3]), memory[-3:]
            row[f"{name}_memory_mib"] = value * 1024 if unit == "GiB" else value
        rows.append(row)
    return pd.DataFrame(rows).set_index("time").sort_index()


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


def save_summary(stats: dict[str, dict], output: Path) -> None:
    fig, axes = plt.subplots(1, 2, figsize=(12, 4.8))
    scenarios = ["create", "redirect"]
    labels = [LABELS[key] for key in scenarios]

    throughput = [stats[key]["throughput"] for key in scenarios]
    axes[0].barh(labels, throughput, color=[COLORS[key] for key in scenarios], height=0.48)
    axes[0].set_title("Throughput")
    axes[0].set_xlabel("requests / second")
    axes[0].grid(axis="x", alpha=0.8)
    for index, value in enumerate(throughput):
        axes[0].text(value + max(throughput) * 0.02, index, f"{value:.1f}", va="center", color="#0F172A")

    p95 = [stats[key]["pct2ResTime"] / 1000 for key in scenarios]
    p99 = [stats[key]["pct3ResTime"] / 1000 for key in scenarios]
    positions = range(len(scenarios))
    axes[1].barh([p - 0.16 for p in positions], p95, height=0.28, color="#2563EB", label="P95")
    axes[1].barh([p + 0.16 for p in positions], p99, height=0.28, color="#F59E0B", label="P99")
    axes[1].set_yticks(list(positions), labels)
    axes[1].set_title("Tail latency")
    axes[1].set_xlabel("seconds")
    axes[1].grid(axis="x", alpha=0.8)
    axes[1].legend(frameon=False, loc="lower right")
    for values, delta in ((p95, -0.16), (p99, 0.16)):
        for index, value in enumerate(values):
            axes[1].text(value + max(p99) * 0.015, index + delta, f"{value:.2f}s", va="center", fontsize=9)

    fig.suptitle("ShortLink load-test summary", fontsize=16, fontweight="bold", x=0.04, ha="left")
    fig.text(0.04, 0.91, "Single app instance · 30s ramp-up · 60s duration · 0% assertion errors", color="#64748B")
    fig.tight_layout(rect=(0, 0, 1, 0.88))
    fig.savefig(output / "benchmark-summary.png", dpi=180, bbox_inches="tight")
    plt.close(fig)


def save_timeseries(samples_by_scenario: dict[str, pd.DataFrame], output: Path) -> None:
    fig, axes = plt.subplots(2, 1, figsize=(12, 7.2), sharex=False)
    for scenario, samples in samples_by_scenario.items():
        series = samples.set_index("time")["elapsed"].resample("2s")
        throughput = series.count() / 2
        p95 = series.quantile(0.95) / 1000
        elapsed_seconds = (throughput.index - throughput.index[0]).total_seconds()
        axes[0].plot(elapsed_seconds, throughput, color=COLORS[scenario], linewidth=2, label=LABELS[scenario])
        axes[1].plot(elapsed_seconds, p95, color=COLORS[scenario], linewidth=2, label=LABELS[scenario])

    axes[0].set_title("Observed throughput over time")
    axes[0].set_ylabel("requests / second")
    axes[1].set_title("P95 response time over time")
    axes[1].set_ylabel("seconds")
    axes[1].set_xlabel("seconds since first completed request")
    for axis in axes:
        axis.grid(alpha=0.8)
        axis.legend(frameon=False, loc="upper left")
    fig.suptitle("ShortLink load-test timeline", fontsize=16, fontweight="bold", x=0.06, ha="left")
    fig.tight_layout(rect=(0, 0, 1, 0.95))
    fig.savefig(output / "benchmark-timeline.png", dpi=180, bbox_inches="tight")
    plt.close(fig)


def save_resources(resources: dict[str, pd.DataFrame], output: Path) -> dict[str, dict[str, float]]:
    fig, axes = plt.subplots(2, 2, figsize=(12, 7.2), sharex=False)
    peaks = {}
    for column, scenario in enumerate(("create", "redirect")):
        frame = resources[scenario]
        seconds = (frame.index - frame.index[0]).total_seconds()
        for component, color in (("app", "#2563EB"), ("mysql", "#F59E0B"), ("redis", "#64748B")):
            axes[0, column].plot(seconds, frame[f"{component}_cpu"], label=component, color=color, linewidth=1.8)
            axes[1, column].plot(seconds, frame[f"{component}_memory_mib"], label=component, color=color, linewidth=1.8)
        axes[0, column].set_title(f"{LABELS[scenario]} · CPU")
        axes[0, column].set_ylabel("% of one CPU core")
        axes[1, column].set_title(f"{LABELS[scenario]} · memory")
        axes[1, column].set_ylabel("MiB")
        axes[1, column].set_xlabel("seconds")
        peaks[scenario] = {
            "appCpuPct": float(frame["app_cpu"].max()),
            "appMemoryMiB": float(frame["app_memory_mib"].max()),
            "mysqlCpuPct": float(frame["mysql_cpu"].max()),
            "redisCpuPct": float(frame["redis_cpu"].max()),
        }
    for axis in axes.flat:
        axis.grid(alpha=0.8)
        axis.legend(frameon=False, loc="upper right")
    fig.suptitle("Container resource observations", fontsize=16, fontweight="bold", x=0.06, ha="left")
    fig.tight_layout(rect=(0, 0, 1, 0.95))
    fig.savefig(output / "benchmark-resources.png", dpi=180, bbox_inches="tight")
    plt.close(fig)
    return peaks


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--create-run", type=Path, required=True)
    parser.add_argument("--redirect-run", type=Path, required=True)
    parser.add_argument("--create-resources", type=Path, required=True)
    parser.add_argument("--redirect-resources", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    create_samples, create_stats = load_jmeter(args.create_run)
    redirect_samples, redirect_stats = load_jmeter(args.redirect_run)
    stats = {"create": create_stats, "redirect": redirect_stats}
    resources = {
        "create": load_resources(args.create_resources),
        "redirect": load_resources(args.redirect_resources),
    }
    save_summary(stats, args.output)
    save_timeseries({"create": create_samples, "redirect": redirect_samples}, args.output)
    peaks = save_resources(resources, args.output)
    summary = {"jmeter": stats, "resourcePeaks": peaks}
    (args.output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")


if __name__ == "__main__":
    configure_plotting()
    main()
