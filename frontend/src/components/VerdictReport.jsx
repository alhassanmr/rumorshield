const VERDICT_META = {
  VERIFIED: { label: 'Verified', className: 'verdict--verified', mark: '✓' },
  CONTRADICTED: { label: 'Contradicted', className: 'verdict--contradicted', mark: '✕' },
  OUTDATED: { label: 'Outdated', className: 'verdict--outdated', mark: '!' },
  UNVERIFIED: { label: 'Unverified', className: 'verdict--unverified', mark: '?' },
}

export default function VerdictReport({ result }) {
  const meta = VERDICT_META[result.verdict] || VERDICT_META.UNVERIFIED

  return (
    <article className={`report ${meta.className}`}>
      <header className="report__header">
        <span className="report__mark">{meta.mark}</span>
        <div>
          <div className="report__verdict">{meta.label}</div>
          <div className="report__meta">
            Evidence strength: {result.evidenceStrength} · Claim ID: {result.claimId.slice(0, 8)}
          </div>
        </div>
      </header>

      <p className="report__claim">“{result.originalText}”</p>

      <section className="report__section">
        <h3>What we found</h3>
        <p>{result.explanation}</p>
      </section>

      {result.sources && result.sources.length > 0 && (
        <section className="report__section">
          <h3>Sources</h3>
          <ul className="report__sources">
            {result.sources.map((s) => (
              <li key={s.url}>
                <span className={`report__source-tag report__source-tag--${s.type}`}>
                  {s.type}
                </span>
                <a href={s.url} target="_blank" rel="noreferrer">
                  {s.name}
                </a>
                {s.publishedDate && <span className="report__source-date">{s.publishedDate}</span>}
              </li>
            ))}
          </ul>
        </section>
      )}

      <footer className="report__recommendation">
        <strong>What to do:</strong> {result.recommendation}
      </footer>
    </article>
  )
}
