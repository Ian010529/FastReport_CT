interface FeedbackBannerProps {
  tone: "error" | "warning" | "success" | "info";
  title: string;
  message: string;
  details?: string[];
}

const TONE_STYLES: Record<FeedbackBannerProps["tone"], string> = {
  error: "border-rose-200 bg-rose-50 text-rose-900",
  warning: "border-amber-200 bg-amber-50 text-amber-900",
  success: "border-emerald-200 bg-emerald-50 text-emerald-900",
  info: "border-sky-200 bg-sky-50 text-sky-900",
};

export function FeedbackBanner({
  tone,
  title,
  message,
  details = [],
}: FeedbackBannerProps) {
  return (
    <div className={`rounded-2xl border px-4 py-3 text-sm ${TONE_STYLES[tone]}`}>
      <div className="font-semibold">{title}</div>
      <div className="mt-1 leading-6">{message}</div>
      {details.length > 0 && (
        <ul className="mt-2 list-disc space-y-1 pl-5">
          {details.map((detail) => (
            <li key={detail}>{detail}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
