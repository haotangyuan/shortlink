import { Copy } from "lucide-react";
import { Button } from "./Button";
import { useToast } from "./Toast";

export function CopyButton({ text, label = "复制" }: { text: string; label?: string }) {
  const { notify } = useToast();

  async function copy() {
    await navigator.clipboard.writeText(text);
    notify("已复制到剪贴板", "success");
  }

  return (
    <Button variant="secondary" onClick={copy}>
      <Copy className="h-4 w-4" />
      {label}
    </Button>
  );
}
