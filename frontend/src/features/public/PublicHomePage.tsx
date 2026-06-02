import { ExternalLink, Link as LinkIcon, ShieldCheck, Zap } from "lucide-react";
import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { adminApi } from "../../api/admin";
import type { LinkCreateVO } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { Card, CardContent } from "../../components/ui/Card";
import { CopyButton } from "../../components/ui/CopyButton";
import { Field } from "../../components/ui/Field";
import { Input } from "../../components/ui/Input";
import { Textarea } from "../../components/ui/Textarea";

export function PublicHomePage() {
  const [originUrl, setOriginUrl] = useState("");
  const [describe, setDescribe] = useState("");
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);
  const [result, setResult] = useState<LinkCreateVO | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    if (!originUrl.trim()) {
      setError("请输入原始链接");
      return;
    }
    setCreating(true);
    try {
      setResult(await adminApi.createPublicLink(originUrl.trim(), describe.trim() || undefined));
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建失败");
    } finally {
      setCreating(false);
    }
  }

  return (
    <div>
      <section className="bg-white">
        <div className="mx-auto grid max-w-6xl gap-8 px-4 py-12 lg:grid-cols-[1.1fr_0.9fr] lg:py-16">
          <div className="flex flex-col justify-center">
            <h1 className="text-4xl font-semibold tracking-normal text-slate-950 sm:text-5xl">
              ShortLink
            </h1>
            <p className="mt-4 max-w-2xl text-lg leading-8 text-slate-600">
              面向团队和开发者的短链接管理平台，支持创建、分组、回收站、访问统计和 API Token。
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <a href="#create">
                <Button>
                  <LinkIcon className="h-4 w-4" />
                  创建短链接
                </Button>
              </a>
              <Link to="/login">
                <Button variant="secondary">进入控制台</Button>
              </Link>
            </div>
          </div>
          <div className="grid gap-3 sm:grid-cols-3 lg:grid-cols-1">
            {[
              { icon: Zap, title: "快速创建", text: "公开入口和登录后台都能创建短链接。" },
              { icon: ShieldCheck, title: "后台管理", text: "分组、回收站和 Token 管理对接现有接口。" },
              { icon: ExternalLink, title: "数据洞察", text: "按链接或分组查看 PV、UV、UIP 和来源数据。" },
            ].map((item) => (
              <Card key={item.title}>
                <CardContent className="flex gap-3">
                  <item.icon className="mt-1 h-5 w-5 text-blue-600" />
                  <div>
                    <div className="font-medium text-slate-950">{item.title}</div>
                    <p className="mt-1 text-sm leading-6 text-slate-500">{item.text}</p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      <section id="create" className="mx-auto max-w-4xl px-4 py-10">
        <Card>
          <CardContent>
            <form className="grid gap-4" onSubmit={submit}>
              <Field label="原始链接" error={error}>
                <Input
                  value={originUrl}
                  onChange={(event) => setOriginUrl(event.target.value)}
                  placeholder="https://example.com/article"
                />
              </Field>
              <Field label="描述">
                <Textarea
                  value={describe}
                  onChange={(event) => setDescribe(event.target.value)}
                  placeholder="可选，用于标记这个链接"
                />
              </Field>
              <div>
                <Button type="submit" disabled={creating}>
                  {creating ? "创建中" : "生成短链接"}
                </Button>
              </div>
            </form>
            {result ? (
              <div className="mt-5 rounded-lg border border-emerald-200 bg-emerald-50 p-4">
                <div className="text-sm font-medium text-emerald-950">创建成功</div>
                <div className="mt-2 break-all text-base text-emerald-900">{result.fullShortUrl}</div>
                <div className="mt-4 flex flex-wrap gap-2">
                  <CopyButton text={result.fullShortUrl} />
                  <a href={result.fullShortUrl} target="_blank" rel="noreferrer">
                    <Button variant="secondary">
                      <ExternalLink className="h-4 w-4" />
                      打开
                    </Button>
                  </a>
                </div>
              </div>
            ) : null}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
