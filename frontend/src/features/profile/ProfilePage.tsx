import { useMutation } from "@tanstack/react-query";
import { FormEvent, useEffect, useState } from "react";
import { adminApi } from "../../api/admin";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { Field } from "../../components/ui/Field";
import { Input } from "../../components/ui/Input";
import { useToast } from "../../components/ui/Toast";
import { useAuth } from "../../store/auth";

export function ProfilePage() {
  const { user, username, refreshUser } = useAuth();
  const { notify } = useToast();
  const [form, setForm] = useState({
    username: username ?? "",
    password: "",
    realName: "",
    phone: "",
    mail: "",
  });

  useEffect(() => {
    if (user) {
      setForm((current) => ({
        ...current,
        username: user.username,
        realName: user.realName ?? "",
        phone: user.phone ?? "",
        mail: user.mail ?? "",
      }));
    }
  }, [user]);

  const updateMutation = useMutation({
    mutationFn: () =>
      adminApi.updateUser({
        username: form.username,
        password: form.password || undefined,
        realName: form.realName.trim() || undefined,
        phone: form.phone.includes("*") ? undefined : form.phone.trim() || undefined,
        mail: form.mail.trim() || undefined,
      }),
    onSuccess: async () => {
      await refreshUser();
      setForm((current) => ({ ...current, password: "" }));
      notify("资料已更新", "success");
    },
  });

  function update(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    updateMutation.mutate();
  }

  return (
    <div className="mx-auto max-w-3xl">
      <Card>
        <CardHeader>
          <CardTitle>个人设置</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4" onSubmit={submit}>
            <Field label="用户名">
              <Input value={form.username} disabled />
            </Field>
            <Field label="新密码">
              <Input
                type="password"
                value={form.password}
                onChange={(event) => update("password", event.target.value)}
              />
            </Field>
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="真实姓名">
                <Input value={form.realName} onChange={(event) => update("realName", event.target.value)} />
              </Field>
              <Field label="手机号">
                <Input value={form.phone} onChange={(event) => update("phone", event.target.value)} />
              </Field>
            </div>
            <Field label="邮箱">
              <Input value={form.mail} onChange={(event) => update("mail", event.target.value)} />
            </Field>
            {updateMutation.error ? (
              <p className="text-sm text-red-600">
                {updateMutation.error instanceof Error ? updateMutation.error.message : "更新失败"}
              </p>
            ) : null}
            <div>
              <Button type="submit" disabled={updateMutation.isPending}>
                保存
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
