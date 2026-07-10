import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { adminApi } from "../../api/admin";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { Field } from "../../components/ui/Field";
import { Input } from "../../components/ui/Input";
import { useToast } from "../../components/ui/Toast";

export function RegisterPage() {
  const navigate = useNavigate();
  const { notify } = useToast();
  const [form, setForm] = useState({
    username: "",
    password: "",
    realName: "",
    phone: "",
    mail: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function update(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    if (!form.username.trim() || !form.password) {
      setError("请输入用户名和密码");
      return;
    }
    if (form.password.length < 8 || form.password.length > 72) {
      setError("密码长度应为 8-72 个字符");
      return;
    }
    setLoading(true);
    try {
      await adminApi.register({
        username: form.username.trim(),
        password: form.password,
        realName: form.realName.trim() || undefined,
        phone: form.phone.trim() || undefined,
        mail: form.mail.trim() || undefined,
      });
      notify("注册成功，请登录", "success");
      navigate("/login");
    } catch (err) {
      setError(err instanceof Error ? err.message : "注册失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="mx-auto flex min-h-[calc(100vh-144px)] max-w-lg items-center px-4 py-10">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>注册账号</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4" onSubmit={submit}>
            <Field label="用户名">
              <Input
                value={form.username}
                autoComplete="username"
                onChange={(event) => update("username", event.target.value)}
              />
            </Field>
            <Field label="密码">
              <Input
                type="password"
                value={form.password}
                autoComplete="new-password"
                onChange={(event) => update("password", event.target.value)}
              />
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="真实姓名">
                <Input
                  value={form.realName}
                  autoComplete="name"
                  onChange={(event) => update("realName", event.target.value)}
                />
              </Field>
              <Field label="手机号">
                <Input
                  value={form.phone}
                  autoComplete="tel"
                  onChange={(event) => update("phone", event.target.value)}
                />
              </Field>
            </div>
            <Field label="邮箱">
              <Input
                value={form.mail}
                autoComplete="email"
                onChange={(event) => update("mail", event.target.value)}
              />
            </Field>
            {error ? <p className="text-sm text-red-600">{error}</p> : null}
            <Button type="submit" disabled={loading}>
              {loading ? "注册中" : "注册"}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-slate-500">
            已有账号？{" "}
            <Link className="font-medium text-blue-600" to="/login">
              去登录
            </Link>
          </p>
        </CardContent>
      </Card>
    </section>
  );
}
