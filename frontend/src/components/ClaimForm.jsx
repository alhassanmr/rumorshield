import { useState } from 'react'

const EXAMPLES = [
  'Ghana Card renewal now costs GHS 200',
  'President Mahama is giving out a GHS 1,500 development cash grant',
  'NHIS registration is free right now',
]

export default function ClaimForm({ onSubmit, isLoading }) {
  const [text, setText] = useState('')

  function handleSubmit(e) {
    e.preventDefault()
    if (text.trim().length === 0) return
    onSubmit(text.trim())
  }

  return (
    <form className="intake" onSubmit={handleSubmit}>
      <label className="intake__label" htmlFor="claim">
        Paste the message or claim you want checked
      </label>
      <textarea
        id="claim"
        className="intake__field"
        rows={4}
        placeholder="e.g. They are saying passport fees have gone up to GHS 500..."
        value={text}
        onChange={(e) => setText(e.target.value)}
      />

      <div className="intake__footer">
        <div className="intake__examples">
          <span>Try:</span>
          {EXAMPLES.map((example) => (
            <button
              type="button"
              key={example}
              className="intake__example-chip"
              onClick={() => setText(example)}
            >
              {example.length > 38 ? example.slice(0, 38) + '…' : example}
            </button>
          ))}
        </div>

        <button type="submit" className="intake__submit" disabled={isLoading}>
          {isLoading ? 'Checking…' : 'Verify claim'}
        </button>
      </div>
    </form>
  )
}
