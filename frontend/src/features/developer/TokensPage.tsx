import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyRound, Plus, Trash2 } from "lucide-react";
import { FormEvent, useState } from "react";
import { adminApi } from "../../api/admin";
import { endOfDay } from "../../lib/date";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { CopyButton } from "../../components/ui/CopyButton";
import { Dialog } from "../../components/ui/Dialog";
import { EmptyState } from "../../components/ui/EmptyState";
import { Field } from "../../components/ui/Field";
import { Input } from "../../components/ui/Input";
import { Textarea } from "../../components/ui/Textarea";
import { useToast } from "../../components/ui/Toast";

export function TokensPage() {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [form, setForm] = useState({ name: "", describe: "", validDate: "" });
  const [plainToken, setPlainToken] = useState("");
  const tokensQuery = useQuery({ queryKey: ["tokens"], queryFn: adminApi.getTokens });
  const tokens = tokensQuery.data ?? [];

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["tokens"] });
  const createMutation = useMutation({
    mutationFn: () =>
      adminApi.createToken({
        name: form.name.trim(),
        describe: form.describe.trim() || undefined,
        validDate: endOfDay(form.validDate) ?? null,
      }),
    onSuccess: (token) => {
      setPlainToken(token);
      setForm({ name: "", describe: "", validDate: "" });
      notify("Token 已创建", "success");
      void invalidate();
    },
  });
  const statusMutation = useMutation({
    mutationFn: ({ id, enable }: { id: number; enable: boolean }) => adminApi.updateTokenStatus(id, enable),
    onSuccess: () => void invalidate(),
  });
  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteToken,
    onSuccess: () => {
      notify("Token 已吊销", "success");
      void invalidate();
    },
  });

  function update(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!form.name.trim()) return;
    createMutation.mutate();
  }

  return (
    <div className="grid gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-950">API Token</h1>
        <p className="mt-1 text-sm text-slate-500">创建和管理 Core API 访问令牌</p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>创建 Token</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4" onSubmit={submit}>
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="名称">
                <Input value={form.name} onChange={(event) => update("name", event.target.value)} />
              </Field>
              <Field label="有效期">
                <Input type="date" value={form.validDate} onChange={(event) => update("validDate", event.target.value)} />
              </Field>
            </div>
            <Field label="描述">
              <Textarea value={form.describe} onChange={(event) => update("describe", event.target.value)} />
            </Field>
            {createMutation.error ? (
              <p className="text-sm text-red-600">
                {createMutation.error instanceof Error ? createMutation.error.message : "创建失败"}
              </p>
            ) : null}
            <div>
              <Button type="submit" disabled={createMutation.isPending}>
                <Plus className="h-4 w-4" />
                创建
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Token 列表</CardTitle>
        </CardHeader>
        <CardContent>
          {tokens.length ? (
            <div className="grid gap-3">
              {tokens.map((token) => (
                <div
                  key={token.id}
                  className="grid gap-3 rounded-lg border border-slate-200 p-4 md:grid-cols-[1fr_auto]"
                >
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <KeyRound className="h-4 w-4 text-blue-600" />
                      <span className="font-medium text-slate-950">{token.name}</span>
                      <span className="rounded bg-slate-100 px-2 py-1 text-xs text-slate-500">
                        {token.tokenMasked}
                      </span>
                    </div>
                    <p className="mt-2 text-sm text-slate-500">{token.describe || "未填写描述"}</p>
                    <p className="mt-1 text-xs text-slate-400">有效期：{token.validDate || "永久"}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <label className="flex items-center gap-2 text-sm text-slate-600">
                      <input
                        type="checkbox"
                        checked={token.enableStatus === 0}
                        onChange={(event) =>
                          statusMutation.mutate({ id: token.id, enable: event.target.checked })
                        }
                      />
                      启用
                    </label>
                    <Button variant="danger" onClick={() => deleteMutation.mutate(token.id)}>
                      <Trash2 className="h-4 w-4" />
                      吊销
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title={tokensQuery.isLoading ? "正在加载 Token" : "暂无 Token"} />
          )}
        </CardContent>
      </Card>
      <Dialog open={Boolean(plainToken)} title="Token 明文" onClose={() => setPlainToken("")}>
        <div className="grid gap-4">
          <div className="break-all rounded-md bg-slate-100 p-3 font-mono text-sm text-slate-900">
            {plainToken}
          </div>
          <CopyButton text={plainToken} />
        </div>
      </Dialog>
    </div>
  );
}
