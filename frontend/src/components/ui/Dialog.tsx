import { X } from "lucide-react";
import { cn } from "../../lib/cn";
import { Button } from "./Button";

type DialogProps = {
  open: boolean;
  title: string;
  children: React.ReactNode;
  onClose: () => void;
  className?: string;
};

export function Dialog({ open, title, children, onClose, className }: DialogProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4">
      <div
        role="dialog"
        aria-modal="true"
        className={cn("w-full max-w-lg rounded-lg bg-white shadow-xl", className)}
      >
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <h2 className="text-base font-semibold text-slate-950">{title}</h2>
          <Button variant="ghost" className="h-8 w-8 px-0" onClick={onClose} aria-label="关闭">
            <X className="h-4 w-4" />
          </Button>
        </div>
        <div className="px-5 py-4">{children}</div>
      </div>
    </div>
  );
}
