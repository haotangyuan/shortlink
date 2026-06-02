import { useMutation, useQuery } from "@tanstack/react-query";
import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { adminApi } from "../../api/admin";
import { endOfDay, formatDateInput } from "../../lib/date";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { Field } from "../../components/ui/Field";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { useToast } from "../../components/ui/Toast";

function defaultValidDate() {
  const date = new Date();
  date.setDate(date.getDate() + 3);
  return formatDateInput(date);
}

export function LinkCreatePage() {
  const navigate = useNavigate();
  const { notify } = useToast();
  const groupsQuery = useQuery({ queryKey: ["groups"], queryFn: adminApi.getGroups });
  const [form, setForm] = useState({ originUrl: "", gid: "", describe: "", validDate: defaultValidDate() });
  const [error, setError] = useState("");

  useEffect(() => {
    if (!form.gid && groupsQuery.data?.[0]?.gid) {
      setForm((current) => ({ ...current, gid: groupsQuery.data[0].gid }));
    }
  }, [form.gid, groupsQuery.data]);

  const createMutation = useMutation({
    mutationFn: () =>
      adminApi.createLink({
        originUrl: form.originUrl.trim(),
        gid: form.gid,
        describe: form.describe.trim() || undefined,
        createdType: 1,
        validDateType: 1,
        validDate: endOfDay(form.validDate),
      }),
    onSuccess: (result) => {
      notify("短链接已创建", "success");
      navigate(`/dashboard/links?gid=${encodeURIComponent(result.gid)}`);
    },
  });

  function update(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    if (!form.originUrl.trim() || !form.gid || !form.validDate) {
      setError("请填写原始链接、分组和有效期");
      return;
    }
    createMutation.mutate();
  }

  return (
    <div className="mx-auto max-w-3xl">
      <Card>
        <CardHeader>
          <CardTitle>创建短链接</CardTitle>
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
            {error || createMutation.error ? (
              <p className="text-sm text-red-600">
                {error || (createMutation.error instanceof Error ? createMutation.error.message : "创建失败")}
              </p>
            ) : null}
            <div className="flex gap-2">
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "创建中" : "创建"}
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
