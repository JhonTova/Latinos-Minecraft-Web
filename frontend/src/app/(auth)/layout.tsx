import Link from "next/link";

/** Marco de las pantallas de acceso: sin navegación, para no distraer del paso. */
export default function LayoutAuth({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <main className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center px-4 py-12">
      <Link
        href="/"
        className="mb-8 flex items-center justify-center gap-2 font-semibold"
      >
        <span className="grid size-7 place-items-center rounded-md bg-primary text-on-accent">
          <svg
            viewBox="0 0 24 24"
            fill="currentColor"
            className="size-4"
            aria-hidden
          >
            <path d="M12 2 3 7v10l9 5 9-5V7l-9-5Zm0 2.3 6.5 3.6v1.5L12 13 5.5 9.4V7.9L12 4.3Z" />
          </svg>
        </span>
        LatinosMC
      </Link>

      {children}
    </main>
  );
}
