export function formatDateInput(date: Date) {
  return date.toISOString().slice(0, 10);
}

export function formatTimestamp(ts?: number | string | null): string {
  if (ts == null) return "-";
  const num = typeof ts === "string" ? Number(ts) : ts;
  if (num <= 0) return "-";
  const d = new Date(num);
  if (isNaN(d.getTime())) return "-";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function endOfDay(value: string) {
  return value ? `${value} 23:59:59` : undefined;
}

export function startOfDay(value: string) {
  return `${value} 00:00:00`;
}

export function dateRangeByDays(days: number) {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - (days - 1));
  return {
    startDate: startOfDay(formatDateInput(start)),
    endDate: endOfDay(formatDateInput(end)) ?? "",
  };
}

export function todayRange() {
  return dateRangeByDays(1);
}
