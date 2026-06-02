import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { adminApi } from "../../api/admin";
import { endOfDay } from "../../lib/date";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Field } from "../../components/ui/Field";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { useToast } from "../../components/ui/Toast";

export function LinkEditPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [searchParams] = useSearchParams();
  const gid = searchParams.get("gid") ?? "";
  const fullShortUrl = searchParams.get("fullShortUrl") ?? "";
  const groupsQuery = useQuery({ queryKey: ["groups"], queryFn: adminApi.getGroups });
  const linksQuery = useQuery({
    queryKey: ["links", gid, "edit"],
    enabled: Boolean(gid),
    queryFn: () => adminApi.getLinks(gid, 1, 100),
  });
  const link = linksQuery.data?.records.find((item) => item.fullShortUrl === fullShortUrl);
  const [form, setForm] = useState({ originUrl: "", gid: "", describe: "", validDate: "" });

  useEffect(() => {
    if (link) {
      setForm({
        originUrl: link.originUrl,
        gid: link.gid,
        describe: link.describe ?? "",
        validDate: link.validDate?.slice(0, 10) ?? "",
      });
    }
  }, [link]);

  const updateMutation = useMutation({
    mutationFn: () =>
      adminApi.updateLink({
        originUrl: form.originUrl.trim(),
        fullShortUrl,
        originGid: gid,
        gid: form.gid,
        validDateType: 1,
        validDate: endOfDay(form.validDate),
        describe: form.describe.trim() || undefined,
      }),
    onSuccess: () => {
      notify("短链接已更新", "success");
      void queryClient.invalidateQueries({ queryKey: ["links"] });
      navigate(`/dashboard/links?gid=${encodeURIComponent(form.gid)}`);
    },
  });

  function update(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    updateMutation.mutate();
  }

  if (!gid || !fullShortUrl) {
    return <EmptyState title="缺少短链接参数" />;
  }

  if (!link && !linksQuery.isLoading) {
    return <EmptyState title="未找到短链接" />;
  }

  return (
    <div className="mx-auto max-w-3xl">
      <Card>
        <CardHeader>
          <CardTitle>编辑短链接</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4" onSubmit={submit}>
            <Field label="原始链接">
              <Input value={form.originUrl} onChange={(event) => update("originUrl", event.target.value)} />
            </Field>
            <Field label="分组">
              <Select value={form.gid} onChange={(event) => update("gid", event.target.value)}>
                {groupsQuery.data?.map((group) => (
                  <option key={group.gid} value={group.gid}>
                    {group.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="有效期">
              <Input type="date" value={form.validDate} onChange={(event) => update("validDate", event.target.value)} />
            </Field>
            <Field label="描述">
              <Textarea value={form.describe} onChange={(event) => update("describe", event.target.value)} />
            </Field>
            {updateMutation.error ? (
              <p className="text-sm text-red-600">
                {updateMutation.error instanceof Error ? updateMutation.error.message : "更新失败"}
              </p>
            ) : null}
            <div className="flex gap-2">
              <Button type="submit" disabled={updateMutation.isPending}>
                保存
              </Button>
              <Button variant="secondary" onClick={() => navigate("/dashboard/links")}>
                返回
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
