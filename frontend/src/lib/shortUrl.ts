export function toAbsoluteShortUrl(shortUrl: string) {
  return /^https?:\/\//i.test(shortUrl) ? shortUrl : `http://${shortUrl}`;
}
