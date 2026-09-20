import { useState } from 'react'
import ClaimForm from './components/ClaimForm.jsx'
import VerdictReport from './components/VerdictReport.jsx'
import { verifyClaim } from './api.js'

export default function App() {
  const [result, setResult] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(claimText) {
    setIsLoading(true)
    setError(null)
    try {
      const data = await verifyClaim(claimText)
      setResult(data)
    } catch (err) {
      setError('Could not reach the verification service. Is the backend running on port 8080?')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="page">
      <header className="masthead">
        <div className="masthead__seal">RS</div>
        <div>
          <h1>RumorShield</h1>
          <p className="masthead__tagline">Before you share it, verify it.</p>
        </div>
      </header>

      <main className="content">
        <ClaimForm onSubmit={handleSubmit} isLoading={isLoading} />

        {error && <p className="error-banner">{error}</p>}

        {result && <VerdictReport result={result} />}

        {!result && !error && (
          <p className="empty-state">
            Paste a claim above — a rumor, a WhatsApp forward, an official-sounding
            notice — and RumorShield checks it against a curated set of verified
            Ghanaian civic sources before you decide whether to trust or share it.
          </p>
        )}
      </main>
    </div>
  )
}
