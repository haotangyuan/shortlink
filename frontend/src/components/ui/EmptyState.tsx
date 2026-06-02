export function EmptyState({ title, action }: { title: string; action?: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-6 py-10 text-center">
      <p className="text-sm text-slate-500">{title}</p>
      {action ? <div className="mt-4">{action}</div> : null}
    </div>
  );
}
